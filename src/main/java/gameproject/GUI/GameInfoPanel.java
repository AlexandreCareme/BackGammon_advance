package gameproject.GUI;

import javafx.geometry.Insets;
import javafx.scene.layout.VBox;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;


/**
 * Displays information about the current Backgammon game
 * The panel shows the current player, dice, remaining moves,
 * doubling factor and game log
 */
public class GameInfoPanel {
    private VBox layout;
    private TextArea gamelog;

    private Label turnLabel;
    private Label diceLabel;
    private Label movesLabel;
    private Label doublingFactorLabel;
    private Label title;
    private Label logTitle;
    private Label fogTitle;
    private Label fogMessage;

    /**
     * Creates the game information panel and all its components
     */
    public GameInfoPanel(){

        //Main title
        title = new Label("Game Info");
        title.setStyle(UIStyle.gameInfoTitleStyle());

        // Turn information
        Label turnTitle = new Label("Turn");
        turnTitle.setStyle(UIStyle.gameInfoSectionStyle());

        turnLabel = new Label("-");
        turnLabel.setStyle(UIStyle.gameInfoValueStyle());

        //Dice information
        Label diceTitle = new Label("Dice");
        diceTitle.setStyle(UIStyle.gameInfoSectionStyle());

        diceLabel = new Label("- - ");
        diceLabel.setStyle(UIStyle.gameInfoValueStyle());


        //Moves remaining information
        Label movesTitle = new Label("Moves Remaining");
        movesTitle.setStyle(UIStyle.gameInfoSectionStyle());

        movesLabel = new Label("-");
        movesLabel.setStyle(UIStyle.gameInfoValueStyle());


        // Doubling factor information
        Label doublingFactorTitle = new Label("Doubling Factor");
        doublingFactorTitle.setStyle(UIStyle.gameInfoSectionStyle());

        doublingFactorLabel = new Label("1");
        doublingFactorLabel.setStyle(UIStyle.gameInfoValueStyle());

        //Fog of War information shown instead of the game log
        fogTitle = new Label("FOG OF WAR");
        fogTitle.setStyle("-fx-font-size: 16px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #B8B8B8;");
        fogMessage = new Label("Your opponent know where their checkers are.\n" +
                "You don't.\n\n" +
                "Opponent checkers are revealed\n" +
                "when they can be hit.\n\n" +
                "Can you still outsmart them?\nGood luck!");
        fogMessage.setStyle("-fx-font-size: 14px;" +
                "-fx-text-fill: white;");
        fogMessage.setWrapText(true);




        // Game log
        logTitle = new Label("GAME LOG");
        logTitle.setStyle(UIStyle.gameInfoSectionStyle());

        gamelog = new TextArea();
        gamelog.setEditable(false);
        gamelog.setWrapText(true);

        gamelog.setPromptText("Game information will appear here...");

        gamelog.setStyle(UIStyle.gameInfoLogStyle());

        gamelog.setPrefWidth(250);
        gamelog.setPrefHeight(230);

        // Main dark grey panel

        // Main wooden panel
        layout = new VBox(10);
        layout.setPadding(new Insets(15));
        layout.setStyle(UIStyle.gameInfoPanelStyle());

        layout.getChildren().addAll(title, turnTitle, turnLabel, diceTitle, diceLabel, movesTitle, movesLabel, doublingFactorTitle, doublingFactorLabel, fogTitle, fogMessage, logTitle, gamelog);
        layout.setAlignment(Pos.TOP_LEFT);
        layout.setPrefWidth(290);
    }

    /**
     * Returns the layout of the game information panel
     * @return panel layout
     */
    public VBox getLayout(){
        return layout;
    }

    /**
     * Adds a message to the gamelog
     * @param message message to display
     */
    public void addMessage(String message){
        gamelog.appendText(message + "\n");
    }

    /**
     * Updates the player shown in the Turn field
     * @param playerName name of the current player
     */
    public void setTurn(String playerName){
        turnLabel.setText(playerName);
    }

