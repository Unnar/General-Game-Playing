package org.ggp.base.player.gamer.statemachine;

import java.util.HashMap;
import java.util.List;
import java.util.Random;

import org.ggp.base.player.gamer.event.GamerSelectedMoveEvent;
import org.ggp.base.player.gamer.statemachine.sample.SampleGamer;
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
		Role r = getRole();
		List<List<Move>> jointMoves = machine.getLegalJointMoves(state);

		HashMap<Move, Double> Q = new HashMap<Move, Double>();
		HashMap<Move, Integer> N = new HashMap<Move, Integer>();
		for(Move m : machine.getLegalMoves(state, r)) {
			Q.put(m, 0.0);
			N.put(m, 0);
		}
		try {
			while(true) {
				int choice = random.nextInt(jointMoves.size());
				List<Move> jm = jointMoves.get(choice);
				Move a = jm.get(machine.getRoleIndices().get(r));
				MachineState nextState = machine.getNextState(state, jm);
				int score = runSimulation(nextState, timeout-500);
				Q.put(a, (Q.get(a)*N.get(a) + score)/(N.get(a)+1));
				N.put(a, N.get(a)+1);
			}
		}
		catch(TimeOutException e) {

		}
		Move best = null;
		double bestval = -1;
		for(Move m : machine.getLegalMoves(state, r)) {
			if(bestval < Q.get(m)) {
				best = m;
				bestval = Q.get(m);
			}
		}
		long stop = System.currentTimeMillis();
		notifyObservers(new GamerSelectedMoveEvent(machine.getLegalMoves(state, getRole()), best, stop - start));
		return best;
	}

	private int runSimulation(MachineState state, long timeout) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException, TimeOutException {
		if(System.currentTimeMillis() > timeout) {
			throw new TimeOutException("Minimax timed out");
		}
		StateMachine machine = getStateMachine();
		if(machine.isTerminal(state)) {
			return machine.getGoal(state, getRole());
		}
		List<List<Move>> jointMoves = machine.getLegalJointMoves(state);
		int choice = random.nextInt(jointMoves.size());
		List<Move> jm = jointMoves.get(choice);
		MachineState nextState = machine.getNextState(state, jm);
		return runSimulation(nextState, timeout);
	}

}
