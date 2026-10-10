package gameproject.game.logic;

import gameproject.GUI.BoardView;
import gameproject.GUI.DiceView;
import gameproject.GUI.GameInfoPanel;
import gameproject.ai.AITurnManager;
import gameproject.game.elements.Play;
import gameproject.game.elements.Round;
import gameproject.game.elements.Dice;
import gameproject.player.Player;
import gameproject.ai.AIAgent;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.util.Duration;
import java.util.List;


/**
 * Controls the flow of the Backgammon game.
 * This class connects the game model with the JavaFX interface, manages turn, AI actions,
 * doubling, scoring and game transitions.
 */
public class GameController {
    private Round round;
    private BoardView boardView;
    private Button rollButton;
    private Button doubleButton;
    private Button startButton;
    private BorderPane layout;
    private DiceView diceView;
    private Label whiteScore;
    private Label blackScore;
    private GameInfoPanel gameInfoPanel;
    private int whiteScoreValue = 0;
    private int blackScoreValue = 0;
    private int pointsWon = 0;
    private boolean doubleDeclined = false;
    private Runnable updateHighlight;
    private Runnable showMainMenu;
    private Runnable showDoubleAlert;
    private AITurnManager aiTurnManager;
    private Timeline aiThinkingDelay;
    private Timeline aiMoveDelay;
    private Timeline nextGameDelay;
    private boolean gameActive = true;
    private boolean fogOfWar;

    /**
     * Creates the GameController and connects the game model with the JavaFX components used during the game
     *
     * @param round           the round of the current game
     * @param boardView       the visual representation of the board
     * @param rollButton      button used to roll the dice
     * @param doubleButton    button used to offer a double
     * @param startButton     button used to start the game
     * @param whiteScore      label displaying the white player's score
     * @param blackScore      label displaying the black player's score
     * @param layout          main layout of the game screen
     * @param diceView        visual representation of the dice
     * @param updateHighlight updates the highlighted current player
     */
    public GameController(Round round,
                          BoardView boardView,
                          Button rollButton,
                          Button doubleButton,
                          Button startButton,
                          Label whiteScore,
                          Label blackScore,
                          BorderPane layout,
                          DiceView diceView,
                          Runnable updateHighlight,
                          boolean fogOfWar) {
        this.round = round;
        this.boardView = boardView;
        this.rollButton = rollButton;
        this.doubleButton = doubleButton;
        this.startButton = startButton;
        this.layout = layout;
        this.diceView = diceView;
        this.blackScore = blackScore;
        this.whiteScore = whiteScore;
        this.updateHighlight = updateHighlight;
        this.aiTurnManager = new AITurnManager(round);
        this.fogOfWar = fogOfWar;
    }

    /**
     * Starts a new turn and updated the game info displayed to the player.
     *
     * @param afterTurnReady action to perform once the turn is ready
     */
 void startTurn(Runnable afterTurnReady) {
        round.startTurn();
        startTurnDisplay(afterTurnReady);
    }

    /**
     * Displays the dice result and prepares the board for the turn. If no legal moves
     * are available, the turn is automatically ended after a short delay
     *
     * @param afterTurnReady action to perform once the turn is ready
     */
    private void startTurnDisplay(Runnable afterTurnReady) {
        Dice dice = round.getRoundDice();
        int diceValue1 = dice.getDice1();
        int diceValue2 = dice.getDice2();
        String playerName = round.getCurrentPlayer().getName();
        gameInfoPanel.setTurn(playerName);
        gameInfoPanel.setDice(diceValue1, diceValue2);
        gameInfoPanel.setDoublingFactor(round.getDoublingFactor());

        diceView.rollAnimation(diceValue1, diceValue2, () -> {
            if (dice.isDouble()) {
                gameInfoPanel.addMessage(playerName + " rolled double " + diceValue1);
            } else {
                if(round.getTurnNumber()!= 1){
                    gameInfoPanel.addMessage(playerName + " rolled " + diceValue1 + " and " + diceValue2);
                }

            }

            if (turnIsFinished()) {
                gameInfoPanel.setMovesRemaining(0);
                gameInfoPanel.addMessage("No legal moves available");

                Timeline delay = new Timeline(new KeyFrame(Duration.millis(2000), e -> {
                    finishTurn();
                }));

                delay.play();
                return;
            }
            gameInfoPanel.setMovesRemaining(round.getMoves().length);
            updateHighlight.run();

            if (afterTurnReady != null) {
                afterTurnReady.run();
            }
        });
    }

