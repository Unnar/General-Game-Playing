package org.ggp.base.player.gamer.statemachine;

import java.util.ArrayList;
import java.util.HashMap;
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

public class MonteCarloDAGGamer extends SampleGamer {
	private Random random;
	private MonteCarloDAGNode root;

	private HashMap<MachineState, MonteCarloDAGNode> map;
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

	public MonteCarloDAGGamer() {
		super();
		this.random = new Random();
		map = new HashMap<MachineState, MonteCarloDAGNode>();
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
			}
			else {
				root = new MonteCarloDAGNode(state);
				map.put(state, root);
				nodeCount++;
				expand(root);
			}
		} // else we are still in the initial state of the game
		else if(root == null) { // If we have no root then we create a new one
			root = new MonteCarloDAGNode(state);
			map.put(state, root);
			nodeCount++;
			expand(root);
		}


		numberOfSelectMove++;
		int sims = root.simulations;

		Role r = getRole();
		MonteCarloDAGNode currNode = root;
		// Run simulations until time is running out leaving enough time to return a move
		try {
			List<MonteCarloDAGNode> parents = new ArrayList<MonteCarloDAGNode>();
			List<List<Move>> parentMoves = new ArrayList<List<Move>>();
			while(true) {
				if(System.currentTimeMillis() > realTimeout) {
					throw new TimeOutException("MonteCarlo timed out");
				}
				// If in the selection phase we find a terminal node we propagate the values and
				// start the selection phase again
				if(machine.isTerminal(currNode.state)) {
					propagate(currNode, machine.getGoals(currNode.state), false, realTimeout, parents, parentMoves);
					currNode = root;
					continue;
				}

				List<Move> jm = selectMoves(currNode);

				// We are still inside our tree so we move to the next node
				if(currNode.children.containsKey(jm))
				{
					parents.add(currNode);
					parentMoves.add(jm);
					currNode = currNode.children.get(jm);
				}
				// We have fallen out of our tree so we add the node to our tree and
				// run a simulation from there
				else
				{
					// If the tree we have so far is at our limit we will run a simulation from the leaf
					// and not add anything to our tree
					MachineState nextState = machine.getNextState(currNode.state, jm);
					if(map.size() > maxTreeSize && !map.containsKey(nextState)) {
						List<Integer> scores = runSimulation(currNode.state, realTimeout);
						currNode.simulations++;
						propagate(currNode, scores, false, timeout-500, parents, parentMoves);
						currNode = root;
						continue;
					}
					// Otherwise we generate the next state and make a new node with that state
					// then simulate from that node
					MonteCarloDAGNode newNode;
					if(map.containsKey(nextState)) {
						newNode = map.get(nextState);
					}
					else {
						newNode = new MonteCarloDAGNode(nextState);
						map.put(nextState, newNode);
						parents.add(currNode);
						parentMoves.add(jm);
						nodeCount++;
						expand(newNode);
					}
					currNode.children.put(jm, newNode);
					List<Integer> scores = runSimulation(nextState, realTimeout);
					newNode.simulations++;
					propagate(newNode, scores, true, timeout-500, parents, parentMoves);
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
		System.out.println("DAG Q value: " + root.Q.get(best));
		System.out.println("DAG N value: " + root.N.get(best));
		return best;
	}

	@Override
	public void stateMachineStop() {
		//System.out.println("I AM STOPPING");
		/*try { // Results
			PrintWriter out = new PrintWriter(new FileWriter("mctsdag.txt", true));
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
			out.println("nodes " + map.size());
			out.flush();
			out.close();
		} catch (GoalDefinitionException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		root = null;
		map.clear();
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
		MachineState nextState = machine.getNextState(state, jm);
		return runSimulation(nextState, timeout);
	}

	// Selects the best move for each role from the UCT values
	public List<Move> selectMoves(MonteCarloDAGNode node) throws MoveDefinitionException {
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
	private void expand(MonteCarloDAGNode node) throws MoveDefinitionException {
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
	private void propagate(MonteCarloDAGNode node, List<Integer> scores, boolean incSize, long timeout,
			List<MonteCarloDAGNode> parents, List<List<Move>> parentMoves) throws TimeOutException {
		while(!parents.isEmpty()) {
			MonteCarloDAGNode parent = parents.remove(parents.size()-1);
			List<Move> jm = parentMoves.remove(parentMoves.size()-1);
			for(int i = 0; i < jm.size(); i++) {
				Move m = jm.get(i);
				if(parent.Q.containsKey(m)) {
					parent.N.put(m, parent.N.get(m)+1);
					parent.Q.put(m, parent.Q.get(m)+(scores.get(i)-parent.Q.get(m))/parent.N.get(m));
				}
				else {
					parent.Q.put(m, (double)scores.get(i));
					parent.N.put(m, 1);
				}
			}
			parent.simulations++;
		}
	}
}
