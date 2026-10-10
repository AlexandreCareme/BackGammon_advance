package gameproject.ai;

// AI-assisted (GitHub Copilot Chat), reviewed and adapted by Kilian.
// If committed, disclose this assistance with:
// AI-Assisted-By: GitHub Copilot Chat

import gameproject.game.elements.Board;
import gameproject.game.elements.Location;
import gameproject.game.elements.Point;
import gameproject.player.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for BoardEvaluator.
 * The feature tests use one hand-built board where every feature has a different,
 * non-zero value, so a feature that reads the wrong variable gives the wrong number.
 */
class BoardEvaluatorTest {

    private static final double EPS = 1e-12;
    private static final Player.Color WHITE = Player.Color.WHITE;
    private static final Player.Color BLACK = Player.Color.BLACK;

    // ---------- constructor validation ----------

    @Test
    void rejectsWrongNumberOfCoefficients() {
        assertThrows(IllegalArgumentException.class, () -> new BoardEvaluator(new double[9]));
    }

    @Test
    void rejectsCoefficientsThatDoNotSumToOne() {
        double[] c = new double[8];
        c[0] = 0.5;
        assertThrows(IllegalArgumentException.class, () -> new BoardEvaluator(c));
    }

    @Test
    void rejectsNegativeCoefficient() {
        double[] c = new double[8];
        c[0] = 1.5;
        c[1] = -0.5;
        assertThrows(IllegalArgumentException.class, () -> new BoardEvaluator(c));
    }

    // ---------- evaluate ----------

    @Test
    void startingBoardIsWorthZeroForEveryFeature() {
        // The starting position is symmetric, so every difference feature is 0.
        Board board = new Board();
        for (int feature = 0; feature < 8; feature++) {
            BoardEvaluator evaluator = new BoardEvaluator(onlyFeature(feature));
            assertEquals(0.0, evaluator.evaluate(board, WHITE), EPS, "feature " + feature);
            assertEquals(0.0, evaluator.evaluate(board, BLACK), EPS, "feature " + feature);
        }
    }

    @Test
    void eachFeatureMatchesHandCalculation() {
        // Expected values from WHITE's perspective for testBoard(); see the comments there.
        double[] expected = {
                32.0 / 375.0, // 0 distance:        black 50 - white 18
                1.0 / 15.0,   // 1 on bar:          black 1 - white 0
                2.0 / 15.0,   // 2 off board:       white 2 - black 0
                1.0 / 7.0,    // 3 made points:     white 2 - black 1
                1.0 / 15.0,   // 4 blots:           black 2 - white 1
                5.0 / 15.0,   // 5 safe checkers:   white 7 - black 2
                3.0 / 15.0,   // 6 home checkers:   white 5 - black 2
                1.0 / 6.0     // 7 home made points: white 2 - black 1
        };

        Board board = testBoard();
        for (int feature = 0; feature < 8; feature++) {
            BoardEvaluator evaluator = new BoardEvaluator(onlyFeature(feature));
            assertEquals(expected[feature], evaluator.evaluate(board, WHITE), EPS,
                    "feature " + feature + " for WHITE");
            assertEquals(-expected[feature], evaluator.evaluate(board, BLACK), EPS,
                    "feature " + feature + " for BLACK");
        }
    }

    @Test
    void weightsCombineLinearly() {
        // Equal weights: the result must be the average of the eight single-feature values.
        double[] c = new double[8];
        for (int i = 0; i < 8; i++) {
            c[i] = 1.0 / 8.0;
        }
        Board board = testBoard();

        double sum = 0.0;
        for (int feature = 0; feature < 8; feature++) {
            sum += new BoardEvaluator(onlyFeature(feature)).evaluate(board, WHITE);
        }
        assertEquals(sum / 8.0, new BoardEvaluator(c).evaluate(board, WHITE), EPS);
    }

    // ---------- helpers ----------

    /** Coefficients with weight 1 on one feature and 0 on all others. */
    private static double[] onlyFeature(int index) {
        double[] c = new double[8];
        c[index] = 1.0;
        return c;
    }

    /**
     * Builds this position (index = list index in board.getPoints()):
     *
     * White: idx 0 → 3, idx 1 → 2, idx 10 → 1, off board 2, bar 0
     * Black: idx 23 → 2, idx 12 → 1, idx 13 → 1, bar 1, off board 0
     *
     * White: distance 18, made 2, blots 1, safe 3+2+2off = 7, home checkers 5, home made 2
     * Black: distance 50, made 1, blots 2, safe 2,            home checkers 2, home made 1
     *
     * Checker totals don't add up to 15; evaluate() doesn't check that.
     */
    private static Board testBoard() {
        Board board = new Board();
        for (Point point : board.getPoints()) {
            point.restoreCounts(0, 0);
        }
        set(board.getPoints().get(0), 3, 0);
        set(board.getPoints().get(1), 2, 0);
        set(board.getPoints().get(10), 1, 0);
        set(board.getPoints().get(12), 0, 1);
        set(board.getPoints().get(13), 0, 1);
        set(board.getPoints().get(23), 0, 2);
        set(board.getBar(), 0, 1);
        set(board.getOffBoard(), 2, 0);
        return board;
    }

    private static void set(Location location, int white, int black) {
        location.restoreCounts(white, black);
    }
}