    /**
     * Checks whether the current player has finished their turn. A turn is finished
     * when there are no more moves left or when none of the remaining dice can be
     * used legally
     *
     * @return true if the turn is finished, otherwise false
     */
    private boolean turnIsFinished() {
        int[] moves = round.getMoves();
        if (moves == null) {
            return true;
        }
        if (moves.length == 0) {
            return true;
        }
        if (!round.hasLegalMoves()) {
            return true;
        }
        return false;
    }

    /**
     * It updated the dice and move information after the human made its move.
     * It checks whether the game has been won, and finished the turn when
     * necessary
     */

    /** -JAVADOC- */
    public void movePlayed() {
        updateDiceAppearance();

        int[] remainingMoves = round.getMoves();

        if (remainingMoves != null) {
            gameInfoPanel.setMovesRemaining(remainingMoves.length);
        }

        gameInfoPanel.setDoublingFactor(round.getDoublingFactor());
        if (round.checkWinConditions()) {
            handleGameWon();
            return;
        }
        if (turnIsFinished()) {

            if (remainingMoves != null
                    && remainingMoves.length > 0
                    && !round.hasLegalMoves()) {
                gameInfoPanel.addMessage("No more legal moves available");
            }


            finishTurn();
        }
    }

    /**
     * Ends the current's player turn and starts the next player's turn
     */

    /** -JAVADOC- */
    private void finishTurn() {
        Player endingPlayer = round.getCurrentPlayer();

        boardView.clearLogicalSelection();
        round.endTurn();
        updateHighlight.run();
        gameInfoPanel.resetInfoDice();

        Player startingPlayer = round.getCurrentPlayer();

        gameInfoPanel.addMessage("End of " + endingPlayer.getName());
        gameInfoPanel.addMessage(" ");
        gameInfoPanel.addMessage("Start of " + startingPlayer.getName());

        if (isAITurn()) {
            startAITurn();
        } else {
            gameInfoPanel.setMovesMessage("-");
            enablePlayerControls();
        }
    }

    /**
     * Starts the first turn of the game. The first player is determined by
     * rolling the dice before the first normal turn begins
     */
    public void startGame() {
        round.determineFirstPlayer();
        showGameStartMessages();

        if (isAITurn()) {
            startFirstTurn(() -> {
                startAIThinking();
            });
        } else {
            startFirstTurn(null);
        }
    }

    /**
     * Starts the first turn of the game
     *
     * @param afterTurnReady action to perform once the first turn is ready
     */
    private void startFirstTurn(Runnable afterTurnReady) {
        if (round.getTurnNumber() != 0) {
            throw new RuntimeException("This is not the first turn, the turn number is: " + round.getTurnNumber());
        }
        disablePlayerControls();
        startTurn(afterTurnReady);
    }

    /**
     * Displays the messages describing the dice rolls that determined which
     * player starts the game
     */
    private void showGameStartMessages() {
        gameInfoPanel.addMessage("Game starts");

        String whiteName = round.getWhite().getName();
        String blackName = round.getBlack().getName();
        Dice dice = round.getRoundDice();
        int diceWhite = dice.getDice1();
        int diceBlack = dice.getDice2();

        gameInfoPanel.addMessage(whiteName + " (White) rolled: " + diceWhite);
        gameInfoPanel.addMessage(blackName + " (Black) rolled: " + diceBlack);

        gameInfoPanel.addMessage(
                round.getCurrentPlayer().getName()
                        + " starts with "
                        + diceWhite
                        + " and "
                        + diceBlack
        );
    }

    /**
     * Rolls the dice for a human player and starts their turn
     */
    public void rollDice() {
        disablePlayerControls();
        diceView.reset();
        startTurn(null);
    }


    //***********
    //Managing AI
    //***********

    /**
     * Starts a normal AI turn. The same turn setup used by human players is
     * used here too. After the dice animation finished, the AI starts thinking
     */
    private void startAINormalTurn() {
        startTurn(() -> {
            startAIThinking();
        });
    }

