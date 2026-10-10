package gameproject.ai;

// AI-assisted (GitHub Copilot Chat), reviewed and adapted by Kilian.
// If committed, disclose this assistance with:
// AI-Assisted-By: GitHub Copilot Chat

import gameproject.game.elements.Board;
import gameproject.game.elements.Dice;
import gameproject.game.elements.Play;
import gameproject.game.elements.Point;
import gameproject.game.elements.Round;
import gameproject.game.elements.Location;
import gameproject.player.Player;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies that AdvancedAgent's search move mutation is exactly reversible.
 */
class AdvancedAgentApplyUndoTest {

    @Test
    void normalMoveRestoresState() {
        Round round = createRound(1, 2, Player.Color.WHITE);
        Play play = findPlay(round, Play.Type.NORMAL, 1);

        assertApplyAndUndoRestoresState(round, play);
    }

    @Test
    void normalMoveThatHitsABlotRestoresState() {
        Round round = createCustomRound(
                Player.Color.WHITE,
                5,
                4
        );
        Play play = findPlay(round, Play.Type.NORMAL, 1);

        assertApplyAndUndoRestoresState(round, play);
    }

    @Test
    void bearOffRestoresState() {
        Round round = createBearOffRound();
        // One checker with dice 1 and 2: only one die can be used, so the rules require the higher die (2)
        Play play = findPlay(round, Play.Type.BEAR_OFF, 2);

        assertApplyAndUndoRestoresState(round, play);
    }

    @Test
    void barEntryRestoresState() {
        Round round = createBarEntryRound(false);
        Play play = findPlay(round, Play.Type.OFF_BAR, 1);

        assertApplyAndUndoRestoresState(round, play);
    }

    @Test
    void barEntryThatHitsABlotRestoresState() {
        Round round = createBarEntryRound(true);
        Play play = findPlay(round, Play.Type.OFF_BAR, 1);

        assertApplyAndUndoRestoresState(round, play);
    }

    @Test
    void doubleRollRestoresAllFourRemainingMoves() {
        Round round = createRound(1, 1, Player.Color.WHITE);
        Play play = findPlay(round, Play.Type.NORMAL, 1);

        assertApplyAndUndoRestoresState(round, play);
    }

    @Test
    void completeTurnHelperRestoresOnePlayTurn() {
        Round round = createRound(1, 2, Player.Color.WHITE);
        Play play = findPlay(round, Play.Type.NORMAL, 1);

        assertCompleteTurnApplyAndUndoRestoresState(round, List.of(play));
    }

    @Test
    void completeTurnHelperRestoresMultiPlayTurn() {
        Round round = createRound(1, 2, Player.Color.WHITE);
        List<Play> turn = findTurnWithMinimumLength(round, 2);

        assertCompleteTurnApplyAndUndoRestoresState(round, turn);
    }

    @Test
    void completeTurnHelperRestoresTurnContainingHit() {
        Round round = createCustomRound(
                Player.Color.WHITE,
                5,
                4
        );
        List<Play> turn = findTurnContaining(
                round,
                Play.Type.NORMAL
        );

        assertCompleteTurnApplyAndUndoRestoresState(round, turn);
    }

    @Test
    void completeTurnHelperRestoresBearOffTurn() {
        Round round = createBearOffRound();
        List<Play> turn = findTurnContaining(
                round,
                Play.Type.BEAR_OFF
        );

        assertCompleteTurnApplyAndUndoRestoresState(round, turn);
    }

    @Test
    void completeTurnHelperRestoresBarEntryTurn() {
        Round round = createBarEntryRound(false);
        List<Play> turn = findTurnContaining(
                round,
                Play.Type.OFF_BAR
        );

        assertCompleteTurnApplyAndUndoRestoresState(round, turn);
    }

    @Test
    void completeTurnHelperRestoresFourPlayDoubleTurn() {
        Round round = createRound(1, 1, Player.Color.WHITE);
        List<Play> turn = findTurnWithMinimumLength(round, 4);

        assertCompleteTurnApplyAndUndoRestoresState(round, turn);
    }

    @Test
    void chooseMoveRestoresInputRoundAfterRecursiveTurnGeneration() {
        assertChooseMoveRestoresInputRound(1, 2);
    }

    @Test
    void chooseMoveRestoresInputRoundAfterDoubleRoll() {
        assertChooseMoveRestoresInputRound(1, 1);
    }

    private void assertApplyAndUndoRestoresState(Round round, Play play) {
        AdvancedAgent agent = new AdvancedAgent(
                new BoardEvaluator(deterministicCoefficients()),
                0
        );
        StateSnapshot before = snapshot(round);

        Object undo = invokeApply(agent, round, play);
        invokeUndo(agent, round, undo);

        assertStateEquals(before, snapshot(round));
    }

