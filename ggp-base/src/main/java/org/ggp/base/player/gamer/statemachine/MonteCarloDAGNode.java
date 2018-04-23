package org.ggp.base.player.gamer.statemachine;

import java.util.HashMap;
import java.util.List;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;

public class MonteCarloDAGNode {
	public MachineState state;
	public HashMap<Move, Double> Q;
	public HashMap<Move, Integer> N;
	public int simulations;
	public int size;
	public HashMap<List<Move>, MonteCarloDAGNode> children;

	public MonteCarloDAGNode(MachineState state) {
		this.state = state;
		this.Q = new HashMap<Move, Double>();
		this.N = new HashMap<Move, Integer>();
		this.children = new HashMap<List<Move>, MonteCarloDAGNode>();
		this.simulations = 0;
		this.size = 1;
	}
}
