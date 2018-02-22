package org.ggp.base.player.gamer.statemachine;

import java.util.HashMap;
import java.util.List;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;

public class MonteCarloNode {
	private MachineState state;
	private MonteCarloNode parent;
	private HashMap<Move, Double> Q;
	private HashMap<Move, Integer> N;
	private HashMap<List<Move>, MonteCarloNode> children;

	public MonteCarloNode(MachineState state) {
		this(state, null);
	}
	public MonteCarloNode(MachineState state, MonteCarloNode parent) {
		this.state = state;
		this.parent = parent;
		this.Q = new HashMap<Move, Double>();
		this.N = new HashMap<Move, Integer>();
		this.children = new HashMap<List<Move>, MonteCarloNode>();
	}
}