    private void assertCompleteTurnApplyAndUndoRestoresState(
            Round round,
            List<Play> turn) {
        AdvancedAgent agent = new AdvancedAgent(
                new BoardEvaluator(deterministicCoefficients()),
                0
        );
        StateSnapshot before = snapshot(round);
        Object undoRecords = invokeApplyTurn(agent, round, turn);

        try {
            assertStateNotEqual(before, snapshot(round));
        } finally {
            invokeUndoTurn(agent, round, undoRecords);
        }

        assertStateEquals(before, snapshot(round));
    }

    private void assertChooseMoveRestoresInputRound(int die1, int die2) {
        Round round = createRound(die1, die2, Player.Color.WHITE);
        List<Play> possiblePlays =
                round.getRequiredPlays(round.getMoves());
        StateSnapshot before = snapshot(round);

        AdvancedAgent agent = new AdvancedAgent(
                new BoardEvaluator(deterministicCoefficients()),
                0
        );

        agent.chooseMove(possiblePlays, round);

        assertStateEquals(before, snapshot(round));
    }

    private Object invokeApply(
            AdvancedAgent agent,
            Round round,
            Play play) {
        try {
            Method method = AdvancedAgent.class.getDeclaredMethod(
                    "applyPlayForSearch",
                    Round.class,
                    Play.class
            );
            method.setAccessible(true);
            return method.invoke(agent, round, play);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Could not invoke applyPlayForSearch", exception);
        }
    }

    private Object invokeApplyTurn(
            AdvancedAgent agent,
            Round round,
            List<Play> turn) {
        try {
            Method method = AdvancedAgent.class.getDeclaredMethod(
                    "applyTurnForSearch",
                    Round.class,
                    List.class
            );
            method.setAccessible(true);
            return method.invoke(agent, round, turn);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(
                    "Could not invoke applyTurnForSearch",
                    exception
            );
        }
    }

    private void invokeUndo(
            AdvancedAgent agent,
            Round round,
            Object undo) {
        try {
            Method method = AdvancedAgent.class.getDeclaredMethod(
                    "undoPlayForSearch",
                    Round.class,
                    undo.getClass()
            );
            method.setAccessible(true);
            method.invoke(agent, round, undo);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Could not invoke undoPlayForSearch", exception);
        }
    }

    private void invokeUndoTurn(
            AdvancedAgent agent,
            Round round,
            Object undoRecords) {
        try {
            Method method = AdvancedAgent.class.getDeclaredMethod(
                    "undoTurnForSearch",
                    Round.class,
                    Deque.class
            );
            method.setAccessible(true);
            method.invoke(agent, round, undoRecords);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(
                    "Could not invoke undoTurnForSearch",
                    exception
            );
        }
    }

