package gameproject.player;

import gameproject.game.elements.Play;
import gameproject.game.elements.Round;

import java.util.List;

/**
 * Class that lets the Human interact with the game by choosing from the list of possible
 * moves at each turn using the GUI instead of following an algorithm
 *
 * @author Sven
 * @author Alexandre
 */
public class Human implements Strategy {

    /**
     * Lets the human choose the moves that should be applied.
     * Method should not be called normally as it will throw an exception.
     *
     * @param possiblePlays the possible legal plays
     * @param round the current round of play
     * @return should return nothing.
     * @throws UnsupportedOperationException humans should make a play via the GUI instead
     */
    @Override
    public List<Play> chooseMove(List<Play> possiblePlays, Round round) {
        throw new UnsupportedOperationException("Human players make a move by clicking the checkers on the interface");
    }

    /**
     * Human is not an AI-agent hence we return false.
     *
     * @return false, as it's not AI.
     */
    @Override
    public boolean isAI() {
        return false;
    }
}
