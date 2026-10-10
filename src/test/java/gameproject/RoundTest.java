package gameproject;

// AI-assisted (GitHub Copilot Chat), reviewed and adapted by <your name>.
// Per AGENTS.md: test code only (src/test), disclosed here and in the
// commit that adds it (add an `AI-Assisted-By: GitHub Copilot Chat` trailer, and fill
// in the PR's AI-use disclosure checklist).
//
// Scope: exercises the game-rule logic that lives in Round — which plays
// are legal, and which of the legal plays are actually *required* by the
// dice rules. In particular:
//   - a checker on the bar must re-enter before anything else can move,
//     and if every entry point is blocked, there are no legal plays at all;
//   - a player must use as many of their dice as the position allows, even
//     if a "smaller" individual play looks tempting;
//   - if only one of two different dice can be used at all, the higher one
//     must be the one played;
//   - bearing off with a die bigger than needed is only allowed once no
//     checker is still further from home.
//
// Every scenario below builds a specific board position by hand (via
// Board's public checker-adding methods) rather than relying on
// setupBoard() or real dice throws, so the outcome is deterministic.
// Tests that need a predictable dice sequence inject Dice with a fixed
// initial roll and a seeded Random instance.

import gameproject.game.elements.*;
import gameproject.player.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RoundTest {

    private Player whitePlayer;
    private Player blackPlayer;

    @BeforeEach
    void setUp() {
        whitePlayer = new Player("White", Player.Color.WHITE, null);
        blackPlayer = new Player("Black", Player.Color.BLACK, null);
    }

    /**
     * determineFirstPlayer() throws real dice, so we can't predict who it
     * picks - only that it always lands on one of the two real players
     * (never leaves currentPlayer null/undetermined).
     */
    private void forceTurn(Round round, Player.Color color) {
        round.determineFirstPlayer();
        boolean isWhiteNow = round.checkWhiteTurn();
        boolean wantWhite = color == Player.Color.WHITE;
        if (isWhiteNow != wantWhite) {
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

    private void placeChecker(Round round, Player.Color color, int pointIndex) {
        Board board = round.getCurrentBoard();
        board.getOffBoard().removeChecker(color);
        board.getPoints().get(pointIndex).addChecker(color);
    }

    private void placeCheckerOnBar(Round round, Player.Color color) {
        Board board = round.getCurrentBoard();
        board.getOffBoard().removeChecker(color);
        board.getBar().addChecker(color);
    }

    // ---- basic turn state ---------------------------------------------------

    @Test
    void determineFirstPlayerAlwaysPicksAnActualPlayerTest() {
        Round round = new Round(whitePlayer, blackPlayer);
        round.determineFirstPlayer();

        Player current = round.getCurrentPlayer();
        assertTrue(current == round.getWhite() || current == round.getBlack());
    }

    @Test
    void switchPlayerTogglesBetweenWhiteAndBlackTest() {
        Round round = new Round(whitePlayer, blackPlayer);
        forceTurn(round, Player.Color.WHITE);

        assertTrue(round.checkWhiteTurn());
        round.switchPlayer();
        assertFalse(round.checkWhiteTurn());
        round.switchPlayer();
        assertTrue(round.checkWhiteTurn());
    }

    // ---- dice move expansion -------------------------------------------------

    @Test
    void allMovesReturnsBothDiceForARegularRollTest() {
        Round round = new Round(whitePlayer, blackPlayer);

        int[] moves = round.allMoves(new Dice(4, 6));

        assertArrayEquals(new int[]{4, 6}, moves);
    }

    @Test
    void allMovesReturnsFourCopiesForADoubleRollTest() {
        Round round = new Round(whitePlayer, blackPlayer);

        int[] moves = round.allMoves(new Dice(5, 5));

        assertArrayEquals(new int[]{5, 5, 5, 5}, moves);
    }

    @Test
    void startTurnUsesTheDiceValuesFromFirstPlayerDeterminationTest() {
        Dice dice = new Dice(4, 6, new Random(12345L));
        Round round = new Round(whitePlayer, blackPlayer, dice);

        round.determineFirstPlayer();
        round.startTurn();

        assertArrayEquals(new int[]{2, 5}, round.getMoves());
    }

    @Test
    void startTurnThrowsDiceOnTheNextTurnTest() {
        Dice dice = new Dice(4, 6, new Random(12345L));
        Round round = new Round(whitePlayer, blackPlayer, dice);

        round.determineFirstPlayer();
        round.startTurn();
        round.startTurn();

        assertArrayEquals(new int[]{4, 1}, round.getMoves());
    }

    @Test
    void determineFirstPlayerUsesTheInjectedRandomSequenceTest() {
        Dice dice = new Dice(1, 1, new Random(12345L));
        Round round = new Round(whitePlayer, blackPlayer, dice);

        round.determineFirstPlayer();

        assertEquals(round.getBlack(), round.getCurrentPlayer());
        assertEquals(2, round.getRoundDice().getDice1());
        assertEquals(5, round.getRoundDice().getDice2());
    }

    // ---- checkBearOff / checkBar --------------------------------------------

    @Test
    void checkBearOffIsTrueOnlyWhenAllOfThatColorsCheckersAreHomeTest() {
        Round round = new Round(whitePlayer, blackPlayer);
        forceTurn(round, Player.Color.WHITE);
        clearBoard(round);
        placeChecker(round, Player.Color.WHITE, 2);
        placeChecker(round, Player.Color.WHITE, 5);

        assertTrue(round.checkBearOff());
    }

    @Test
    void checkBearOffIsTrueForBlackWhenAllBlackCheckersAreHomeTest() {
        Round round = new Round(whitePlayer, blackPlayer);
        forceTurn(round, Player.Color.BLACK);
        clearBoard(round);
        placeChecker(round, Player.Color.BLACK, 23);

        assertTrue(round.checkBearOff());
    }

    @Test
    void checkBearOffIsFalseWhenACheckerIsOutsideTheHomeBoardTest() {
        Round round = new Round(whitePlayer, blackPlayer);
        forceTurn(round, Player.Color.WHITE);
        clearBoard(round);
        placeChecker(round, Player.Color.WHITE, 10);

        assertFalse(round.checkBearOff());
    }

    @Test
    void checkBearOffIsFalseForBlackWhenACheckerIsOutsideTheHomeBoardTest() {
        Round round = new Round(whitePlayer, blackPlayer);
        forceTurn(round, Player.Color.BLACK);
        clearBoard(round);
        placeChecker(round, Player.Color.BLACK, 17);

        assertFalse(round.checkBearOff());
    }

    @Test
    void checkBearOffIsFalseWhenCurrentPlayerHasACheckerOnTheBarTest() {
        Round round = new Round(whitePlayer, blackPlayer);
        forceTurn(round, Player.Color.WHITE);
        clearBoard(round);
        placeChecker(round, Player.Color.WHITE, 2);
        placeCheckerOnBar(round, Player.Color.WHITE);

        assertFalse(round.checkBearOff());
    }

    @Test
    void checkBarReflectsWhetherCurrentPlayerHasACheckerOnTheBarTest() {
        Round round = new Round(whitePlayer, blackPlayer);
        forceTurn(round, Player.Color.WHITE);
        clearBoard(round);

        assertFalse(round.barOccupied());

        placeCheckerOnBar(round, Player.Color.WHITE);
        assertTrue(round.barOccupied());
    }

    @Test
    void checkBarReflectsBlackCheckerOnTheBarTest() {
        Round round = new Round(whitePlayer, blackPlayer);
        forceTurn(round, Player.Color.BLACK);
        clearBoard(round);

        assertFalse(round.barOccupied());
        placeCheckerOnBar(round, Player.Color.BLACK);
        assertTrue(round.barOccupied());
    }

    // ---- re-entering from the bar --------------------------------------------

    @Test
    void moveFromBarIsBlockedWhenTheEntryPointIsMadeByTheOpponentTest() {
        Round round = new Round(whitePlayer, blackPlayer);
        forceTurn(round, Player.Color.WHITE);
        clearBoard(round);
        placeCheckerOnBar(round, Player.Color.WHITE);

        int die = 3;
        int entryIndex = 24 - die; // white enters into black's home board
        placeChecker(round, Player.Color.BLACK, entryIndex);
        placeChecker(round, Player.Color.BLACK, entryIndex);

        assertTrue(round.moveFromBar(die).isEmpty());
    }

    @Test
    void moveFromBarEntersOnTheOpenPointForThatDieTest() {
        Round round = new Round(whitePlayer, blackPlayer);
        forceTurn(round, Player.Color.WHITE);
        clearBoard(round);
        placeCheckerOnBar(round, Player.Color.WHITE);

        int die = 3;
        List<Play> plays = round.moveFromBar(die);

        assertEquals(1, plays.size());
        assertEquals(24 - die, plays.get(0).getForwardPoint());
    }

    @Test
    void moveFromBarEntersBlackOnDieMinusOneTest() {
        Round round = new Round(whitePlayer, blackPlayer);
        forceTurn(round, Player.Color.BLACK);
        clearBoard(round);
        placeCheckerOnBar(round, Player.Color.BLACK);

        List<Play> plays = round.moveFromBar(4);

        assertEquals(1, plays.size());
        assertEquals(4 - 1, plays.get(0).getForwardPoint());
    }

    @Test
    void executingWhiteBarEntryKeepsOtherWhiteBarCheckersAndHitsBlackBlotTest() {
        Round round = new Round(whitePlayer, blackPlayer);
        forceTurn(round, Player.Color.WHITE);
        round.startTurn();

        clearBoard(round);
        placeCheckerOnBar(round, Player.Color.WHITE);
        placeCheckerOnBar(round, Player.Color.WHITE);

        int die = round.getMoves()[0];
        int entryPoint = 24 - die;
        placeChecker(round, Player.Color.BLACK, entryPoint);

        Play play = new Play(entryPoint, Play.Type.OFF_BAR, whitePlayer, die);
        round.executePlay(play);

        assertEquals(1, round.getCurrentBoard().getBar().getCheckers(Player.Color.WHITE));
        assertEquals(1, round.getCurrentBoard().getBar().getCheckers(Player.Color.BLACK));
        assertEquals(1, round.getCurrentBoard().getPoints().get(entryPoint).getCheckers(Player.Color.WHITE));
        assertEquals(0, round.getCurrentBoard().getPoints().get(entryPoint).getCheckers(Player.Color.BLACK));
    }

    @Test
    void whiteBarEntryWithSixAndTwoKeepsSecondCheckerWhenSixIsBlockedTest() {
        Random firstRoll = new Random() {
            private final int[] rolls = {5, 1};
            private int nextRoll;

            @Override
            public int nextInt(int bound) {
                return rolls[nextRoll++];
            }
        };
        Dice dice = new Dice(6, 2, firstRoll);
        Round round = new Round(whitePlayer, blackPlayer, dice);
        forceTurn(round, Player.Color.WHITE);

        clearBoard(round);
        placeCheckerOnBar(round, Player.Color.WHITE);
        placeCheckerOnBar(round, Player.Color.WHITE);
        placeChecker(round, Player.Color.BLACK, 18);
        placeChecker(round, Player.Color.BLACK, 18);
        round.startTurn();

        List<Play> initialPlays = round.getRequiredPlays(round.getMoves());
        assertEquals(1, initialPlays.size());
        assertEquals(2, initialPlays.get(0).getDieUsed());

        round.executePlay(initialPlays.get(0));
        List<Play> remainingPlays = round.getRequiredPlays(round.getMoves());

        assertEquals(1, round.getCurrentBoard().getBar().getCheckers(Player.Color.WHITE));
        assertEquals(0, round.getCurrentBoard().getBar().getCheckers(Player.Color.BLACK));
        assertEquals(1, round.getCurrentBoard().getPoints().get(22).getCheckers(Player.Color.WHITE));
        assertTrue(remainingPlays.isEmpty(), "the remaining six is blocked at point 19");
    }

    @Test
    void possiblePlaysOnlyOffersBarEntriesWhileACheckerIsOnTheBarTest() {
        Round round = new Round(whitePlayer, blackPlayer);
        forceTurn(round, Player.Color.WHITE);
        clearBoard(round);
        placeCheckerOnBar(round, Player.Color.WHITE);
        // a second, free white checker elsewhere - should NOT be movable
        // while the bar checker hasn't re-entered
        placeChecker(round, Player.Color.WHITE, 10);

        List<Play> plays = round.possiblePlays(new int[]{3, 5});

        assertFalse(plays.isEmpty());
        for (Play play : plays) {
            assertEquals(Play.Type.OFF_BAR, play.getType());
        }
    }

    @Test
    void possiblePlaysForBlackOnlyOffersBarEntriesWhileACheckerIsOnTheBarTest() {
        Round round = new Round(whitePlayer, blackPlayer);
        forceTurn(round, Player.Color.BLACK);
        clearBoard(round);
        placeCheckerOnBar(round, Player.Color.BLACK);
        placeChecker(round, Player.Color.BLACK, 10);

        List<Play> plays = round.possiblePlays(new int[]{3, 5});

        assertFalse(plays.isEmpty());
        for (Play play : plays) {
            assertEquals(Play.Type.OFF_BAR, play.getType());
            assertEquals(blackPlayer, play.getPlayer());
        }
    }

    @Test
    void noLegalPlaysWhenOnTheBarAndOpponentsHomeBoardIsFullyClosedTest() {
        Round round = new Round(whitePlayer, blackPlayer);
        forceTurn(round, Player.Color.WHITE);
        clearBoard(round);
        placeCheckerOnBar(round, Player.Color.WHITE);

        // close every point in black's home board (indices 18-23) with 2+
        // black checkers, so no die value gives white a legal entry
        for (int i = 18; i <= 23; i++) {
            placeChecker(round, Player.Color.BLACK, i);
            placeChecker(round, Player.Color.BLACK, i);
        }

        assertTrue(round.possiblePlays(new int[]{3, 5}).isEmpty());
        assertTrue(round.getRequiredPlays(new int[]{3, 5}).isEmpty());
    }

    // ---- mandatory dice usage -------------------------------------------------

    @Test
    void mustPlayTheHigherDieWhenOnlyOneOfTwoDiceCanBePlayedTest() {
        Round round = new Round(whitePlayer, blackPlayer);
        forceTurn(round, Player.Color.WHITE);

        // a single white checker; wherever it lands with either die, the
        // *other* die's follow-up move is blocked by the same black point,
        // so at most one die can ever be used this turn - but both 6 and 3
        // are individually playable as a first move.
        clearBoard(round);
        placeChecker(round, Player.Color.WHITE, 14);
        placeChecker(round, Player.Color.BLACK, 5);
        placeChecker(round, Player.Color.BLACK, 5);

        List<Play> required = round.getRequiredPlays(new int[]{6, 3});

        assertEquals(1, required.size());
        assertEquals(6, required.get(0).getDieUsed());
        assertEquals(8, required.get(0).getForwardPoint());
    }

    @Test
    void mustPlayInAWayThatUsesBothDiceWhenThatIsPossibleTest() {
        Round round = new Round(whitePlayer, blackPlayer);
        forceTurn(round, Player.Color.WHITE);

        // a single white checker at point 6 (index 5): playing the 6
        // directly bears it off and wastes the 3 (total = 1 move), but
        // playing the 3 first (to point 3) leaves a 6 that can still bear
        // it off afterwards (total = 2 moves) - so the 3 is the only
        // required first play, even though bearing off with the 6 looks
        // like a perfectly legal move on its own.
        clearBoard(round);
        placeChecker(round, Player.Color.WHITE, 5);

        List<Play> required = round.getRequiredPlays(new int[]{6, 3});

        assertEquals(1, required.size());
        assertEquals(3, required.get(0).getDieUsed());
        assertEquals(Play.Type.NORMAL, required.get(0).getType());
        assertEquals(2, required.get(0).getForwardPoint());
    }

    // ---- bearing off with an oversized die -----------------------------------

    @Test
    void bearingOffWithAnOversizedDieIsBlockedWhileACheckerIsFurtherFromHomeTest() {
        Round round = new Round(whitePlayer, blackPlayer);
        forceTurn(round, Player.Color.WHITE);
        clearBoard(round);
        placeChecker(round, Player.Color.WHITE, 2);
        placeChecker(round, Player.Color.WHITE, 5); // further from off

        List<Play> plays = round.moveBearOff(6);

        assertEquals(1, plays.size());
        assertEquals(5, plays.get(0).getInitialPoint(), "only the checker further back may use the oversized die");
    }

    @Test
    void bearingOffWithAnOversizedDieIsAllowedOnceNoCheckerIsFurtherBackTest() {
        Round round = new Round(whitePlayer, blackPlayer);
        forceTurn(round, Player.Color.WHITE);
        clearBoard(round);
        placeChecker(round, Player.Color.WHITE, 2);

        List<Play> plays = round.moveBearOff(6);

        assertEquals(1, plays.size());
        assertEquals(2, plays.get(0).getInitialPoint());
    }

    @Test
    void bearingOffBlackWithAnExactDieIsAllowedInTheBlackHomeBoardTest() {
        Round round = new Round(whitePlayer, blackPlayer);
        forceTurn(round, Player.Color.BLACK);
        clearBoard(round);
        placeChecker(round, Player.Color.BLACK, 23);

        List<Play> plays = round.moveBearOff(1);

        assertEquals(1, plays.size());
        assertEquals(23, plays.get(0).getInitialPoint());
    }

    @Test
    void bearingOffBlackWithAnOversizedDieIsBlockedWhileACheckerIsFurtherFromHomeTest() {
        Round round = new Round(whitePlayer, blackPlayer);
        forceTurn(round, Player.Color.BLACK);
        clearBoard(round);
        placeChecker(round, Player.Color.BLACK, 21);
        placeChecker(round, Player.Color.BLACK, 20);

        List<Play> plays = round.moveBearOff(6);

        assertEquals(1, plays.size());
        assertEquals(20, plays.get(0).getInitialPoint());
    }

    @Test
    void bearingOffBlackWithAnOversizedDieIsAllowedOnceNoCheckerIsFurtherBackTest() {
        Round round = new Round(whitePlayer, blackPlayer);
        forceTurn(round, Player.Color.BLACK);
        clearBoard(round);
        placeChecker(round, Player.Color.BLACK, 21);

        List<Play> plays = round.moveBearOff(6);

        assertEquals(1, plays.size());
        assertEquals(21, plays.get(0).getInitialPoint());
    }

    // ---- move direction depends on color --------------------------------------

    @Test
    void moveBoardMovesWhiteTowardLowerIndicesTest() {
        Round round = new Round(whitePlayer, blackPlayer);
        forceTurn(round, Player.Color.WHITE);
        clearBoard(round);
        placeChecker(round, Player.Color.WHITE, 10);

        List<Play> plays = round.moveBoard(4);

        assertEquals(1, plays.size());
        assertEquals(6, plays.get(0).getForwardPoint());
    }

    @Test
    void moveBoardMovesBlackTowardHigherIndicesTest() {
        Round round = new Round(whitePlayer, blackPlayer);
        forceTurn(round, Player.Color.BLACK);
        clearBoard(round);
        placeChecker(round, Player.Color.BLACK, 2);

        List<Play> plays = round.moveBoard(3);

        assertEquals(1, plays.size());
        assertEquals(5, plays.get(0).getForwardPoint());
    }

    @Test
    void moveBoardDoesNotOfferBlackMoveOntoBlockedPointTest() {
        Round round = new Round(whitePlayer, blackPlayer);
        forceTurn(round, Player.Color.BLACK);
        clearBoard(round);
        placeChecker(round, Player.Color.BLACK, 2);
        placeChecker(round, Player.Color.WHITE, 5);
        placeChecker(round, Player.Color.WHITE, 5);

        List<Play> plays = round.moveBoard(3);

        assertTrue(plays.isEmpty());
    }
}