    private List<Play> findTurnWithMinimumLength(
            Round round,
            int minimumLength) {
        List<Play> possiblePlays =
                round.getRequiredPlays(round.getMoves());
        return invokeOptimizedTurnGenerator(
                new AdvancedAgent(
                        new BoardEvaluator(deterministicCoefficients()),
                        0
                ),
                round,
                possiblePlays
        ).stream()
                .filter(turn -> turn.size() >= minimumLength)
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "No turn with at least " + minimumLength + " plays"
                ));
    }

    private List<Play> findTurnContaining(
            Round round,
            Play.Type type) {
        List<Play> possiblePlays =
                round.getRequiredPlays(round.getMoves());
        return invokeOptimizedTurnGenerator(
                new AdvancedAgent(
                        new BoardEvaluator(deterministicCoefficients()),
                        0
                ),
                round,
                possiblePlays
        ).stream()
                .filter(turn -> turn.stream()
                        .anyMatch(play -> play.getType() == type))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "No turn containing " + type + " was generated"
                ));
    }

    /**
     * Calls the agent's turn generator. getPossibleTurns now builds the turns
     * itself from the round, so possiblePlays is only kept for the callers.
     */
    private List<List<Play>> invokeOptimizedTurnGenerator(
            AdvancedAgent agent,
            Round round,
            List<Play> possiblePlays) {
        return agent.getPossibleTurns(round);
    }

    private StateSnapshot snapshot(Round round) {
        Board board = round.getCurrentBoard();
        int[] points = new int[48];

        for (int i = 0; i < board.getPoints().size(); i++) {
            Point point = board.getPoints().get(i);
            points[i * 2] = point.getCheckers(Player.Color.WHITE);
            points[i * 2 + 1] = point.getCheckers(Player.Color.BLACK);
        }

        int[] bar = locationCounts(board.getBar());
        int[] offBoard = locationCounts(board.getOffBoard());
        int[] moves = round.getMoves() == null
                ? null
                : round.getMoves().clone();

        return new StateSnapshot(points, bar, offBoard, moves);
    }

    private int[] locationCounts(Location location) {
        return new int[]{
                location.getCheckers(Player.Color.WHITE),
                location.getCheckers(Player.Color.BLACK)
        };
    }

    private void assertStateEquals(
            StateSnapshot expected,
            StateSnapshot actual) {
        assertArrayEquals(expected.points(), actual.points(), "point counts");
        assertArrayEquals(expected.bar(), actual.bar(), "bar counts");
        assertArrayEquals(expected.offBoard(), actual.offBoard(), "off-board counts");

        if (expected.moves() == null || actual.moves() == null) {
            assertEquals(expected.moves(), actual.moves(), "remaining moves");
        } else {
            assertArrayEquals(expected.moves(), actual.moves(), "remaining moves");
        }
    }

    private void assertStateNotEqual(
            StateSnapshot expected,
            StateSnapshot actual) {
        boolean samePoints = Arrays.equals(expected.points(), actual.points());
        boolean sameBar = Arrays.equals(expected.bar(), actual.bar());
        boolean sameOffBoard = Arrays.equals(
                expected.offBoard(),
                actual.offBoard()
        );
        boolean sameMoves = Arrays.equals(
                expected.moves(),
                actual.moves()
        );

        if (samePoints && sameBar && sameOffBoard && sameMoves) {
            throw new AssertionError(
                    "Applying the complete turn did not change the round"
            );
        }
    }

    private Round createRound(
            int die1,
            int die2,
            Player.Color color) {
        Round base = new Round(
                new Player("White", Player.Color.WHITE, null),
                new Player("Black", Player.Color.BLACK, null),
                new Dice(1, 2, new FixedRandom(0, 1))
        );
        forceTurn(base, color);
        return new Round(base, new Dice(die1, die2));
    }

    private Round createCustomRound(
            Player.Color color,
            int whitePoint,
            int blackPoint) {
        Round round = createRound(1, 2, color);
        clearBoard(round);
        placeChecker(round, Player.Color.WHITE, whitePoint);
        placeChecker(round, Player.Color.BLACK, blackPoint);
        return round;
    }

    private Round createBearOffRound() {
        Round round = createRound(1, 2, Player.Color.WHITE);
        clearBoard(round);
        placeChecker(round, Player.Color.WHITE, 0);
        return round;
    }

    private Round createBarEntryRound(boolean withBlot) {
        Round round = createRound(1, 2, Player.Color.WHITE);
        clearBoard(round);
        placeCheckerOnBar(round, Player.Color.WHITE);
        if (withBlot) {
            placeChecker(round, Player.Color.BLACK, 23);
        }
        return round;
    }

    private Play findPlay(
            Round round,
            Play.Type type,
            int die) {
        List<Play> plays = round.getRequiredPlays(round.getMoves());
        return plays.stream()
                .filter(play -> play.getType() == type)
                .filter(play -> play.getDieUsed() == die)
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "No " + type + " play using die " + die + " was generated"
                ));
    }

    private void forceTurn(Round round, Player.Color color) {
        round.determineFirstPlayer();
        if (round.checkWhiteTurn() != (color == Player.Color.WHITE)) {
            round.switchPlayer();
        }
    }

    private void clearBoard(Round round) {
        Board board = round.getCurrentBoard();
        for (Point point : board.getPoints()) {
            while (!point.isEmpty(Player.Color.WHITE)) {
                point.removeChecker(Player.Color.WHITE);
                board.getOffBoard().addChecker(Player.Color.WHITE);
            }
            while (!point.isEmpty(Player.Color.BLACK)) {
                point.removeChecker(Player.Color.BLACK);
                board.getOffBoard().addChecker(Player.Color.BLACK);
            }
        }
    }

    private void placeChecker(
            Round round,
            Player.Color color,
            int pointIndex) {
        Board board = round.getCurrentBoard();
        board.getOffBoard().removeChecker(color);
        board.getPoints().get(pointIndex).addChecker(color);
    }

    private void placeCheckerOnBar(
            Round round,
            Player.Color color) {
        Board board = round.getCurrentBoard();
        board.getOffBoard().removeChecker(color);
        board.getBar().addChecker(color);
    }

    private static double[] deterministicCoefficients() {
        double[] coefficients = new double[8];
        for (int i = 0; i < 8; i++) {
            coefficients[i] = 1.0 / 8.0;
        }
        return coefficients;
    }

    private record StateSnapshot(
            int[] points,
            int[] bar,
            int[] offBoard,
            int[] moves) {
    }

    private static final class FixedRandom extends Random {
        private final int[] values;
        private int index;

        private FixedRandom(int... values) {
            this.values = values;
        }

        @Override
        public int nextInt(int bound) {
            if (index >= values.length) {
                throw new IllegalStateException(
                        "Opening roll requested too many values"
                );
            }
            return values[index++];
        }
    }
}
