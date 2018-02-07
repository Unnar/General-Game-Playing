package org.ggp.base.util.statemachine;

import java.util.BitSet;
import java.util.List;
import java.util.Set;

import org.ggp.base.util.gdl.grammar.GdlSentence;

import is.ru.cadia.ggp.propnet.PropNetMove;
import is.ru.cadia.ggp.propnet.structure.components.StaticComponent;

public class PropNetMachineState extends MachineState {
	public PropNetMachineState() {
		super();
		this.contents = null;
		this.assignment = new BitSet();
		this.computed = new BitSet();
		this.nbBaseComponents = 0;
	}
	public PropNetMachineState(Set<GdlSentence> contents, int nbComponents, int nbBaseComponents) {
		super();
		this.contents = contents;
		this.assignment = new BitSet(nbComponents);
		this.computed = new BitSet(nbComponents);
		this.nbBaseComponents = nbBaseComponents;
	}
	private final BitSet assignment;
	private final BitSet computed;
	private final Set<GdlSentence> contents;
	private final int nbBaseComponents;
	@Override
	public Set<GdlSentence> getContents() {
		// TODO Auto-generated method stub
		return null;
	}

	public void setBasePropositions(BitSet baseBitSet) {
		for(int i = 0; i < baseBitSet.size(); i++) {
			computed.set(i, true);
			assignment.set(i, baseBitSet.get(i));
		}
	}

	public void setInput(List<PropNetMove> moves) {
		for(int i = nbBaseComponents; i < assignment.size(); i++) {
			computed.set(i, false);
		}
		for(PropNetMove move : moves) {
			int id = move.getInputComponent().id;
			computed.set(id, true);
			assignment.set(id, true);
		}
	}

	public boolean findValue(StaticComponent sc, List<StaticComponent> comps) {
		if(computed.get(sc.id)) return assignment.get(sc.id);
    	if(sc.isCyclic) {
    		computed.set(sc.id, true);
    		assignment.set(sc.id, false);
    		return false;
    	}
    	boolean res = false;
    	switch(sc.type) {
		case AND:
			res = true;
			for(int i : sc.inputs) {
				if(!findValue(comps.get(i), comps)) res = false;
			}
			break;
		case BASE:
			// should already be computed
			assert(false);
			break;
		case FALSE:
			res = false;
			break;
		case INIT:
			// intentionally blank, should never happen
			assert(false);
			break;
		case INPUT:
			// if not already computed then it was a move that wasn't made and should be false
			res = false;
			break;
		case NOT:
			res = !findValue(comps.get(sc.inputs[0]), comps);
			break;
		case OR:
			res = false;
			for(int i : sc.inputs) {
				if(findValue(comps.get(i), comps)) res = true;
			}
			break;
		case PIPE:
			res = findValue(comps.get(sc.inputs[0]), comps);
			break;
		case TRUE:
			res = true;
			break;
		default:
			res = false;
			break;

    	}
    	computed.set(sc.id, true);
    	assignment.set(sc.id, res);
    	return res;
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
