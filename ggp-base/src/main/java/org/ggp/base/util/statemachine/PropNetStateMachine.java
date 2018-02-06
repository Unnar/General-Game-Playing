package org.ggp.base.util.statemachine;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

import is.ru.cadia.ggp.propnet.PropNetMove;
import is.ru.cadia.ggp.propnet.structure.GGPBasePropNetStructureFactory;
import is.ru.cadia.ggp.propnet.structure.PropNetStructure;
import is.ru.cadia.ggp.propnet.structure.PropNetStructureFactory;
import is.ru.cadia.ggp.propnet.structure.components.BaseProposition;
import is.ru.cadia.ggp.propnet.structure.components.StaticComponent;
import is.ru.cadia.ggp.propnet.structure.components.StaticComponent.Type;

public class PropNetStateMachine extends StateMachine {
	private PropNetStructure structure;
    private List<StaticComponent> components;
    private List<BaseProposition> basePropositions;
    private List<StaticComponent> inputComponents;
    private StaticComponent terminalComponent;
    /** The player roles */
    private List<Role> roles;


	// ============================================
    //          Stubs for implementations
    // ============================================
    //  The following methods are required for a valid
    // state machine implementation.
    /**
     * Initializes the StateMachine to describe the given game rules.
     * <p>
     * This method should only be called once, and it should be called before any
     * other methods on the StateMachine.
     */
    @Override
	public void initialize(List<Gdl> description) {
    	PropNetStructureFactory factory = new GGPBasePropNetStructureFactory();
    	try {
			structure = factory.create(description);
			File file = new File("propnet.dot");
			structure.renderToFile(file);
			roles = new ArrayList<Role>(Arrays.asList(structure.getRoles()));
			components = new ArrayList<StaticComponent>(Arrays.asList(structure.getComponents()));
			inputComponents = new ArrayList<StaticComponent>();
			terminalComponent = structure.getTerminalProposition();
			for(StaticComponent component : components) {
				if(component.type == Type.INPUT) {
					inputComponents.add(component);
				}
			}
			basePropositions = new ArrayList<BaseProposition>(Arrays.asList(structure.getBasePropositions()));
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    /**
     * Returns the goal value for the given role in the given state. Goal values
     * are always between 0 and 100.
     *
     * @throws GoalDefinitionException if there is no goal value or more than one
     * goal value for the given role in the given state. If this occurs when this
     * is called on a terminal state, this indicates an error in either the game
     * description or the StateMachine implementation.
     */
    @Override
	public int getGoal(MachineState state, Role role) throws GoalDefinitionException{
    	int ind = -1;
    	boolean cmp[] = new boolean[components.size()];
    	boolean mem[] = new boolean[components.size()];
    	BitSet assignment = ((PropNetMachineState)state).getAssignment();
    	for(BaseProposition bp : basePropositions) {
    		cmp[bp.id] = true;
    		mem[bp.id] = assignment.get(bp.id);
    	}
    	int rid = structure.getRoleId(role);
    	StaticComponent[] goals = structure.getGoalPropositions(rid);
    	for(int i = 0; i < goals.length; i++) {
    		if(findValue(goals[i].id, mem, cmp)) {
    			if(ind == -1) ind = i;
    			else throw new GoalDefinitionException(state, role);
    		}
    	}
    	if(ind == -1) return 0;
    	return structure.getGoalValues(rid)[ind];
    }
    /**
     * Returns true if and only if the given state is a terminal state (i.e. the
     * game is over).
     */
    @Override
	public boolean isTerminal(MachineState state) {
    	if(state.getClass() != PropNetMachineState.class) return false;
    	boolean cmp[] = new boolean[components.size()];
    	boolean mem[] = new boolean[components.size()];
    	BitSet assignment = ((PropNetMachineState)state).getAssignment();
    	for(BaseProposition bp : basePropositions) {
    		cmp[bp.id] = true;
    		mem[bp.id] = assignment.get(bp.id);
    	}
    	return findValue(terminalComponent.id, mem, cmp);
    }

    /**
     * Returns a list of the roles in the game, in the same order as they
     * were defined in the game description.
     * <p>
     * The result will be the same as calling {@link Role#computeRoles(List)}
     * on the game rules used to initialize this state machine.
     */
    @Override
	public List<Role> getRoles(){
    	return roles;
    }
    /**
     * Returns the initial state of the game.
     */
    @Override
	public MachineState getInitialState() {
    	BitSet initAssignment = new BitSet(components.size());
		for(BaseProposition bp : basePropositions) {
			initAssignment.set(bp.id, bp.initialValue);
		}
		return new PropNetMachineState(null, initAssignment);
    }

    /**
     * Returns a list containing every move that is legal for the given role in the
     * given state.
     *
     * @throws MoveDefinitionException if the role has no legal moves. This indicates
     * an error in either the game description or the StateMachine implementation.
     */
    // TODO: There are philosophical reasons for this to return Set<Move> rather than List<Move>.
    @Override
	public List<Move> getLegalMoves(MachineState state, Role role) throws MoveDefinitionException{
    	boolean cmp[] = new boolean[components.size()];
    	boolean mem[] = new boolean[components.size()];
    	BitSet assignment = ((PropNetMachineState)state).getAssignment();
    	for(BaseProposition bp : basePropositions) {
    		cmp[bp.id] = true;
    		mem[bp.id] = assignment.get(bp.id);
    	}

    	ArrayList<Move> res = new ArrayList<Move>();
    	for(PropNetMove m : structure.getPossibleMoves(structure.getRoleId(role)) ) {
    		if(findValue(m.getLegalComponent().id, mem, cmp)) {
    			res.add(m);
    		}
    	}

    	return res;
    }


    /**
     * Returns the next state of the game given the current state and a joint move
     * list containing one move per role.
     *
     * @param moves A list containing one move per role. The moves should be
     * listed in the same order as roles are listed by {@link #getRoles()}.
     * @throws TransitionDefinitionException indicates an error in either the
     * game description or the StateMachine implementation.
     */
    @Override
	public MachineState getNextState(MachineState state, List<Move> moves) throws TransitionDefinitionException{
    	boolean cmp[] = new boolean[components.size()];
    	boolean mem[] = new boolean[components.size()];
    	BitSet assignment = ((PropNetMachineState)state).getAssignment();
    	for(BaseProposition bp : basePropositions) {
    		cmp[bp.id] = true;
    		mem[bp.id] = assignment.get(bp.id);
    	}
    	for(int i = 0; i < moves.size(); i++) {
    		int id = structure.getPropNetMove(i, moves.get(i)).getInputComponent().id;
    		cmp[id] = true;
    		mem[id] = true;
    	}

    	BitSet newAssignment = new BitSet(components.size());
    	for(BaseProposition bp : basePropositions) {
    		if(bp.inputs.length == 1) {
    			newAssignment.set(bp.id, findValue(bp.inputs[0], mem, cmp));
    		}
    	}

    	return new PropNetMachineState(null, newAssignment);
    }

    private boolean findValue(int id, boolean mem[], boolean cmp[])
    {
    	if(cmp[id]) return mem[id];
    	if(components.get(id).isCyclic) {
    		cmp[id] = true;
    		mem[id] = false;
    		return false;
    	}
    	boolean res = false;
    	switch(components.get(id).type) {
		case AND:
			res = true;
			for(int i : components.get(id).inputs) {
				if(!findValue(i, mem, cmp)) res = false;
			}
			break;
		case BASE:
			// should already be in memoization
			assert(false);
			break;
		case FALSE:
			res = false;
			break;
		case INIT:
			// intentionally blank
			assert(false);
			break;
		case INPUT:
			// should already be in memoization
			assert(false);
			break;
		case NOT:
			res = !findValue(components.get(id).inputs[0], mem, cmp);
			break;
		case OR:
			res = false;
			for(int i : components.get(id).inputs) {
				if(findValue(i, mem, cmp)) res = true;
			}
			break;
		case PIPE:
			res = findValue(components.get(id).inputs[0], mem, cmp);
			break;
		case TRUE:
			res = true;
			break;
		default:
			res = false;
			break;

    	}
    	cmp[id] = true;
    	mem[id] = res;
    	return res;
    }
}
