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
	private final int maxTreeSize = 500000;
	private int numberOfSelectMove;
	private int simulations;
	private int nodeCount;

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
		//System.out.println("THE VALUE OF C IS: " + C);
	}

	@Override
	public StateMachine getInitialStateMachine() {
		return new RecursiveForwardChangePropNetStateMachine(new GGPBasePropNetStructureFactory());
	}

	@Override
	public Move stateMachineSelectMove(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		long start = System.currentTimeMillis();
		long realTimeout = start + (timeout-start)*9/10;
		StateMachine machine = getStateMachine();
		MachineState state = getCurrentState();
		List<GdlTerm> lastMoves = getMatch().getMostRecentMoves();
		// We do not always want to start with a new tree after a move has been made
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
				nodeCount++;
				expand(root);
			}
		} // else we are still in the initial state of the game
		else if(root == null) { // If we have no root then we create a new one
			root = new MonteCarloNode(state);
			nodeCount++;
			expand(root);
		}


		numberOfSelectMove++;
		int sims = root.simulations;

		Role r = getRole();
		boolean tooBig = false;
		MonteCarloNode currNode = root;
		// Run simulations until time is running out leaving enough time to return a move
		try {
			while(true) {
				if(System.currentTimeMillis() > realTimeout) {
					throw new TimeOutException("MonteCarlo timed out");
				}
				// If in the selection phase we find a terminal node we propagate the values and
				// start the selection phase again
				if(machine.isTerminal(currNode.state)) {
					propagate(currNode, machine.getGoals(currNode.state), false, realTimeout);
					tooBig = false;
					currNode = root;
					continue;
				}
				if(currNode.size >= maxTreeSize) {
					tooBig = true;
				}
				List<Move> jm = selectMoves(currNode);

				// We are still inside our tree so we move to the next node
				if(currNode.children.containsKey(jm))
				{
					currNode = currNode.children.get(jm);
				}
				// We have fallen out of our tree so we add the node to our tree and
				// run a simulation from there
				else
				{
					// If the tree we have so far is at our limit we will run a simulation from the leaf
					// and not add anything to our tree
					if(tooBig) {
						List<Integer> scores = runSimulation(currNode.state, realTimeout);
						currNode.simulations++;
						propagate(currNode, scores, false, timeout-500);
						tooBig = false;
						currNode = root;
						continue;
					}
					// Otherwise we generate the next state and make a new node with that state
					// then simulate from that node
					MachineState nextState = machine.getNextState(currNode.state, jm);
					MonteCarloNode newNode = new MonteCarloNode(nextState, currNode, jm);
					nodeCount++;
					currNode.children.put(jm, newNode);
					expand(newNode);
					List<Integer> scores = runSimulation(nextState, realTimeout);
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
		// We choose the move in the root which has the highest Q value
		Move best = null;
		double bestval = -1;
		for(Move m : machine.getLegalMoves(state, r)) {
			if(root.Q.containsKey(m) && bestval < root.Q.get(m)) {
				best = m;
				bestval = root.Q.get(m);
			}
		}
		long stop = System.currentTimeMillis();
		simulations += root.simulations - sims;
		//notifyObservers(new GamerSelectedMoveEvent(machine.getLegalMoves(state, getRole()), best, stop - start));
		System.out.println("Monte Carlo Q value: " + root.Q.get(best));
		System.out.println("Monte Carlo N value: " + root.N.get(best));
		return best;
	}

	@Override
	public void stateMachineStop() {
		//System.out.println("I AM STOPPING");

		/*try { // Results
			PrintWriter out = new PrintWriter(new FileWriter("mcts.txt", true));
			out.println("match " + getMatch().getMatchId());
			out.println("role " + getRole().getName().getValue() +  " " + getStateMachine().getRoleIndices().get(getRole()));
			out.println("steps " + numberOfSelectMove);
			List<Integer> goals = getStateMachine().getGoals(getStateMachine().getMachineStateFromSentenceList(getMatch().getMostRecentState()));
			out.print("scores");
			for(int i : goals) {
				out.print(" " + i);
			}
			out.println();
			double avgSims = ((double)simulations)/numberOfSelectMove;
			out.println("avgsims " + avgSims);
			out.println("nodes " + nodeCount);
			out.flush();
			out.close();
		} catch (GoalDefinitionException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		root = null;
//		System.out.println("Number of expanded states (maxnodes): " + expandedNodes);
//		System.out.println("Playing game took " + (System.currentTimeMillis() - gameStart) + " ms");
	}

	@Override
    public void stateMachineMetaGame(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
    {
		simulations = 0;
		numberOfSelectMove = 0;
		nodeCount = 0;
		/*try {
			// Note: this is an ugly hack that relies on the the match ID
			// being "something.gamename.timestamp", which happens to be the case
			// for the Server app and the Kiosk. This will only work while testing if
			// the game rules are not scrambled.
			// TODO: take this out for the final version of the player
			//       (simply using gameName = null, should be safe)
			String gameName = getMatch().getMatchId().replaceAll("(\\.|-)[0-9]*$", "");
			System.out.println("match: " + getMatch().getMatchId() + ", game:" + gameName);
			PropNetStructureFactory propnetFactory = new GGPBasePropNetStructureFactory();
			PropNetStructure propNet;
			propNet = propnetFactory.create(gameName, getMatch().getGame().getRules());
			StateMachine m = new RecursiveForwardChangePropNetStateMachine(propNet);
			switchStateMachine(m);
		} catch (InterruptedException | IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}*/
//		gameStart = System.currentTimeMillis();
//		expandedNodes = 0;
//		stateMachineSelectMove(timeout);
    }

	private List<Integer> runSimulation(MachineState state, long timeout) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException, TimeOutException {
		if(System.currentTimeMillis() > timeout) {
			throw new TimeOutException("MonteCarlo timed out");
		}
		StateMachine machine = getStateMachine();
		// If we are at a terminal state return the goal values
		if(machine.isTerminal(state)) {
			return machine.getGoals(state);
		}
		// Select a random move independently for each role to create a random joint move
		List<Move> jm = new ArrayList<Move>();
		for(Role r : machine.getRoles()) {
			List<Move> moves = machine.getLegalMoves(state, r);
			int choice = random.nextInt(moves.size());
			jm.add(moves.get(choice));
		}
//		List<List<Move>> jointMoves = machine.getLegalJointMoves(state);
//		int choice = random.nextInt(jointMoves.size());
//		List<Move> jm = jointMoves.get(choice);
		MachineState nextState = machine.getNextState(state, jm);
		return runSimulation(nextState, timeout);
	}

	// Selects the best move for each role from the UCT values
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

	// Initializes the Q and N values for a node
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

	// Back propagates the values for the nodes in the tree that were selected in the selection phase
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