    /**
     * Displays a short delay before the AI chooses its move
     */
    private void startAIThinking() {
        if(!gameActive) {
            return;
        }
        gameInfoPanel.addMessage(round.getCurrentPlayer().getName() + " is thinking...");
        aiThinkingDelay = new Timeline(
                new KeyFrame(Duration.millis(1500), e -> {
                    if(gameActive) {
                        chooseAImoves();
                    }
                })
        );
        aiThinkingDelay.play();
    }

    /**
     * Starts an AI turn if the current player is an AI. The Ai first checks
     * whether it wants to offer a double. Otherwise, it starts its normal turn
     */
    private void startAITurn() {

        if (!isAITurn()) {
            return;
        }

        AIAgent ai = (AIAgent) round.getCurrentPlayer().getStrategy();

        if (ai.shouldDouble(round)) {
            handleDoubleOffer();
            return;
        }
        startAINormalTurn();
    }

    /**
     * Asks the AI to choose its move for the current turn
     */
    private void chooseAImoves() {
        List<Play> aiMoves = aiTurnManager.chooseMoves();

        if (aiMoves.isEmpty()) {
            gameInfoPanel.addMessage("Computer has no legal moves");
            finishTurn();
            return;
        }
        playAIMoves(aiMoves, 0);
    }

    /**
     * Plays the AI's move one at a time with a short delay between the moves
     * so that the player can easily see what the AI did.
     *
     * @param aiMoves   moves selected by the AI
     * @param moveIndex index of the move currently being played
     */
    private void playAIMoves(List<Play> aiMoves, int moveIndex) {
        if(!gameActive) {
            return;
        }
        if(moveIndex >= aiMoves.size()) {
            finishTurn();
            return;
        }
        Play play = aiMoves.get(moveIndex);
        round.executePlay(play);

        if(round.checkWinConditions()){
            handleGameWon();
            return;
        }
        updateHighlight.run();
        boardView.resetBoard();
        gameInfoPanel.setMovesRemaining(round.getMoves().length);
        updateDiceAppearance();

        showAIMoveMessage(play);

        aiMoveDelay = new Timeline(new KeyFrame(Duration.millis(1500), event -> {
            if (gameActive) {
                playAIMoves(aiMoves, moveIndex + 1);
            }
        }));

        aiMoveDelay.play();

    }

    /**
     * Adds a readable description of an AI move to the gamelog
     *
     * @param play the move made by the AI
     */
    private void showAIMoveMessage(Play play) {
        String playerName = play.getPlayer().getName();
        if (play.getType() == Play.Type.OFF_BAR) {
            int finalPosition = play.getForwardPoint() + 1;

            gameInfoPanel.addMessage(playerName + " entered a checker from the bar to point " + finalPosition);

        } else if (play.getType() == Play.Type.BEAR_OFF) {
            int startingPosition = play.getInitialPoint() + 1;

            gameInfoPanel.addMessage(playerName + " bore off a checker from point " + startingPosition);

        } else {
            int finalPosition = play.getForwardPoint() + 1;
            int startingPosition = play.getInitialPoint() + 1;

            gameInfoPanel.addMessage(playerName + " moved from point " + startingPosition + " to point " + finalPosition);
        }
    }


    //*****************
    //Managing Doubling
    //*****************


    /**
     * Offers a double to the opponent. If the opponent is an AI, the AI
     * responds automatically. Otherwise, the human player is shows a
     * confirmation dialog
     */
    public void handleDoubleOffer() {
        if (!round.offerDouble()) {
            return;
        }
        Player player = round.getCurrentPlayer();
        Player opponent = round.getOpponent(player);
        gameInfoPanel.addMessage(player.getName() + " offered double");
        disablePlayerControls();
        if (opponent.isAI()) {
            handleAIDoubleResponse();
        } else {
            Platform.runLater(showDoubleAlert);
        }
    }

    /**
     * Allows the AI to decide whether to accept a double or not
     */
    public void handleAIDoubleResponse() {
        Player opponent = round.getOpponent(round.getCurrentPlayer());
        AIAgent ai = (AIAgent) opponent.getStrategy();

        if (ai.shouldAcceptDouble(round)) {
            acceptDouble();
        } else {
            declineDouble();
        }
    }

