package gameproject;

// AI-assisted (Claude), reviewed and adapted by Kilian.
// AI-assisted (GitHub Copilot), reviewed and adapted by Kilian.
// Per AGENTS.md: test code only (src/test), disclosed here and in the
// commit that adds it (add an `AI-Assisted-By: Claude` trailer, and fill
// in the PR's AI-use disclosure checklist).
//
// Scope: exercises Board's game-mechanics methods — setup, normal moves
// (including hitting a blot and being blocked), bearing off, and
// re-entering from the bar.

import gameproject.game.elements.Board;
import gameproject.game.elements.Play;
import gameproject.game.elements.Point;
import gameproject.game.elements.Round;
import gameproject.player.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class BoardTest {

    private Player whitePlayer;
    private Player blackPlayer;

    @BeforeEach
    void setUp() {
        // Strategy isn't exercised by any Board method, so null is fine here.
        whitePlayer = new Player("White", Player.Color.WHITE, null);
        blackPlayer = new Player("Black", Player.Color.BLACK, null);
    }

    private Board boardWithAllCheckersOffBoard() {
        Board board = new Board();
        moveAllCheckersToOffBoard(board);
        assertCheckerTotals(board);
        return board;
    }

    private void moveAllCheckersToOffBoard(Board board) {
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

    private void placeChecker(Board board, Player.Color color, int pointIndex) {
        board.getOffBoard().removeChecker(color);
        board.getPoints().get(pointIndex).addChecker(color);
    }

    private void placeCheckerOnBar(Board board, Player.Color color) {
        board.getOffBoard().removeChecker(color);
        board.getBar().addChecker(color);
    }

    private void assertCheckerTotals(Board board) {
        for (Player.Color color : Player.Color.values()) {
            int total = board.getBar().getCheckers(color)
                    + board.getOffBoard().getCheckers(color);
            for (Point point : board.getPoints()) {
                total += point.getCheckers(color);
            }
            assertEquals(15, total, color + " must always have 15 checkers");
        }
    }

    private void assertStartingPosition(Board board) {
        assertEquals(2, board.getPoints().get(23).getCheckers(Player.Color.WHITE), "point 24");
        assertEquals(5, board.getPoints().get(12).getCheckers(Player.Color.WHITE), "point 13");
        assertEquals(3, board.getPoints().get(7).getCheckers(Player.Color.WHITE), "point 8");
        assertEquals(5, board.getPoints().get(5).getCheckers(Player.Color.WHITE), "point 6");
        assertEquals(2, board.getPoints().get(0).getCheckers(Player.Color.BLACK), "point 1");
        assertEquals(5, board.getPoints().get(11).getCheckers(Player.Color.BLACK), "point 12");
        assertEquals(3, board.getPoints().get(16).getCheckers(Player.Color.BLACK), "point 17");
        assertEquals(5, board.getPoints().get(18).getCheckers(Player.Color.BLACK), "point 19");
        assertEquals(0, board.getBar().getCheckers(Player.Color.WHITE));
        assertEquals(0, board.getBar().getCheckers(Player.Color.BLACK));
        assertEquals(0, board.getOffBoard().getCheckers(Player.Color.WHITE));
        assertEquals(0, board.getOffBoard().getCheckers(Player.Color.BLACK));
        assertCheckerTotals(board);
    }

    /**
     * Checks whether new toString method of board produces same String as printBoard Method
     */
    @Test
    void toStringTest(){
        String Text = "[5,0] [0,0] [0,0] [0,0] [0,3] [0,0] || [0,5] [0,0] [0,0] [0,0] [0,0] [2,0] | Black \n[0,5] [0,0] [0,0] [0,0] [3,0] [0,0] || [5,0] [0,0] [0,0] [0,0] [0,0] [0,2] | White \nBar: [0,0]   OffBoard: [0,0] [White,Black]";

        Board board = new Board();

        assertEquals(Text,board.toString());

    }

    /**
     * Tests printing to console
     * @param
     */
//    static void main(String[] args) {
//        Board board = new Board();
//        board.setupBoard();
//        System.out.println(board);
//
//    }

    // ---- setup / initial state -------------------------------------------------

    @Test
    void newBoardHasStandardStartingPositionTest() {
        Board board = new Board();

        assertEquals(24, board.getPoints().size());
        assertStartingPosition(board);
    }

    @Test
    void constructorPlacesStandardStartingPositionTest() {
        Board board = new Board();

        assertStartingPosition(board);
    }

    // ---- normal moves ------------------------------------------------------


    @Test
    void makeNormalPlayMovesCheckerBetweenPointsTest() {
        Board board = boardWithAllCheckersOffBoard();
        placeChecker(board, Player.Color.WHITE, 10);

        Play play = new Play(10, 7, Play.Type.NORMAL, whitePlayer, 3);
        board.makePlay(play);

        assertEquals(0, board.getPoints().get(10).getCheckers(Player.Color.WHITE));
        assertEquals(1, board.getPoints().get(7).getCheckers(Player.Color.WHITE));
        assertCheckerTotals(board);
    }

    @Test
    void makeNormalPlayMovesBlackCheckerBetweenPointsTest() {
        Board board = boardWithAllCheckersOffBoard();
        placeChecker(board, Player.Color.BLACK, 2);

        Play play = new Play(2, 5, Play.Type.NORMAL, blackPlayer, 3);
        board.makePlay(play);

        assertEquals(0, board.getPoints().get(2).getCheckers(Player.Color.BLACK));
        assertEquals(1, board.getPoints().get(5).getCheckers(Player.Color.BLACK));
        assertCheckerTotals(board);
    }

    @Test
    void makeNormalPlayHitsLoneOpponentCheckerTest() {
        Board board = boardWithAllCheckersOffBoard();
        placeChecker(board, Player.Color.WHITE, 10);
        placeChecker(board, Player.Color.BLACK, 7); // a single black checker = a blot

        Play play = new Play(10, 7, Play.Type.NORMAL, whitePlayer, 3);
        board.makePlay(play);

        assertEquals(1, board.getPoints().get(7).getCheckers(Player.Color.WHITE));
        assertEquals(0, board.getPoints().get(7).getCheckers(Player.Color.BLACK));
        assertEquals(1, board.getBar().getCheckers(Player.Color.BLACK), "the hit black checker should go to the bar");
        assertCheckerTotals(board);
    }

    @Test
    void makeNormalPlayHitsLoneWhiteCheckerWithBlackPlayTest() {
        Board board = boardWithAllCheckersOffBoard();
        placeChecker(board, Player.Color.BLACK, 2);
        placeChecker(board, Player.Color.WHITE, 5);

        Play play = new Play(2, 5, Play.Type.NORMAL, blackPlayer, 3);
        board.makePlay(play);

        assertEquals(1, board.getPoints().get(5).getCheckers(Player.Color.BLACK));
        assertEquals(0, board.getPoints().get(5).getCheckers(Player.Color.WHITE));
        assertEquals(1, board.getBar().getCheckers(Player.Color.WHITE));
        assertCheckerTotals(board);
    }

    @Test
    void makeNormalPlayWithEmptySourceThrowsWithoutMutatingBoardTest() {
        Board board = boardWithAllCheckersOffBoard();
        placeChecker(board, Player.Color.BLACK, 7);

        Play play = new Play(10, 7, Play.Type.NORMAL, whitePlayer, 3);

        assertThrows(IllegalStateException.class, () -> board.makePlay(play));
        assertEquals(0, board.getBar().getCheckers(Player.Color.BLACK));
        assertEquals(0, board.getPoints().get(7).getCheckers(Player.Color.WHITE));
        assertEquals(1, board.getPoints().get(7).getCheckers(Player.Color.BLACK));
        assertCheckerTotals(board);
    }

    @Test
    void makeNormalPlayOntoPointBlockedByTwoOpponentCheckersThrowsTest() {
        Board board = boardWithAllCheckersOffBoard();
        placeChecker(board, Player.Color.WHITE, 10);
        placeChecker(board, Player.Color.BLACK, 7);
        placeChecker(board, Player.Color.BLACK, 7); // 2 black checkers = blocked for white

        Play play = new Play(10, 7, Play.Type.NORMAL, whitePlayer, 3);

        assertThrows(IllegalStateException.class, () -> board.makePlay(play));
        assertCheckerTotals(board);
    }

    @Test
    void makePlayDispatchesNormalPlayCorrectlyTest() {
        Board board = boardWithAllCheckersOffBoard();
        placeChecker(board, Player.Color.WHITE, 10);

        Play play = new Play(10, 7, Play.Type.NORMAL, whitePlayer, 3);
        board.makePlay(play);

        assertEquals(0, board.getPoints().get(10).getCheckers(Player.Color.WHITE));
        assertEquals(1, board.getPoints().get(7).getCheckers(Player.Color.WHITE));
        assertCheckerTotals(board);
    }

    @Test
    void makePlayDispatchesBearOffPlayCorrectlyTest() {
        Board board = boardWithAllCheckersOffBoard();
        placeChecker(board, Player.Color.BLACK, 2);

        Play play = new Play(2, Play.Type.BEAR_OFF, blackPlayer, 3);
        board.makePlay(play);

        assertEquals(0, board.getPoints().get(2).getCheckers(Player.Color.BLACK));
        assertEquals(15, board.getOffBoard().getCheckers(Player.Color.BLACK));
        assertCheckerTotals(board);
    }

    @Test
    void makePlayDispatchesOffBarPlayCorrectlyTest() {
        Board board = boardWithAllCheckersOffBoard();
        placeCheckerOnBar(board, Player.Color.WHITE);

        Play play = new Play(5, Play.Type.OFF_BAR, whitePlayer, 3);
        board.makePlay(play);

        assertEquals(0, board.getBar().getCheckers(Player.Color.WHITE));
        assertEquals(1, board.getPoints().get(5).getCheckers(Player.Color.WHITE));
        assertCheckerTotals(board);
    }

    // ---- bearing off ------------------------------------------------------

    @Test
    void makeBearOffPlayMovesCheckerFromPointToOffBoardTest() {
        Board board = boardWithAllCheckersOffBoard();
        placeChecker(board, Player.Color.WHITE, 2);

        Play play = new Play(2, Play.Type.BEAR_OFF, whitePlayer, 3);
        board.makePlay(play);

        assertEquals(0, board.getPoints().get(2).getCheckers(Player.Color.WHITE));
        assertEquals(15, board.getOffBoard().getCheckers(Player.Color.WHITE));
        assertCheckerTotals(board);
    }

    @Test
    void makeBearOffPlayMovesBlackCheckerToOffBoardTest() {
        Board board = boardWithAllCheckersOffBoard();
        placeChecker(board, Player.Color.BLACK, 23);

        Play play = new Play(23, Play.Type.BEAR_OFF, blackPlayer, 1);
        board.makePlay(play);

        assertEquals(0, board.getPoints().get(23).getCheckers(Player.Color.BLACK));
        assertEquals(15, board.getOffBoard().getCheckers(Player.Color.BLACK));
        assertCheckerTotals(board);
    }

    @Test
    void makeBearOffPlayFromEmptyPointThrowsTest() {
        Board board = boardWithAllCheckersOffBoard();

        Play play = new Play(2, Play.Type.BEAR_OFF, whitePlayer, 3);

        assertThrows(IllegalStateException.class, () -> board.makePlay(play));
        assertCheckerTotals(board);
    }

    // ---- entering from the bar ----------------------------------------------
    //
    @Test
    void makeOffBarPlayEntersWhiteCheckerOntoForwardPointTest() {
        Board board = boardWithAllCheckersOffBoard();
        placeCheckerOnBar(board, Player.Color.WHITE);

        Play play = new Play(5, Play.Type.OFF_BAR, whitePlayer, 3);
        board.makePlay(play);

        assertEquals(0, board.getBar().getCheckers(Player.Color.WHITE));
        assertEquals(1, board.getPoints().get(5).getCheckers(Player.Color.WHITE));
        assertCheckerTotals(board);
    }

    @Test
    void makeOffBarPlayEntersBlackCheckerOntoForwardPointTest() {
        Board board = boardWithAllCheckersOffBoard();
        placeCheckerOnBar(board, Player.Color.BLACK);

        Play play = new Play(18, Play.Type.OFF_BAR, blackPlayer, 3);
        board.makePlay(play);

        assertEquals(0, board.getBar().getCheckers(Player.Color.BLACK));
        assertEquals(1, board.getPoints().get(18).getCheckers(Player.Color.BLACK));
        assertCheckerTotals(board);
    }

    @Test
    void makeOffBarPlayWhiteHitsLoneBlackCheckerTest() {
        Board board = boardWithAllCheckersOffBoard();
        placeCheckerOnBar(board, Player.Color.WHITE);
        placeChecker(board, Player.Color.BLACK, 5);

        Play play = new Play(5, Play.Type.OFF_BAR, whitePlayer, 3);
        board.makePlay(play);

        assertEquals(1, board.getPoints().get(5).getCheckers(Player.Color.WHITE));
        assertEquals(0, board.getPoints().get(5).getCheckers(Player.Color.BLACK));
        assertEquals(1, board.getBar().getCheckers(Player.Color.BLACK));
        assertCheckerTotals(board);
    }

    @Test
    void makeOffBarPlayBlackHitsLoneWhiteCheckerTest() {
        Board board = boardWithAllCheckersOffBoard();
        placeCheckerOnBar(board, Player.Color.BLACK);
        placeChecker(board, Player.Color.WHITE, 18);

        Play play = new Play(18, Play.Type.OFF_BAR, blackPlayer, 3);
        board.makePlay(play);

        assertEquals(1, board.getPoints().get(18).getCheckers(Player.Color.BLACK));
        assertEquals(0, board.getPoints().get(18).getCheckers(Player.Color.WHITE));
        assertEquals(1, board.getBar().getCheckers(Player.Color.WHITE));
        assertCheckerTotals(board);
    }

    @Test
    void makeOffBarPlayOntoPointBlockedByTwoOpponentCheckersThrowsTest() {
        Board board = boardWithAllCheckersOffBoard();
        placeCheckerOnBar(board, Player.Color.WHITE);
        placeChecker(board, Player.Color.BLACK, 5);
        placeChecker(board, Player.Color.BLACK, 5);

        Play play = new Play(5, Play.Type.OFF_BAR, whitePlayer, 3);

        assertThrows(IllegalStateException.class, () -> board.makePlay(play));
        assertCheckerTotals(board);
    }

    // ---- copy constructor / independence -------------------------------------

    @Test
    void boardCopyConstructorAppliesPlayWithoutMutatingOriginalBoardTest() {
        Round round = new Round(whitePlayer, blackPlayer);
        moveAllCheckersToOffBoard(round.getCurrentBoard());
        placeChecker(round.getCurrentBoard(), Player.Color.WHITE, 10);

        Play play = new Play(10, 7, Play.Type.NORMAL, whitePlayer, 3);
        Board newBoard = new Board(round, play);

        // The board stored in `round` must be untouched...
        assertEquals(1, round.getCurrentBoard().getPoints().get(10).getCheckers(Player.Color.WHITE));
        assertEquals(0, round.getCurrentBoard().getPoints().get(7).getCheckers(Player.Color.WHITE));

        // ...while the new snapshot reflects the play.
        assertEquals(0, newBoard.getPoints().get(10).getCheckers(Player.Color.WHITE));
        assertEquals(1, newBoard.getPoints().get(7).getCheckers(Player.Color.WHITE));
        assertCheckerTotals(round.getCurrentBoard());
        assertCheckerTotals(newBoard);
    }
}
