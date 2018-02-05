package org.ggp.base.util.statemachine;

import java.util.BitSet;
import java.util.Set;

import org.ggp.base.util.gdl.grammar.GdlSentence;

public class PropNetMachineState extends MachineState {
	public PropNetMachineState() {
		super();
		this.contents = null;
		this.assignment = new BitSet();
	}
	public PropNetMachineState(Set<GdlSentence> contents, BitSet assignment) {
		super();
		this.contents = contents;
		this.assignment = assignment;
	}
	private final BitSet assignment;
	private final Set<GdlSentence> contents;
	@Override
	public Set<GdlSentence> getContents() {
		// TODO Auto-generated method stub
		return null;
	}

	public BitSet getAssignment() {
		return assignment;
	}

	@Override
	public MachineState clone() {
		// TODO Auto-generated method stub
		return null;
	}

}
