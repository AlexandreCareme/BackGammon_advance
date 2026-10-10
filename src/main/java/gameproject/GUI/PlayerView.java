package gameproject.GUI;

import gameproject.player.Player;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * Displays the names of the two players and highlights the player
 * whose turn it currently is
 */
public class PlayerView {

    private final Label whitePlayer;
    private final Label blackPlayer;

    /**
     * Creates the player info view
     * @param whiteName name of the white player
     * @param blackName name of the black player
     */
    public PlayerView(String whiteName, String blackName) {
        whitePlayer = new Label(whiteName);
        blackPlayer = new Label(blackName);

        whitePlayer.setMinWidth(180);
        blackPlayer.setMinWidth(180);

        whitePlayer.setAlignment(javafx.geometry.Pos.CENTER);
        blackPlayer.setAlignment(javafx.geometry.Pos.CENTER);

        setNormalStyle();
    }

    /**
     * Retruns the layout containing both player names
     * @return layout containing the player names
     */
    public VBox getLayout() {
        VBox layout = new VBox(150);

        layout.getChildren().addAll(
                whitePlayer,
                blackPlayer
        );

        layout.setAlignment(javafx.geometry.Pos.CENTER);
        layout.setPrefHeight(500);
        layout.setPrefWidth(150);

        return layout;
    }

    /**
     * Highlights the player whose turn it currently is
     * @param currentPlayer player whose turn it is
     */
    public void updateHighlight(Player currentPlayer) {
        if (currentPlayer == null) {
            return;
        }

        if (currentPlayer.getColor() == Player.Color.WHITE) {
            setPlayerHighlight(whitePlayer);
        } else {
            setPlayerHighlight(blackPlayer);
        }
    }

    /**
     * Sets both player labels to their normal styles
     */
    private void setNormalStyle() {
        whitePlayer.setStyle(
                "-fx-font-size: 20px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: black;" +
                        "-fx-background-color: #F5E6C8;" +
                        "-fx-padding: 12px 20px;" +
                        "-fx-background-radius: 8px;"
        );

        blackPlayer.setStyle(
                "-fx-font-size: 20px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: white;" +
                        "-fx-background-color: #2B2B2B;" +
                        "-fx-padding: 12px 20px;" +
                        "-fx-background-radius: 8px;"
        );
    }

    /**
     * Highlights the current player's label
     * -JAVADOC- @param currentPlayer missing
     */
    private void setPlayerHighlight(Label currentPlayer) {
        setNormalStyle();

        currentPlayer.setStyle(
                "-fx-font-size: 20px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: black;" +
                        "-fx-background-color: #FFD700;" +
                        "-fx-padding: 12px 20px;" +
                        "-fx-background-radius: 8px;"
        );
    }
}