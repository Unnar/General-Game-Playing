package org.ggp.base.util.statemachine;

import java.util.ArrayList;
import java.util.List;

import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

import is.ru.cadia.ggp.propnet.structure.GGPBasePropNetStructureFactory;
import is.ru.cadia.ggp.propnet.structure.PropNetStructure;
import is.ru.cadia.ggp.propnet.structure.PropNetStructureFactory;

public class PropNetStateMachine extends StateMachine {

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
			PropNetStructure structure = factory.create(description);
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

    	return -1;
    }
    /**
     * Returns true if and only if the given state is a terminal state (i.e. the
     * game is over).
     */
    @Override
	public boolean isTerminal(MachineState state) {

    	return true;
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

    	return new ArrayList<Role>();
    }
    /**
     * Returns the initial state of the game.
     */
    @Override
	public MachineState getInitialState() {

    	return new SimpleMachineState();
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

    	return new ArrayList<Move>();
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

    	return new SimpleMachineState();
    }
}
