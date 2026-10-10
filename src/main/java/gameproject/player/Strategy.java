package gameproject.player;

import gameproject.game.elements.Play;
import gameproject.game.elements.Round;

import java.util.List;

/**
 * Interface that represents a strategy used for choosing between legal moves.
 *
 * @author Sven
 * @author Alexandre
 */
public interface Strategy {
    /**
     * Chooses a move or sequence of moves from the available legal moves according to the algorithm used
     *
     * @param possiblePlays the possible legal plays
     * @param round the current round that is played
     * @return the chosen play or plays
     */
    List<Play> chooseMove(List<Play> possiblePlays, Round round);

    /**
     * Checks if this player is controlled by AI
     *
     * @return true if the player is AI
     */
    boolean isAI();
}