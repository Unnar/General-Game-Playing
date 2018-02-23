package org.ggp.base.player.gamer.statemachine;

import java.util.HashMap;
import java.util.List;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;

public class MonteCarloNode {
	public MachineState state;
	public MonteCarloNode parent;
	public List<Move> parentMove;
	public HashMap<Move, Double> Q;
	public HashMap<Move, Integer> N;
	public int simulations;
	public int size;
	public HashMap<List<Move>, MonteCarloNode> children;

	public MonteCarloNode(MachineState state) {
		this(state, null, null);
	}
	public MonteCarloNode(MachineState state, MonteCarloNode parent, List<Move> parentMove) {
		this.state = state;
		this.parent = parent;
		this.parentMove = parentMove;
		this.Q = new HashMap<Move, Double>();
		this.N = new HashMap<Move, Integer>();
		this.children = new HashMap<List<Move>, MonteCarloNode>();
		this.simulations = 0;
		this.size = 1;
	}
}
