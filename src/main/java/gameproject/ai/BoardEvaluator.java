package gameproject.ai;

import gameproject.game.elements.Board;
import gameproject.game.elements.Point;
import gameproject.player.Player;

import java.util.List;


/**
 * Class can evaluate the board position based on variables and their corresponding weights.
 * The final evaluation is a double between -1 and 1 (this fact is used in the search algorithm)
 * The weights must be non-negative and sum to one in order to maintain this fact.
 *
 * @author Sven
 * @author Kilian
 * @author Alexandre
 * @author Diederick
 */
public class BoardEvaluator {

    /**
     * The weights of the variables
     */
    private final double[] coefficients;

    /**
     * Constructs a new evaluator which uses the weights as passed to the constructor
     *
     * @param coefficients the weights of variables
     * @throws IllegalArgumentException if weights violate constraints error will be thrown.
     */
    public BoardEvaluator(double[] coefficients) {
        // Check if the correct amount of coefficients is passed.
        if (coefficients.length != 8) {
            throw new IllegalArgumentException("Board evaluator requires exactly 8 coefficients");
        }

        // Check if they weights sum to one.
        double sumLambda = 0.0;
        for (double coefficient : coefficients) {
            if (coefficient < 0 || coefficient > 1) {
                throw new IllegalArgumentException("Coefficient is out of bounds");
            }
            sumLambda += coefficient;
        }

        if (Math.abs(sumLambda - 1.0) > 1e-9) {
            throw new IllegalArgumentException("Coefficients do not sum to one");
        }
        this.coefficients = coefficients;
    }

    /**
     * Evaluates the given board position using eight weighted variables.
     * The value returned is bounded between [-1,1].
     * Uses perfect information from both players and is therefore not used for Fog-Of-War.
     *
     * @param board the board position to evaluate
     * @param color the color for which the board position is evaluated
     * @return the weighted objective value of the board position
     */
    public double evaluate(Board board, Player.Color color) {
        double[] ownVariables = getVariables(board, color);
        double[] opponentVariables = getVariables(board, color.opponent());

        double value = 0;
        for (int i = 0; i < coefficients.length; i++){
            value += coefficients[i]*(ownVariables[i]-opponentVariables[i]);
        }
        return value;
    }

    /**
     * Evaluates the given board position for Fog-Of-War using only information
     * about the player's own checkers.
     * The returned value is bounded between [0,1].
     *
     * @param board the board position to evaluate
     * @param color the color for which the board position is evaluated
     * @return the weighted objective value of the player's own position
     */
    public double evaluateFOW(Board board, Player.Color color) {
        double[] variables = getVariables(board, color);

        double value = 0;
        for (int i = 0; i < coefficients.length; i++) {
            value += coefficients[i]*variables[i];
        }
        return value;
    }

    /**
     * Calculates the eight normalized variables used by the board evaluator
     * for one player. All variables are bounded between [0,1], where a higher
     * value represents a better position.
     *
     * @param board the board position to evaluate
     * @param color the color for which the variables are calculated
     * @return an array containing the eight normalized variables
     */
    private double[] getVariables(Board board, Player.Color color) {
        List<Point> points = board.getPoints();

        // Variables
        int distance = 0;
        int madePoints = 0;
        int blots = 0;
        int safeCheckers = 0;
        int homeCheckers = 0;
        int homeMadePoints = 0;

        // Iterate once over the board and update after each point the variables
        for (int i = 0; i < points.size(); i++) {
            int checkersOnPoint = points.get(i).getCheckers(color);
            if (checkersOnPoint > 0) {
                int checkerDistance = (color == Player.Color.WHITE) ? i + 1 : 24 - i;
                distance += checkersOnPoint * checkerDistance;
                boolean home = (color == Player.Color.WHITE) ? i < 6 : i > 17;
                if (home) {
                    homeCheckers += checkersOnPoint;
                }
                if (checkersOnPoint == 1) {
                    blots++;
                } else {
                    madePoints++;
                    safeCheckers += checkersOnPoint;
                    if (home) {
                        homeMadePoints++;
                    }
                }
            }
        }

        // Get checkers on the bar and offboard
        int barCheckers = board.getBar().getCheckers(color);
        int offBoardCheckers = board.getOffBoard().getCheckers(color);

        // Final updates
        distance += barCheckers * 25;
        safeCheckers += offBoardCheckers;

        double[] variables = new double[8];

        // Normalize each variable and make 0: bad and 1: good
        variables[0] = 1.0 - distance / 375.0;
        variables[1] = 1.0 - barCheckers / 15.0;
        variables[2] = offBoardCheckers / 15.0;
        variables[3] = madePoints / 7.0;
        variables[4] = 1.0 - blots / 15.0;
        variables[5] = safeCheckers / 15.0;
        variables[6] = homeCheckers / 15.0;
        variables[7] = homeMadePoints / 6.0;

        return variables;
    }

}
