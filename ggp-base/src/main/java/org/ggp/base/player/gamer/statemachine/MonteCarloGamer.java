package org.ggp.base.player.gamer.statemachine;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.ggp.base.player.gamer.statemachine.sample.SampleGamer;
import org.ggp.base.util.gdl.grammar.GdlTerm;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

import is.ru.cadia.ggp.propnet.bitsetstate.RecursiveForwardChangePropNetStateMachine;
import is.ru.cadia.ggp.propnet.structure.GGPBasePropNetStructureFactory;

public class MonteCarloGamer extends SampleGamer {
	private Random random;
	private MonteCarloNode root;

	private final double C = 50;
	private final int maxTreeSize = 2000000;

	public class TimeOutException extends Exception {
		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		public TimeOutException(String message) {
			super(message);
		}
	}

	public MonteCarloGamer() {
		super();
		this.random = new Random();
		System.out.print("THE VALUE OF C IS: " + C);
	}

	@Override
	public StateMachine getInitialStateMachine() {
		return new RecursiveForwardChangePropNetStateMachine(new GGPBasePropNetStructureFactory());
	}

	@Override
	public Move stateMachineSelectMove(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		long start = System.currentTimeMillis();
		StateMachine machine = getStateMachine();
		MachineState state = getCurrentState();
		List<GdlTerm> lastMoves = getMatch().getMostRecentMoves();
		if (lastMoves != null) {
			List<Move> jointMove = new ArrayList<Move>();
			for (GdlTerm sentence : lastMoves)
			{
					jointMove.add(machine.getMoveFromTerm(sentence));
			}
			if(root.children.containsKey(jointMove)) {
				root = root.children.get(jointMove);
				root.parent = null;
				root.parentMove = null;
			}
			else {
				root = new MonteCarloNode(state);
				expand(root);
			}
		} // else we are still in the initial state of the game
		else if(root == null) {
			root = new MonteCarloNode(state);
			expand(root);
		}
		Role r = getRole();
		boolean tooBig = false;
		MonteCarloNode currNode = root;
		try {
			while(true) {
				if(System.currentTimeMillis() > timeout-500) {
					throw new TimeOutException("MonteCarlo timed out");
				}
				if(machine.isTerminal(currNode.state)) {
					propagate(currNode, machine.getGoals(currNode.state), false, timeout-500);
					tooBig = false;
					currNode = root;
					continue;
				}
				if(currNode.size >= maxTreeSize) {
					tooBig = true;
				}
				List<Move> jm = selectMoves(currNode);

				if(currNode.children.containsKey(jm))
				{
					currNode = currNode.children.get(jm);
				}
				else
				{
					if(tooBig) {
						List<Integer> scores = runSimulation(currNode.state, timeout-500);
						currNode.simulations++;
						propagate(currNode, scores, false, timeout-500);
						tooBig = false;
						currNode = root;
						continue;
					}
					MachineState nextState = machine.getNextState(currNode.state, jm);

					MonteCarloNode newNode = new MonteCarloNode(nextState, currNode, jm);
					currNode.children.put(jm, newNode);
					expand(newNode);
					List<Integer> scores = runSimulation(nextState, timeout-500);
					newNode.simulations++;
					propagate(newNode, scores, true, timeout-500);
					tooBig = false;
					currNode = root;
				}
			}
		}
		catch(TimeOutException e) {
			System.out.println("Caught TimeOutException with " + (timeout-System.currentTimeMillis()) +  " ms remaining");
		}
		Move best = null;
		double bestval = -1;
		for(Move m : machine.getLegalMoves(state, r)) {
			if(root.Q.containsKey(m) && bestval < root.Q.get(m)) {
				best = m;
				bestval = root.Q.get(m);
			}
		}
		long stop = System.currentTimeMillis();
		//notifyObservers(new GamerSelectedMoveEvent(machine.getLegalMoves(state, getRole()), best, stop - start));
		System.out.println("Monte Carlo Q value: " + root.Q.get(best));
		System.out.println("Monte Carlo N value: " + root.N.get(best));
		return best;
	}

	@Override
	public void stateMachineStop() {
		System.out.println("I AM STOPPING");
		root = null;
//		System.out.println("Number of expanded states (maxnodes): " + expandedNodes);
//		System.out.println("Playing game took " + (System.currentTimeMillis() - gameStart) + " ms");
	}

	@Override
    public void stateMachineMetaGame(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
    {
//		gameStart = System.currentTimeMillis();
//		expandedNodes = 0;
		stateMachineSelectMove(timeout);
    }

	private List<Integer> runSimulation(MachineState state, long timeout) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException, TimeOutException {
		if(System.currentTimeMillis() > timeout) {
			throw new TimeOutException("MonteCarlo timed out");
		}
		StateMachine machine = getStateMachine();
		if(machine.isTerminal(state)) {
			return machine.getGoals(state);
		}
		List<List<Move>> jointMoves = machine.getLegalJointMoves(state);
		int choice = random.nextInt(jointMoves.size());
		List<Move> jm = jointMoves.get(choice);
		MachineState nextState = machine.getNextState(state, jm);
		return runSimulation(nextState, timeout);
	}

	public List<Move> selectMoves(MonteCarloNode node) throws MoveDefinitionException {
		StateMachine machine = getStateMachine();

		ArrayList<Move> res = new ArrayList<Move>();
		for(Role r : machine.getRoles()) {
			Move bestMove = null;
			double bestVal = -1;
			for(Move m : machine.getLegalMoves(node.state, r)) {
				double curr = UCT(node.Q.get(m), node.N.get(m), node.simulations);
				if(bestVal < curr) {
					bestMove = m;
					bestVal = curr;
				}
			}
			res.add(bestMove);
		}

		return res;
	}

	public double UCT(double q, double n, double sims) {
		return (sims == 0 || n == 0) ? Double.POSITIVE_INFINITY : q + C * Math.sqrt(Math.log(sims)/n);
	}

	private void expand(MonteCarloNode node) throws MoveDefinitionException {
		StateMachine machine = getStateMachine();
		if(machine.isTerminal(node.state)) return;
		for(Role r : machine.getRoles()) {
			for(Move m : machine.getLegalMoves(node.state, r)) {
				node.Q.put(m, 0.0);
				node.N.put(m, 0);
			}
		}
	}

	private void propagate(MonteCarloNode node, List<Integer> scores, boolean incSize, long timeout) throws TimeOutException {
		while(node.parent != null) {
			if(System.currentTimeMillis() > timeout) {
				throw new TimeOutException("MonteCarlo timed out");
			}
			for(int i = 0; i < node.parentMove.size(); i++) {
				Move m = node.parentMove.get(i);
				if(node.parent.Q.containsKey(m)) {
					node.parent.Q.put(m, (node.parent.Q.get(m)*node.parent.N.get(m) + scores.get(i))/(node.parent.N.get(m)+1));
					node.parent.N.put(m, node.parent.N.get(m)+1);
				}
				else {
					node.parent.Q.put(m, (double)scores.get(i));
					node.parent.N.put(m, 1);
				}
			}
			node.parent.simulations++;
			if(incSize) {
				node.parent.size++;
			}
			node = node.parent;
		}
	}
}
