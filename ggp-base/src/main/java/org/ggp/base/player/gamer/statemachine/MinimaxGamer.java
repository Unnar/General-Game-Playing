package org.ggp.base.player.gamer.statemachine;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;

import org.ggp.base.player.gamer.event.GamerSelectedMoveEvent;
import org.ggp.base.player.gamer.statemachine.sample.SampleGamer;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

import is.ru.cadia.ggp.propnet.bitsetstate.RecursiveForwardChangePropNetStateMachine;
import is.ru.cadia.ggp.propnet.structure.GGPBasePropNetStructureFactory;
import is.ru.cadia.ggp.propnet.structure.PropNetStructure;
import is.ru.cadia.ggp.propnet.structure.PropNetStructureFactory;

public class MinimaxGamer extends SampleGamer {

	private boolean allLeafsAreTerminal;
	private HashMap<MachineState, MoveValue> mem;
	private int expandedNodes = 0;
	private int numberOfSelectMove;

	private final int risk = 49;
	private long gameStart;

	public class TimeOutException extends Exception {
		/**
		 *
		 */
		private static final long serialVersionUID = 1L; //WTF is this

		public TimeOutException(String message) {
			super(message);
		}
	}

	private class MoveValue implements Comparable<MoveValue> {
		private final int value;
		private final Move move;
		private final int depthExplored;
		private final boolean fullyExplored;
		public int getValue() {
			return value;
		}
		public Move getMove() {
			return move;
		}
		public int getDepthExplored() {
			return depthExplored;
		}
		public boolean getFullyExplored() {
			return fullyExplored;
		}
		@Override
		public int compareTo(MoveValue other) {
			return other.value - this.value;
		}
		public MoveValue(Move move, int value) {
			this(move, value, 0, false);
		}

		public MoveValue(Move move, int value, int depthExplored, boolean fullyExplored) {
			this.move = move;
			this.value = value;
			this.depthExplored = depthExplored;
			this.fullyExplored = fullyExplored;
		}
	}

	@Override
	public void stateMachineStop() {
		try {
			PrintWriter out = new PrintWriter(new FileWriter("minimaxresults.txt", true));
			out.println("match " + getMatch().getMatchId());
			out.println("role " + getRole().getName().getValue() +  " " + getStateMachine().getRoleIndices().get(getRole()));
			out.println("steps " + (numberOfSelectMove-1));
			List<Integer> goals = getStateMachine().getGoals(getStateMachine().getMachineStateFromSentenceList(getMatch().getMostRecentState()));
			out.print("scores");
			for(int i : goals) {
				out.print(" " + i);
			}
			out.println();
			out.println("nodes " + expandedNodes);
			out.flush();
			out.close();
		} catch (GoalDefinitionException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Number of expanded states (maxnodes): " + expandedNodes);
		System.out.println("Playing game took " + (System.currentTimeMillis() - gameStart) + " ms");
	}

	@Override
	public Move stateMachineSelectMove(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		long start = System.currentTimeMillis();
		numberOfSelectMove++;
		StateMachine machine = getStateMachine();
		MachineState state = getCurrentState();
		int maxDepth = 1;
        MoveValue best = null;
        System.out.println(System.currentTimeMillis() + " " + timeout);
        try {
			while(true){
				allLeafsAreTerminal = true;
				best = minimax(machine, state, maxDepth, timeout-500);
		        maxDepth++;;
		        if(allLeafsAreTerminal) break;
			}
        }
        catch (TimeOutException e) {
        	allLeafsAreTerminal = false;
        }
        if(best == null) return null;
        if(best.getMove() == null) {
        	System.out.println(state.toString());
        }
        if(allLeafsAreTerminal) {
        	System.out.println("Fully explored tree");
        }
        System.out.println(best.getMove());
        System.out.println(best.getValue());
        long stop = System.currentTimeMillis();
        notifyObservers(new GamerSelectedMoveEvent(machine.getLegalMoves(state, getRole()), best.getMove(), stop - start));
		return best.getMove();
	}


	private MoveValue minimax(StateMachine machine, MachineState state, int maxDepth, long timeout)
			throws MoveDefinitionException, GoalDefinitionException, TransitionDefinitionException,
					TimeOutException{
		if(System.currentTimeMillis() > timeout) {
			throw new TimeOutException("Minimax timed out");
		}
		if(mem.containsKey(state)) {
			MoveValue res = mem.get(state);
			if(res.getFullyExplored()) {
				return res;
			}
			else if(res.getDepthExplored() >= maxDepth) {
				allLeafsAreTerminal = false;
				return res;
			}
		}
		if(machine.isTerminal(state)) {
			return new MoveValue(null, machine.getGoal(state, getRole()), maxDepth, true);
		}
		else if(maxDepth <= 0) {
			allLeafsAreTerminal = false;
			return evaluate(state);
		}
		List<Move> moves = machine.getLegalMoves(state, getRole());
		MoveValue best = new MoveValue(null, -1);
		expandedNodes++;
		for(Move move : moves) {
			boolean explored = true;
			List<List<Move>> allMoves = machine.getLegalJointMoves(state, getRole(), move);
			MoveValue worst = new MoveValue(null, 101);
			for(List<Move> jointMoves : allMoves) {
				MoveValue curr = minimax(machine, machine.getNextState(state, jointMoves), maxDepth - 1, timeout);
				if(curr.getValue() < worst.getValue()) {
					worst = curr;
				}
				explored &= curr.getFullyExplored();
			}
			if(worst.getValue() > best.getValue()) {
				best = new MoveValue(move, worst.getValue(), maxDepth, explored);
			}
		}
		mem.put(state, best);

		return best;
	}



	private MoveValue evaluate(MachineState state) {
		// TODO Auto-generated method stub
		return new MoveValue(null, risk, 0, false);
	}

	@Override
    public void stateMachineMetaGame(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
    {
		expandedNodes = 0;
		numberOfSelectMove = 0;
		gameStart = System.currentTimeMillis();
		mem = new HashMap<MachineState, MoveValue>();
		try {
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
		}
		stateMachineSelectMove(timeout);
        // Sample gamers do no metagaming at the beginning of the match.
    }
}