    /**
     * Accepts the offered double and continues the game
     */
    public void acceptDouble() {
        round.acceptDouble();

        gameInfoPanel.setDoublingFactor(round.getDoublingFactor());
        Player player = round.getCurrentPlayer();
        Player opponent = round.getOpponent(player);

        gameInfoPanel.addMessage(opponent.getName() + " accepted a double");
        gameInfoPanel.addMessage("Doubling factor is now " + round.getDoublingFactor());

        if (player.isAI()) {
            startAINormalTurn();
        } else {
            enablePlayerControls();
        }

    }

    /**
     * Declines the offered double. The player who offered the double wins the
     * current double value in points.
     */
    public void declineDouble() {
        Player winner = round.getCurrentPlayer();
        Player decliner = round.getOpponent(winner);

        round.declineDouble();
        doubleDeclined = true;
        pointsWon = round.getDoublingFactor();
        int points = pointsWon;
        gameInfoPanel.addMessage(decliner.getName() + " declined the double");

        gameInfoPanel.addMessage(winner.getName() + " wins " + points + " point(s)");

        addPointsToScore(winner, points);
        finishGame();
    }


    /**
     * Handles a normal game win. The winner receives the appropriate number of points
     * before the controller decides whether the match has also been won
     */
    private void handleGameWon() {

        addPointsToWinner();
        finishGame();
    }

    /**
     * Performs the common cleanup after a game ends. If the match has been won,
     * the match winning screen is shown. Otherwise, the board reset for the next game
     */
    private void finishGame() {
        disablePlayerControls();
        boardView.clearLogicalSelection();
        if (matchIsWon()) {
            showWinScreen();
        } else {
            prepareNextGame();
        }
    }

    /**
     * Adds the appropriate number of points to the game winner
     */
    private void addPointsToWinner() {
        Player winner = round.getWinner();
        pointsWon = getPointsWon();
        addPointsToScore(winner, pointsWon);
    }

    /**
     * Calculates the number of point won for the current game. The win
     * type determines whether the result is a normal win, gammon or
     * backgammon. This value is then multiplied by the current doubling
     * factor
     *
     * @return number of points won
     */
    private int getPointsWon() {
        int winTypeValue = round.getWinTypeValue();
        int doublingFactor = round.getDoublingFactor();
        return winTypeValue * doublingFactor;
    }

    /**
     * Adds points to the appropriate player's match score and updates
     * the score displayed on screen
     *
     * @param winner player receiving the points
     * @param points number of points added
     */
    private void addPointsToScore(Player winner, int points) {
        if (winner == round.getWhite()) {
            whiteScoreValue += points;
            whiteScore.setText(winner.getName() + ": " + whiteScoreValue);
        } else {
            blackScoreValue += points;
            blackScore.setText(winner.getName() + ": " + blackScoreValue);
        }
    }

    /**
     * Checks whether either player has reached the target score
     *
     * @return true if the match has been won
     */
    private boolean matchIsWon() {
        if (whiteScoreValue >= round.getTargetScore()) {
            return true;
        }
        if (blackScoreValue >= round.getTargetScore()) {
            return true;
        }
        return false;
    }

    /**
     * Resets the board and prepares the next game in the match
     */
    private void prepareNextGame() {
        Player winner = round.getWinner();

        if(!doubleDeclined) {
            int winTypeValue = round.getWinTypeValue();
            int points = getPointsWon();
            String winMessage = winner.getName() + " wins " + points + " point(s)";

            if(winTypeValue == 2){
                winMessage += " - GAMMON";
            }else if (winTypeValue == 3){
                winMessage += " - BACKGAMMON";
            }
            gameInfoPanel.addMessage(winMessage);
        }
        //Reveal the complete board at the end of a Fog of War game
        if (fogOfWar){
            boardView.setFogOfWar(false);
            boardView.resetBoard();
        }

        nextGameDelay= new  Timeline(
                new KeyFrame(Duration.millis(3000), event -> {
                    if(!gameActive){
                        return;
                    }
                    gameInfoPanel.reset();
                    gameInfoPanel.addMessage("Ready for next Game");

                    round.resetRound();
                    boardView.resetBoard();
                    diceView.reset();

                    doubleDeclined = false;
                    pointsWon = 0;
                    startButton.setDisable(false);
                })
        );
        nextGameDelay.play();
    }

