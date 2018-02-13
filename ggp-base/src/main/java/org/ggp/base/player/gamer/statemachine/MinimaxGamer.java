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

	private final int risk = 50;

	@Override
	public Move stateMachineSelectMove(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		StateMachine machine = getStateMachine();
		MachineState state = getCurrentState();
		Move move = minimax(machine, state, timeout);

		return move;
	}

	private Move minimax(StateMachine machine, MachineState state, long timeout) throws MoveDefinitionException, GoalDefinitionException, TransitionDefinitionException	{
		int maxDepth = 1;
        long start = System.currentTimeMillis();
        long now = System.currentTimeMillis();
        MoveValue best = null;
		while(now - start <= timeout * 0.98) {
			best = DLS(machine, state, maxDepth);
	        now = System.currentTimeMillis();
		}

		return best.move;
	}

	private MoveValue DLS(StateMachine machine, MachineState state, int maxDepth)
			throws MoveDefinitionException, GoalDefinitionException, TransitionDefinitionException {
		if(machine.isTerminal(state)) {
			return new MoveValue(null, machine.getGoal(state, getRole()));
		}
		else if(maxDepth == 0) {
			return evaluate(state);
		}
		List<Move> moves = machine.getLegalMoves(state, getRole());
		MoveValue best = new MoveValue(null, 0);
		for(Move move : moves) {
			List<List<Move>> allMoves = machine.getLegalJointMoves(state, getRole(), move);
			MoveValue worst = new MoveValue(null, 100);
			for(List<Move> jointMoves : allMoves) {
				MoveValue curr = DLS(machine, machine.getNextState(state, jointMoves), maxDepth - 1);
				if(curr.value < worst.value) {
					worst = curr;
				}
			}
			if(worst.value > best.value) {
				best = new MoveValue(move, worst.value);
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
