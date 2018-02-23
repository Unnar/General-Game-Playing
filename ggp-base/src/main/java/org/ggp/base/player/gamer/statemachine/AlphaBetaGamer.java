package org.ggp.base.player.gamer.statemachine;

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

public class AlphaBetaGamer extends SampleGamer {


	private HashMap<MachineState, MoveValue> maxMem;
	private HashMap<MachineStateMove, MoveValue> minMem;
	private int expandedNodes = 0;

	public class MachineStateMove {
		private final MachineState state;
		private final Move move;

		public MachineStateMove(MachineState state, Move move) {
			this.state = state;
			this.move = move;
		}

		@Override
		public int hashCode() {
			return state.hashCode() * 31 * move.hashCode();
		}
	}

	public class TimeOutException extends Exception {
		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		public TimeOutException(String message) {
			super(message);
		}
	}

	public enum Bound {
		EXACT, UPPER, LOWER
	}

	private class MoveValue implements Comparable<MoveValue> {
		private final int value;
		private final Move move;
		private int depthExplored;
		private Bound bound;

		public int getValue() {
			return value;
		}
		public Move getMove() {
			return move;
		}
		public int getDepthExplored() {
			return depthExplored;
		}
		public Bound getBound() {
			return bound;
		}
		public void setBound(Bound other) {
			bound = other;
		}
		public void setDepthExplored(Integer d) {
			depthExplored = d;
		}
		@Override
		public int compareTo(MoveValue other) {
			return other.value - this.value;
		}
		public MoveValue(Move move, int value) {
			this(move, value, 0, Bound.EXACT);
		}

		public MoveValue(Move move, int value, int depthExplored, Bound bound) {
			this.move = move;
			this.value = value;
			this.depthExplored = depthExplored;
			this.bound = bound;
		}
	}

	private final int risk = 49;
	private long gameStart;


	@Override
	public StateMachine getInitialStateMachine() {
		return new RecursiveForwardChangePropNetStateMachine(new GGPBasePropNetStructureFactory());
	}

	@Override
	public void stateMachineStop() {
		System.out.println("Number of expanded states (maxnodes): " + expandedNodes);
		System.out.println("Playing game took " + (System.currentTimeMillis() - gameStart) + " ms");
	}

	@Override
	public Move stateMachineSelectMove(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		long start = System.currentTimeMillis();
		StateMachine machine = getStateMachine();
		MachineState state = getCurrentState();
		int maxDepth = 1;
        MoveValue best = null;
        try {
			while(true){
				best = minimax(machine, state, maxDepth, timeout-500, 0, 100);
				if(best.getDepthExplored() == Integer.MAX_VALUE) {
					break;
				}
		        maxDepth++;;
			}
        }
        catch (TimeOutException e) {
        }
        if(best.getMove() == null) {
        	System.out.println(state.toString());
        }
        System.out.println(best.getMove());
        System.out.println(best.getValue());
        long stop = System.currentTimeMillis();
        notifyObservers(new GamerSelectedMoveEvent(machine.getLegalMoves(state, getRole()), best.getMove(), stop - start));
		return best.getMove();
	}


	private MoveValue minimax(StateMachine machine, MachineState state, int maxDepth, long timeout, int alpha, int beta)
			throws MoveDefinitionException, GoalDefinitionException, TransitionDefinitionException,
					TimeOutException{
		if(System.currentTimeMillis() > timeout) {
			throw new TimeOutException("Minimax timed out");
		}
		if(maxMem.containsKey(state)) {
			MoveValue res = maxMem.get(state);
			if (res.getDepthExplored() >= maxDepth) {
				if (res.getBound() == Bound.EXACT) return res;
				else if(res.getBound() == Bound.LOWER) alpha = Math.max(alpha, res.getValue());
				else if(res.getBound() == Bound.UPPER) beta = Math.min(beta, res.getValue());

				if(alpha >= beta) return res;
			}
		}
		if(machine.isTerminal(state)) {
			return new MoveValue(null, machine.getGoal(state, getRole()), Integer.MAX_VALUE, Bound.EXACT);
		}
		else if(maxDepth <= 0) {
			return evaluate(state);
		}
		List<Move> moves = machine.getLegalMoves(state, getRole());
		MoveValue best = new MoveValue(null, -1);
		expandedNodes++;
		boolean fullyExplored = true;
		for(Move move : moves) {
			if(alpha >= beta) {
				best.setBound(Bound.LOWER);
				break;
			}
			MoveValue curr = minValue(machine, state, maxDepth, timeout, move, alpha, beta);
			if(curr.getDepthExplored() != Integer.MAX_VALUE) {
				fullyExplored = false;
			}
			if(curr.getValue() > best.getValue()) {
				best = new MoveValue(move, curr.getValue(), maxDepth, Bound.EXACT);
				alpha = Math.max(alpha, curr.getValue());
			}
		}
		if(fullyExplored) {
			best.setDepthExplored(Integer.MAX_VALUE);
		}
		maxMem.put(state, best);

		return best;
	}

	private MoveValue minValue(StateMachine machine, MachineState state, int maxDepth,
			long timeout, Move move, int alpha, int beta)
			throws MoveDefinitionException, GoalDefinitionException,
			TransitionDefinitionException, TimeOutException {
		//boolean explored = true;
		MachineStateMove key = new MachineStateMove(state, move);
		if(minMem.containsKey(key)) {
			MoveValue res = minMem.get(key);
			if (res.getDepthExplored() >= maxDepth) {
				if (res.getBound() == Bound.EXACT) return res;
				else if(res.getBound() == Bound.LOWER) alpha = Math.max(alpha, res.getValue());
				else if(res.getBound() == Bound.UPPER) beta = Math.min(beta, res.getValue());

				if(alpha >= beta) return res;
			}
		}
		boolean fullyExplored = true;
		List<List<Move>> allMoves = machine.getLegalJointMoves(state, getRole(), move);
		MoveValue worst = new MoveValue(null, 101);
		for(List<Move> jointMoves : allMoves) {
			if(alpha >= beta) {
				worst.setBound(Bound.UPPER);
				break;
			}
			MoveValue curr = minimax(machine, machine.getNextState(state, jointMoves), maxDepth - 1, timeout, alpha, beta);

			if(curr.getValue() < worst.getValue()) {
				worst = new MoveValue(null, curr.getValue(), maxDepth, Bound.EXACT);
				beta = Math.min(beta, curr.getValue());
			}
			if(curr.getDepthExplored() != Integer.MAX_VALUE) {
				fullyExplored = false;
			}
		}
		if(fullyExplored) {
			worst.setDepthExplored(Integer.MAX_VALUE);
		}
		minMem.put(key, worst);
		return worst;
	}

	private MoveValue evaluate(MachineState state) {
		// TODO Auto-generated method stub
		return new MoveValue(null, risk, 0, Bound.EXACT);
	}

	@Override
    public void stateMachineMetaGame(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
    {
		gameStart = System.currentTimeMillis();
		expandedNodes = 0;
		maxMem = new HashMap<MachineState, MoveValue>();
		minMem = new HashMap<MachineStateMove, MoveValue>();
		stateMachineSelectMove(timeout);
    }
}
