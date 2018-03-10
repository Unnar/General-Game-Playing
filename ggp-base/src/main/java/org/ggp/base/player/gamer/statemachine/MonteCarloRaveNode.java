package org.ggp.base.player.gamer.statemachine;

import java.util.HashMap;
import java.util.List;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;

public class MonteCarloRaveNode {
	public MachineState state;
	public MonteCarloRaveNode parent;
	public List<Move> parentMove;
	public HashMap<Move, Double> Q;
	public HashMap<Move, Double> QRAVE;
	public HashMap<Move, Integer> N;
	public HashMap<Move, Integer> NRAVE;
	public int simulations;
	public int size;
	public HashMap<List<Move>, MonteCarloRaveNode> children;

	public MonteCarloRaveNode(MachineState state) {
		this(state, null, null);
	}
	public MonteCarloRaveNode(MachineState state, MonteCarloRaveNode parent, List<Move> parentMove) {
		this.state = state;
		this.parent = parent;
		this.parentMove = parentMove;
		this.Q = new HashMap<Move, Double>();
		this.QRAVE = new HashMap<Move, Double>();
		this.N = new HashMap<Move, Integer>();
		this.NRAVE = new HashMap<Move, Integer>();
		this.children = new HashMap<List<Move>, MonteCarloRaveNode>();
		this.simulations = 0;
		this.size = 1;
	}
}
