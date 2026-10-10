package gameproject.ai;

import gameproject.game.elements.Play;
import gameproject.game.elements.Round;
import gameproject.player.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Class representing an AI agent in its easy form.
 * It uses a recursive searching algorithm that takes the current board state and dice throw
 * and searches through all possible legal move sequences within the current turn.
 * Each resulting board position is evaluated using a heuristic, after which the move sequence
 * resulting in the highest evaluation is chosen.
 *
 * @author Sven
 */
public class EasyAgent extends AIAgent {
    /**
     * Stores the current best moves found by the algorithm
     */
    private List<Play> bestMoves;
    /**
     * Stores the current best value found by the algorithm
     */
    private double bestValue;

    /**
     * Create a new Easy AI agent based on the evaluator function passed
     * @param evaluator the heuristic function
     */
    public EasyAgent(BoardEvaluator evaluator) {
        super(evaluator);
    }

    /**
     * Searches through all possible legal move sequences within the current turn and chooses
     * the sequence resulting in the highest board evaluation.
     *
     * @param possiblePlays the possible legal plays at the start of the turn
     * @param round the current round being played
     * @return the sequence of plays with the highest board evaluation
     */
    @Override
    public List<Play> chooseMove(List<Play> possiblePlays, Round round) {
        // At the beginning of the algorithm set the maximum value low and create an empty set of best moves.
        bestValue = Double.NEGATIVE_INFINITY;
        bestMoves = new ArrayList<>();
        Player.Color color = round.getCurrentPlayer().getColor();

        // For each possible play we apply the first step and see if it's objective value is the best.
        for (Play play : possiblePlays) {
            Round newRound = new Round(round);
            newRound.executePlay(play);

            // Add the played move
            List<Play> moves = new ArrayList<>();
            moves.add(play);

            // Continue down till all moves are played
            continueTurn(newRound, moves, color);
        }
        return bestMoves;
    }

    /**
     * Recursive helper method which adds all possible plays that are available in a turn to possible turns
     * and checks if it's the best option it has seen based on the evaluator and stores the best moves and values
     *
     * @param round the current snapshot of the round from which we start
     * @param movesMade the moves which have been made previously
     * @param color the color of which the turn belongs
     */
    private void continueTurn(Round round, List<Play> movesMade, Player.Color color) {

        // Get the possible required plays with the remaining dice
        List<Play> possiblePlays = round.getRequiredPlays(round.getMoves());

        // If no moves are available anymore, evaluate the current position
        if (possiblePlays.isEmpty()) {
            double value = getEvaluator().evaluate(round.getCurrentBoard(), color);

            // Store if it is better than the ones seen before
            if (value > bestValue) {
                bestValue = value;
                bestMoves = new ArrayList<>(movesMade);
            }
            return;
        }

        // For each possible play, execute it
        for (Play play : possiblePlays) {
            Round newRound = new Round(round);
            newRound.executePlay(play);

            // Add the move
            List<Play> newMoves = new ArrayList<>(movesMade);
            newMoves.add(play);

            // Search for more moves
            continueTurn(newRound, newMoves, color);
        }
    }
}
