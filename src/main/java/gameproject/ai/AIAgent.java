package gameproject.ai;

import gameproject.game.elements.Board;
import gameproject.game.elements.Play;
import gameproject.game.elements.Round;
import gameproject.player.Player;
import gameproject.player.Strategy;

import java.util.List;

/**
 * Class that implements the algorithm for an AI agent to be able to interact with the game
 * It has an evaluator which makes him evaluate a certain position as well as being able
 * to check whether a double should be proposed and/or accepted.
 *
 * @author Sven
 * @author Alexandre
 */
public abstract class AIAgent implements Strategy {
    /**
     * The evaluator used to distinguish values of different boards
     */
    private final BoardEvaluator evaluator;

    /**
     * Creates an instance of an AI agent
     *
     * @param evaluator the evaluator it uses
     */
    public AIAgent(BoardEvaluator evaluator) {
        this.evaluator = evaluator;
    }

    /**
     * Gives the evaluator which is used on a certain AI agent
     *
     * @return the evaluator
     */
    public BoardEvaluator getEvaluator() {
        return evaluator;
    }

    /**
     * Algorithm which decides which plays it should choose in a turn
     *
     * @param possiblePlays the possible legal plays
     * @param round the current round that is played
     * @return plays in turn
     */
    public abstract List<Play> chooseMove(List<Play> possiblePlays, Round round);

    /**
     * Determines whether the AI should propose a double.
     * The decision is based on the evaluation of the board for both players.
     *
     * @param round the current round being played
     * @return true if the AI should propose the double
     */
    public boolean shouldDouble(Round round) {

        if (!round.canDouble()) {
            return false;
        }

        return getAdvantage(round) >= 0.20;
    }

    /**
     * Determines whether the AI should accept a double.
     * The decision is based on the evaluation of the board for both players.
     *
     * @param round the current round being played
     * @return true if the AI should accept the double
     */
    public boolean shouldAcceptDouble(Round round) {
        return getAdvantage(round) >= -0.20;
    }

    /**
     * Calculates the AI's relative advantage based on its board evaluator.
     * The advantage is bounded between -1 and 1.
     *
     * @param round the current round being played
     * @return the relative advantage
     */
    private double getAdvantage(Round round) {
        Player.Color aiColor = round.getCurrentPlayer().getColor();
        Board board = round.getCurrentBoard();

        double aiValue = evaluator.evaluate(board, aiColor);
        double opponentValue = evaluator.evaluate(board, aiColor.opponent());

        return (aiValue - opponentValue) / 2.0;
    }

    /**
     * Determines whether the AI should accept a double in the Fog-Of-War game mode.
     * The decision is based on the evaluation of the board for own player.
     *
     * @param round the current round that is played
     * @return whether he should accept a doubling or not
     */
    public boolean shouldAcceptDoubleFOW(Round round) {
        return getAdvantageFOW(round) >= 0.20;
    }

    /**
     * Determines whether the AI should propose a double in the Fog-of-war game mode.
     * The decision is based on the evaluation of the board for own player.
     *
     * @param round the current round being played
     * @return true if the AI should propose the double
     */
    public boolean shouldDoubleFOW(Round round) {
        if (!round.canDouble()) {
            return false;
        }

        return getAdvantageFOW(round) >= 0.60;
    }
    /**
     * Calculates the AI's advantage based on its board evaluator.
     * The advantage is bounded between 0 and 1.
     * Used in the Fog-Of-War implementation
     *
     * @param round the current round being played
     * @return the relative advantage
     */
    private double getAdvantageFOW(Round round) {
        Player.Color aiColor = round.getCurrentPlayer().getColor();
        Board board = round.getCurrentBoard();

        return evaluator.evaluateFOW(board, aiColor);
    }
    @Override
    public boolean isAI() {
        return true;
    }
}

