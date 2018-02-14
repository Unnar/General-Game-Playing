package org.ggp.base.player.gamer.statemachine;

import java.util.List;

import org.ggp.base.player.gamer.statemachine.sample.SampleGamer;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public class MinimaxGamer extends SampleGamer {

	private boolean allLeafsAreTerminal;

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
		public int getValue() {
			return value;
		}
		public Move getMove() {
			return move;
		}
		@Override
		public int compareTo(MoveValue other) {
			return other.value - this.value;
		}
		public MoveValue(Move move, int value) {
			this.move = move;
			this.value = value;
		}
	}

	private final int risk = 49;

	@Override
	public Move stateMachineSelectMove(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		//long start = System.currentTimeMillis();
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
        catch (TimeOutException e) {}
        if(best.getMove() == null) {
        	System.out.print(state.toString());
        }
        System.out.println(best.getMove());
        System.out.println(best.getValue());
        //long stop = System.currentTimeMillis();
        //notifyObservers(new GamerSelectedMoveEvent(moves, best.getMove(), stop - start))
		return best.getMove();
	}


	private MoveValue minimax(StateMachine machine, MachineState state, int maxDepth, long timeout)
			throws MoveDefinitionException, GoalDefinitionException, TransitionDefinitionException,
					TimeOutException{
		if(System.currentTimeMillis() > timeout) {
			throw new TimeOutException("Minimax timed out");
		}
		if(machine.isTerminal(state)) {
			return new MoveValue(null, machine.getGoal(state, getRole()));
		}
		else if(maxDepth <= 0) {
			allLeafsAreTerminal = false;
			return evaluate(state);
		}
		List<Move> moves = machine.getLegalMoves(state, getRole());
		MoveValue best = new MoveValue(null, -1);
		for(Move move : moves) {
			List<List<Move>> allMoves = machine.getLegalJointMoves(state, getRole(), move);
			MoveValue worst = new MoveValue(null, 101);
			for(List<Move> jointMoves : allMoves) {
				MoveValue curr = minimax(machine, machine.getNextState(state, jointMoves), maxDepth - 1, timeout);
				if(curr.getValue() < worst.getValue()) {
					worst = curr;
				}
			}
			if(worst.getValue() > best.getValue()) {
				best = new MoveValue(move, worst.getValue());
			}
		}


		return best;
	}



	private MoveValue evaluate(MachineState state) {
		// TODO Auto-generated method stub
		return new MoveValue(null, risk);
	}

	@Override
    public void stateMachineMetaGame(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
    {
        // Sample gamers do no metagaming at the beginning of the match.
    }
}
