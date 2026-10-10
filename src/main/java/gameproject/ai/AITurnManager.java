package gameproject.ai;

import gameproject.game.elements.Play;
import gameproject.game.elements.Round;
import gameproject.player.Player;
import java.util.List;

/**
 * Handles choosing moves for a computer-controlled player
 */
public class AITurnManager {

    private final Round round;
    private boolean active = true;

    /**
     * Creates an AI turn manager for a round
     * @param round the current game round
     */
    public AITurnManager(Round round) {
        this.round = round;
    }

    /**
     * Chooses the moves for the current player
     *
     * @return the moves chosen by AI
     */

    /** -JAVADOC- */
    public List<Play> chooseMoves(){

        if(!active){
            return List.of();
        }

        Player currentPlayer  = round.getCurrentPlayer();


        if (currentPlayer == null){
            return List.of();
        }
        if (!currentPlayer.isAI()){
            return List.of();
        }

        List<Play> possiblePlays = round.getRequiredPlays(round.getMoves());
        return currentPlayer.getStrategy().chooseMove(possiblePlays, round);
    }

    /**
     * Stops this AI turn manager
     */
    public void stop(){
        active = false;
    }

}
