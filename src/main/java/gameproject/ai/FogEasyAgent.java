package gameproject.ai;

import gameproject.game.elements.Play;
import gameproject.game.elements.Round;
import gameproject.player.Player;
import java.util.ArrayList;
import java.util.List;

/**
 * Easy AI agent for Fog of War games
 * The AI evaluated its own position and gives a small bonus for moves that
 * hit a visible opponent blot
 *
 * @author Alexandre
 * @author Sven
 */
public class FogEasyAgent extends AIAgent {

    private double bestValue;
    private List<Play> bestMoves;
    private FogAIInformation fogInformation = new FogAIInformation();

    /**
     * Creates an Easy Fog of War AI agent
     * @param evaluator board evaluator used to evaluate positions
     */
    public FogEasyAgent(BoardEvaluator evaluator) {
        super(evaluator);
    }

    /**
     * Chooses the sequence of moves with the highest evaluated value.
     * The AI considers all possible moves for the current turn.
     * @param possiblePlay the possible legal plays available to the AI
     * @param round the current round that is played
     * @return best sequence of plays for the turn
     */
    @Override
    public List<Play> chooseMove(List<Play> possiblePlay, Round round){
        bestValue = -1000;
        bestMoves = new ArrayList<Play>();

        Player.Color color = round.getCurrentPlayer().getColor();
        for (Play play : possiblePlay) {
            int hits = 0;
            if(fogInformation.isVisibleBlot(round, play, color)){
                hits++;
            }
            Round newRound = new Round(round);
            newRound.executePlay(play);

            List<Play> moves = new ArrayList<>();
            moves.add(play);

            continueTurn(newRound, moves, color, hits);
        }
        return bestMoves;
    }

    /**
     * Continues evaluating possible moves until all moves for the turn have
     * been used or no legal moves remain
     * @param round current game round
     * @param movesMade moves already selected during the turn
     * @param color colour of the AI
     */
    private void continueTurn(Round round, List<Play> movesMade, Player.Color color, int hits) {
        if(!round.hasLegalMoves()){
            /*
             * Give a small bonus for moves that hit a visible opponent blot
             */
           double value = getEvaluator().evaluateFOW(round.getCurrentBoard(), color);
           value += hits;
            /*
             * Keep the move sequence with the highest evaluation
             */
            if(value > bestValue){
                bestValue = value;
                bestMoves = new ArrayList<>(movesMade);
            }
            return;
        }
        List<Play> possiblePlays = round.getRequiredPlays(round.getMoves());
        for(Play play : possiblePlays){
            int newHits = hits;
            if(fogInformation.isVisibleBlot(round, play, color)){
                newHits++;
            }
            Round newRound = new Round(round);
            newRound.executePlay(play);
            List<Play> newMoves = new ArrayList<>(movesMade);
            newMoves.add(play);
            continueTurn(newRound, newMoves, color, newHits);
        }
    }
}