    /**
     * Displays the screen shown when a player wins the match
     */
    private void showWinScreen() {

        Player winner = round.getWinner();


        int points = pointsWon;
        Label winLabel = new Label(winner.getName() + " Wins the Match!");
        Label pointsLabel;

        if (points == 1) {
            pointsLabel = new Label("+" + points + " point");
        } else {
            pointsLabel = new Label("+" + points + " points");
        }
        pointsLabel.setStyle("-fx-font-size: 24px;" +
                "-fx-font-weight: bold;");

        Label scoreLabel = new Label("Match score: " + whiteScoreValue + " - " + blackScoreValue);
        scoreLabel.setStyle("-fx-font-size: 20px;");
        Label targetScoreLabel = new Label("Playing to: " + round.getTargetScore());
        targetScoreLabel.setStyle("-fx-font-size: 20px;");


        winLabel.setStyle("-fx-font-size: 40px;" +
                "-fx-font-weight: bold;");

        Button playAgainButton = new Button("Play Again");
        Button exitButton = new Button("Exit");

        playAgainButton.setStyle("-fx-font-size: 18px;");
        exitButton.setStyle("-fx-font-size: 18px;");

        HBox buttons = new HBox(15);
        buttons.getChildren().addAll(playAgainButton, exitButton);
        buttons.setAlignment(Pos.CENTER);

        VBox winScreen = new VBox(25);
        winScreen.getChildren().addAll(winLabel, pointsLabel, scoreLabel, targetScoreLabel, buttons);
        winScreen.setAlignment(Pos.CENTER);

        layout.setCenter(winScreen);
        layout.setTop(null);
        layout.setLeft(null);
        layout.setBottom(null);

        exitButton.setOnAction(event -> {
            System.exit(0);
        });

        playAgainButton.setOnAction(event -> {
            showMainMenu.run();
        });
    }


    /**
     * Updates the visual appearance of the dice based on which dice values have
     * already been used during the turn
     */
    private void updateDiceAppearance() {
        int[] remainingMoves = round.getMoves();

        if (remainingMoves == null) {
            return;
        }
        int value1 = round.getRoundDice().getDice1();
        int value2 = round.getRoundDice().getDice2();

        diceView.updateUsedDice(remainingMoves, value1, value2);
    }

    /**
     * Checks whether the current player is controlled by an AI
     *
     * @return true if the current player is an AI
     */
    private boolean isAITurn() {
        if (round.getCurrentPlayer() == null) {
            return false;
        }
        return round.getCurrentPlayer().isAI();
    }

    /**
     * Enables controls that the human player can use
     */
    private void enablePlayerControls() {
        rollButton.setDisable(false);
        doubleButton.setDisable(!round.canDouble());
    }

    /**
     * Disables player controls while the game is performing an action such as
     * rolling, moving or waiting for AI
     */
    private void disablePlayerControls(){
        rollButton.setDisable(true);
        doubleButton.setDisable(true);
    }

    /**
     * Stops all scheduled game actions when leaving the game screen
     */
    public void stop(){
        gameActive = false;

        if(aiThinkingDelay != null){
            aiThinkingDelay.stop();
        }
        if(aiMoveDelay != null){
            aiMoveDelay.stop();
        }
        if(nextGameDelay != null){
            nextGameDelay.stop();
        }

        aiTurnManager.stop();

    }

    /**
     * Sets the game information panel used by the controlled
     * @param gameInfoPanel panel displaying game info and messages
     */
    public void setGameInfoPanel(GameInfoPanel gameInfoPanel) {
        this.gameInfoPanel = gameInfoPanel;
    }

    /**
     * Sets the action used to display the doubling confirmation dialog
     * @param runnable action that displays the doubling dialog
     */
    public void setShowDoubleAlert(Runnable runnable) {
        this.showDoubleAlert = runnable;
    }

    /**
     * Sets the action used to return to the main menu
     * @param showMainMenu action that displays the main menu
     */
    public void setShowMainMenu(Runnable showMainMenu) {
        this.showMainMenu = showMainMenu;
    }
}