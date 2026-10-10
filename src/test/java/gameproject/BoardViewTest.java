package gameproject;

// AI-assisted (GitHub Copilot Chat), reviewed and adapted by Kilian.
// If committed, disclose this assistance with:
// AI-Assisted-By: GitHub Copilot Chat

import gameproject.GUI.BoardView;
import gameproject.game.elements.Dice;
import gameproject.game.elements.Round;
import gameproject.player.Player;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * UI regression tests for BoardView.
 *
 * These tests intentionally verify observable JavaFX behavior rather than
 * inspecting BoardView's private selection fields.
 */
class BoardViewTest extends ApplicationTest {

    private Round round;
    private BoardView boardView;
    private Stage stage;

    @Override
    public void start(Stage stage) {
        this.stage = stage;

        Player white = new Player("White", Player.Color.WHITE, null);
        Player black = new Player("Black", Player.Color.BLACK, null);

        Random openingRoll = new Random() {
            @Override
            public int nextInt(int bound) {
                return bound == 6 ? (nextValue++ == 0 ? 0 : 1) : super.nextInt(bound);
            }

            private int nextValue;
        };
        Dice dice = new Dice(1, 2, openingRoll);
        round = new Round(white, black, dice);
        round.determineFirstPlayer();

        if (round.getCurrentPlayer() == black) {
            round.switchPlayer();
        }
        round.startTurn();

        Pane offBoard = new Pane();
        offBoard.setPrefSize(80, 550);

        boardView = new BoardView(round, offBoard);
        boardView.setPrefSize(800, 550);

        stage.setScene(new Scene(boardView, 800, 550));
        stage.show();
        boardView.refreshBoard();
    }

    @AfterEach
    void closeStage() {
        if (stage != null) {
            interact(() -> stage.hide());
        }
    }

    @Test
    void selectingWhiteCheckerHighlightsItsLegalDestinations() {
        Circle checker = findWhiteCheckerOnPoint(5);
        assertNotNull(checker, "The initial white checker on point 6 should be rendered");

        clickOn(checker);

        Polygon destination = (Polygon) boardView.getChildren().get(4);
        assertEquals(Color.YELLOW, destination.getStroke(),
                "Selecting point 6 with a 1 die should highlight point 5");
    }

    private Circle findWhiteCheckerOnPoint(int pointIndex) {
        double boardWidth = 800;
        double margin = 10;
        double barWidth = boardWidth * 0.06;
        double sideWidth = (boardWidth - 2 * margin - barWidth) / 2;
        double pointWidth = sideWidth / 6;

        int col = pointIndex < 12 ? pointIndex : 23 - pointIndex;
        double centerX = margin + col * pointWidth + pointWidth / 2;
        if (pointIndex >= 6 && pointIndex < 12) {
            centerX += barWidth;
        }
        if (pointIndex >= 12 && pointIndex < 18) {
            centerX += barWidth;
        }

        for (var node : boardView.getChildren()) {
            if (node instanceof Circle circle
                    && Color.WHITE.equals(circle.getFill())
                    && Math.abs(circle.getCenterX() - centerX) < 0.1) {
                return circle;
            }
        }
        return null;
    }
}