    /**
     * updates the dice shown in the Dice field.
     * When doubles are rolled, the dice are shown four time because doubles
     * give the player four moves
     * @param dice1 value of the first dice
     * @param dice2 value of the second die
     */
    public void setDice(int dice1, int dice2) {
        if (dice1 == dice2) {
            diceLabel.setText(
                    getDiceSymbol(dice1) + " " +
                            getDiceSymbol(dice2) + " " +
                            getDiceSymbol(dice1) + " " +
                            getDiceSymbol(dice2));
        } else {
            diceLabel.setText(
                    getDiceSymbol(dice1) + " " +
                            getDiceSymbol(dice2));
        }

        diceLabel.setStyle(UIStyle.gameInfoDiceStyle());
    }


    /**
     * Converts a numerical dice value into its dice symbol
     * @param value dice value from 1-6
     * @return corresponding dice symbol
     */
    private String getDiceSymbol(int value) {

        switch (value) {
            case 1:
                return "⚀";
            case 2:
                return "⚁";
            case 3:
                return "⚂";
            case 4:
                return "⚃";
            case 5:
                return "⚄";
            case 6:
                return "⚅";
            default:
                return "?";
        }
    }

    /**
     * Updates the number of moves remaining
     * @param moves number of moves remaining
     */
    public void setMovesRemaining(int moves){
        movesLabel.setText(String.valueOf(moves));
    }

    /**
     * Displays a text message in the moves remaining field
     * @param message message to display
     */
    public void setMovesMessage(String message) {
        movesLabel.setText(message);
    }

    /**
     * Resets only the dice display after a turn has ended.
     * This is different from reset() which resets the entire information panel
     */
    public void resetInfoDice(){
        diceLabel.setText("roll dice");
        diceLabel.setStyle( "-fx-font-size: 13px;" +
                "-fx-text-fill: white;");
    }

    /**
     * updates the doubling factor shown in the panel
     * @param factor current doubling factor
     */
    public void setDoublingFactor(int factor){
        doublingFactorLabel.setText(String.valueOf(factor));
    }

    /**
     * Resets the entire information panel for a new game.
     * This resets the turn, dice, remaining moves, doubling
     * factor and clears the game log
     */
    public void reset(){
        turnLabel.setText("-");
        diceLabel.setText("-  -");
        movesLabel.setText("-");
        doublingFactorLabel.setText("1");
        gamelog.clear();
    }

    /**
     * Changes the information displayed in the panel depending on the selected game mode.
     * During Fog of War, the game log is hidden because it could reveal information about
     * the opponent's moves. Instead, an explanation of the game is displayed.
     * @param fogOfWar true when Fog of War mode is active
     */
    public void setFogOfWar(boolean fogOfWar){
        if(fogOfWar){
            title.setText("FOG OF WAR");
            title.setStyle("-fx-font-size: 22px;" +
                    "-fx-font-weight: bold;" +
                    "-fx-text-fill: #B8B8B8;");

            layout.setStyle("-fx-background-color: #202020;" +
                    "-fx-border-color: #555555;" +
                    "-fx-border-width: 2px;");

            fogTitle.setVisible(true);
            fogTitle.setManaged(true);
            fogMessage.setVisible(true);
            fogMessage.setManaged(true);

            logTitle.setVisible(false);
            logTitle.setManaged(false);

            gamelog.setVisible(false);
            gamelog.setManaged(false);
        }else{
            title.setText("Game Info");
            title.setStyle(UIStyle.gameInfoTitleStyle());

            layout.setStyle(UIStyle.gameInfoPanelStyle());


            fogTitle.setVisible(false);
            fogTitle.setManaged(false);
            fogMessage.setVisible(false);
            fogMessage.setManaged(false);

            logTitle.setVisible(true);
            logTitle.setManaged(true);

            gamelog.setVisible(true);
            gamelog.setManaged(true);
        }
    }
}
