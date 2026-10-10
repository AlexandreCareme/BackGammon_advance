package gameproject.GUI;

import gameproject.game.elements.*;
import gameproject.game.logic.FogOfWar;
import gameproject.game.logic.GameController;
import gameproject.player.Player;
import javafx.geometry.Pos;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Circle;
import javafx.scene.control.Label;

import java.util.List;


/**
 * Displays the Backgammon board and handles player interaction with it.
 * This class is responsible for drawing the board, checkers, the bar and bear-off
 * area. It also handles selecting checkers, highlighting legal moves for human players.
 * The actual game rules and game state are handled by the model classes, especially round, board
 * and play.
 */
public class BoardView extends Pane {
    private Polygon[] triangles;
    private Rectangle bar;
    private Pane offBoard;
    private Label bearOffLabel;
    private Label barLabel;
    private Label[] pointLabels;
    private GameInfoPanel gameInfoPanel;


    private Round round;
    private GameController gameController;

    private int selectedPoint = -1;
    private boolean selectedFromBar = false;
    private boolean bearOffAvailable = false;
    private boolean fogEnabled = false;
   private FogOfWar fogOfWar = new FogOfWar();


    /**
     * Creates the visual Backgammon board
     * @param round current game round containing the board state
     * @param offBoard pane used to display checkers that have been borne off
     */
    public BoardView(Round round, Pane offBoard) {
        this.round = round;
        this.offBoard = offBoard;

        /*
         * Bear-off area
         */
        bearOffLabel = new Label("BEAR OFF");

        bearOffLabel.setStyle("-fx-font-family: 'Georgia';" +
                "-fx-font-size: 20px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #E8D2B2;");


        bearOffLabel.setRotate(-90);

        bearOffLabel.setPrefWidth(500);
        bearOffLabel.setPrefHeight(80);
        bearOffLabel.setAlignment(Pos.CENTER);

        bearOffLabel.setLayoutX(-210);
        bearOffLabel.setLayoutY(275);

        offBoard.getChildren().add(bearOffLabel);

        offBoard.setOnMouseClicked(event ->{
            makeBearOffMove();
        });

        /*
         * Creating the 24 board points (where points are triangles)
         */
        triangles = new Polygon[24];
        for (int i = 0; i < 24; i++) {
            triangles[i] = new Polygon();
            getChildren().add(triangles[i]);
        }

        /*
         * Point numbers
         */
        pointLabels = new Label[24];
        for (int i = 0; i < 24; i++) {
            pointLabels[i] = new Label(String.valueOf(i + 1));

            pointLabels[i].setStyle("-fx-font-family: 'Georgia';" +
                    "-fx-font-size: 12px;" +
                    "-fx-font-weight: bold;" +
                    "-fx-text-fill: #FFF0D6;");

            getChildren().add(pointLabels[i]);

        }

        /*
         * Middle bar
         */
        bar = new Rectangle();
        barLabel = new Label("BAR");

        barLabel.setStyle("-fx-font-family: 'Georgia';" +
                "-fx-font-size: 20px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #E8D2B2;");


        barLabel.setRotate(-90);
        getChildren().add(bar);
        getChildren().add(barLabel);

        setStyle("-fx-background-color: #A8794D;" +
                "-fx-border-color: #382719;" +
                "-fx-border-width: 8px;");

        /*
         * Resize the board whenever the window changes size
         */
        widthProperty().addListener((obs, oldValue, newValue) -> resizeBoard());
        heightProperty().addListener((obs, oldValue, newValue) -> resizeBoard());

        resizeBoard();
        setupPointsClicks();
    }

    /**
     * Resizes and redraws the board based on its current dimensions
     */
    private void resizeBoard() {
        double boardWidth = getWidth();
        double boardHeight = getHeight();

        if (boardHeight <= 0 || boardWidth <= 0) {
            return;
        }
        double margin = 10;
        double barWidth = boardWidth * 0.06;


        double sideWidth = (boardWidth - 2 * margin - barWidth) / 2;
        double pointWidth = sideWidth / 6;
        double pointHeight = boardHeight * 0.45;

        /*
         * Middle bar
         */
        bar.setX(margin + sideWidth);
        bar.setY(margin);
        bar.setWidth(barWidth);
        bar.setHeight(boardHeight - 2 * margin);
        bar.setFill(Color.web("#70452F"));
        barLabel.setLayoutX(bar.getX() + bar.getWidth() / 2 -23);
        barLabel.setLayoutY(bar.getY() + bar.getHeight() / 2  - barLabel.getPrefWidth() / 2);



        /*
         * 24 board points
         */
        for (int i = 0; i < 24; i++) {
            int col;
            if (i < 12) {
                col = i;
            } else {
                col = 23 - i;
            }
            double x = margin + col * pointWidth;

            if (i >= 6 && i < 12) {
                x += barWidth;
            }

            if (i >= 12) {
                x = margin + (23 - i) * pointWidth;
                if (i < 18) {
                    x += barWidth;
                }
            }
            /*
             * Draw the triangle
             */
            Polygon triangle = triangles[i];
            triangle.getPoints().clear();

            if (i < 12) {

                //Top row
                triangle.getPoints().addAll(x, margin, x + pointWidth, margin, x + pointWidth / 2, margin + pointHeight);

            } else {
                //Bottom row
                triangle.getPoints().addAll(x, boardHeight - margin, x + pointWidth, boardHeight - margin, x + pointWidth / 2, boardHeight - margin - pointHeight);
            }

            /*
             *Alternate the point colours
             */

            // Alternate the point colours
            if (i % 2 == 0) {
                triangle.setFill(Color.web("#D9B58A"));
            } else {
                triangle.setFill(Color.web("#633B2A"));
            }

            triangles[i].setStroke(Color.web("#3E2A1D"));
            triangles[i].setStrokeWidth(1);


            /*
             * Position the point number
             */
            Label pointLabel = pointLabels[i];
            pointLabel.setLayoutX(x + pointWidth/2-5);
            if(i >= 12){
                pointLabel.setLayoutY(boardHeight - margin - 20);
            }else{
                pointLabel.setLayoutY(margin + 5);
            }

        }
        drawCheckers();

    }

    /**
     * Removes the old checker graphics and draws all checkers
     * according to the current board state
     */
    private void drawCheckers() {
        //Remove old checkers from the board
        getChildren().removeIf(node -> node instanceof Circle);

        //Remove old borne-off checkers
        offBoard.getChildren().removeIf(node -> node instanceof Circle);

        double radius = 20;
        double spacing = radius * 1.6;

        /*
         * Draw checkers on the 24 points
         */
        for (int i = 0; i < 24; i++) {

            Point point = round.getCurrentBoard().getPoints().get(i);

            int whiteCheckers = point.getCheckers(Player.Color.WHITE);
            int blackCheckers = point.getCheckers(Player.Color.BLACK);

            //White checkers are always visible
            for (int j = 0; j < whiteCheckers; j++) {
                drawChecker(i, j, true, radius, spacing);
            }
            //Black checkers are visible in normal game
            // or temporarily revealed in Fog Of War
            if(!fogEnabled || fogOfWar.isRevealed(i)){
                int numberToDraw = blackCheckers;

                //In Fog of War, only reveal a single opponent checker
                if(fogEnabled){
                    numberToDraw = fogOfWar.getVisibleOpponentCheckers(i);
                }
                for (int j = 0; j < numberToDraw; j++) {
                    drawChecker(i, j, false, radius, spacing);
                }
            }
        }
        drawBarCheckers();
        drawOffBoardCheckers();
    }

    /**
     * Refreshes the checker graphics on the board
     */
    public void refreshBoard(){
        drawCheckers();
    }

    /**
     * Draws one checker on a board point
     * @param pointIndex index of the point containing the checker
     * @param checkerNumber positon of the checker in the stack
     * @param white true if the checker is white, false if black
     * @param radius radios of the checker
     * @param spacing vertical spacing between checkers
     */
    private void drawChecker(int pointIndex, int checkerNumber, boolean white, double radius, double spacing) {
        Circle checker = new Circle(radius);


        if (white) {
            checker.setFill(Color.web("#FFF5E3"));
            checker.setStroke(Color.web("#8B6F47"));
        } else {
            checker.setFill(Color.web("#29231F"));
            checker.setStroke(Color.web("#BDA789"));
        }


        /*
         * Calculate the checker's position
         */
        double boardWidth = getWidth();
        double boardHeight = getHeight();

        double margin = 10;
        double barWidth = boardWidth * 0.06;

        double sideWidth = (boardWidth - 2 * margin - barWidth) / 2;
        double pointWidth = sideWidth / 6;

        int col;

        if (pointIndex < 12) {
            col = pointIndex;
        } else {
            col = 23 - pointIndex;
        }
        double x = margin + col * pointWidth + pointWidth / 2;
        if (pointIndex >= 6 && pointIndex < 12) {
            x += barWidth;
        }
        if (pointIndex >= 12) {
            x = margin + col * pointWidth + pointWidth / 2;

            if (pointIndex < 18) {
                x += barWidth;
            }
        }
        double y;

        if (pointIndex < 12) {
            y = margin + radius + checkerNumber * spacing;
        } else {
            y = boardHeight - margin - radius - checkerNumber * spacing;
        }

        checker.setCenterX(x);
        checker.setCenterY(y);

        /*
         * Handle checker selection
         */
        checker.setOnMouseClicked(event -> {

            Player currentPlayer = round.getCurrentPlayer();

            if (currentPlayer == null || currentPlayer.isAI()) {
                return;
            }

            Player.Color checkerColour = white ? Player.Color.WHITE : Player.Color.BLACK;

            if (currentPlayer.getColor() != checkerColour){
                return;
            }
            selectedFromBar = false;
            selectedPoint = pointIndex;

            showLegalMoves();
            /*
             * This will prevent clicking the checker from also triggering
             * the triangle underneath it
             */
            event.consume();
        });
        getChildren().add(checker);
    }


    /**
     * Draws all checkers currently on the bar
     */
    private void drawBarCheckers() {
        Bar bar = round.getCurrentBoard().getBar();
        int whiteCheckers = bar.getCheckers(Player.Color.WHITE);
        int blackCheckers = bar.getCheckers(Player.Color.BLACK);

        double radius = 20;
        double spacing = radius * 1.6;


        double boardWidth = getWidth();
        double boardHeight = getHeight();

        double barX = boardWidth / 2;

        //White Checkers

        for (int i = 0; i < whiteCheckers; i++) {
            Circle checker = new Circle(radius);
            checker.setFill(Color.web("#FFF5E3"));
            checker.setStroke(Color.web("#8B6F47"));
            checker.setCenterX(barX);
            checker.setCenterY(boardHeight / 2 - radius - i * spacing);

            checker.setOnMouseClicked(event -> {

                Player currentPlayer = round.getCurrentPlayer();

                if (currentPlayer == null || currentPlayer.isAI()) {
                    return;
                }

                if (currentPlayer.getColor() != Player.Color.WHITE){
                    return;
                }
                selectedFromBar = true;
                selectedPoint = -1;
                showLegalMoves();
                event.consume();
            });
            getChildren().add(checker);
        }

        //Black checker from the bar
        for (int i = 0; i < blackCheckers; i++) {
            Circle checker = new Circle(radius);
            checker.setFill(Color.web("#29231F"));
            checker.setStroke(Color.web("#BDA789"));
            checker.setCenterX(barX);
            checker.setCenterY(boardHeight / 2 + radius + i * spacing);

            checker.setOnMouseClicked(event -> {

                Player currentPlayer = round.getCurrentPlayer();

                if (currentPlayer == null || currentPlayer.isAI()) {
                    return;
                }

                if (currentPlayer.getColor() != Player.Color.BLACK){
                    return;
                }
                selectedFromBar = true;
                selectedPoint = -1;

                showLegalMoves();
                event.consume();
            });
            getChildren().add(checker);
        }
    }

    /**
     * Draws all checkers that have been borne off
     */
    private void drawOffBoardCheckers() {
        OffBoard offBoardModel = round.getCurrentBoard().getOffBoard();
        int whiteCheckers = offBoardModel.getCheckers(Player.Color.WHITE);
        int blackCheckers = offBoardModel.getCheckers(Player.Color.BLACK);

        double radius = 12;
        double spacing = 18;

        double offBoardHeight = offBoard.getHeight();
        double centerY = offBoardHeight / 2;

        //White Checkers
        for (int i = 0; i < whiteCheckers; i++) {
            Circle checker = new Circle(radius);
            checker.setFill(Color.web("#FFF5E3"));
            checker.setStroke(Color.web("#8B6F47"));

            checker.setCenterX(offBoard.getWidth() / 2);
            checker.setCenterY(centerY - radius - i * spacing);
            offBoard.getChildren().add(checker);
        }

        //Black Checkers
        for (int i = 0; i < blackCheckers; i++) {
            Circle checker = new Circle(radius);
            checker.setFill(Color.web("#FFF5E3"));
            checker.setStroke(Color.web("#8B6F47"));

            checker.setCenterX(offBoard.getWidth() / 2);
            checker.setCenterY(centerY + radius + i * spacing);
            offBoard.getChildren().add(checker);
        }
    }

    /**
     * Connects each board point to its mouse click action
     */
    private void setupPointsClicks() {
        for (int i = 0; i < 24; i++) {
            final int pointIndex = i;

            triangles[i].setOnMouseClicked(event -> {
                makeMove(pointIndex);
            });

        }
    }

    /**
     * Attempts to make a normal move to the selected destination
     * @param destination index of the destination point
     */
    private void makeMove(int destination) {
        int[] moves = round.getMoves();
        Player currentPlayer = round.getCurrentPlayer();

        if (currentPlayer == null || currentPlayer.isAI()) {
            return;
        }

        if (moves == null || moves.length == 0) {
            return;
        }
        List<Play> legalPlays = round.getRequiredPlays(moves);

        for (Play play : legalPlays) {
            boolean correctStart;

            if (selectedFromBar) {
                correctStart = play.getInitialPoint() == -1;
            } else {
                correctStart = play.getInitialPoint() == selectedPoint;
            }
            if (correctStart && play.getForwardPoint() == destination) {
                round.executePlay(play);

                int startingPostion =  play.getInitialPoint() + 1;
                int finalPosition = play.getForwardPoint() + 1;
                String playerName = play.getPlayer().getName();
                if (play.getType() == Play.Type.OFF_BAR){
                    gameInfoPanel.addMessage(playerName + " entered a checker from the bar to point " + finalPosition);
                }else{

                    gameInfoPanel.addMessage(playerName + " moved from point " +startingPostion + " to point " + finalPosition +  ".");
                }

                clearLogicalSelection();
                fogOfWar.clearRevealPoints();
                drawCheckers();

                gameController.movePlayed();

                return;
            }
        }
    }

    /**
     * Attempts to bear off the selected checker
     */
    private void makeBearOffMove(){

        if (round.getCurrentPlayer() == null
                || round.getCurrentPlayer().isAI()) {
            return;
        }

        if (!bearOffAvailable){
            return;
        }
        if (selectedPoint == -1){
            return;
        }
        int[] moves = round.getMoves();
        if (moves == null || moves.length == 0){
            return;
        }
        List<Play> legalPlays = round.getRequiredPlays(moves);
        for (Play play: legalPlays){
            if (play.getInitialPoint() == selectedPoint && play.getForwardPoint() == -1){
                round.executePlay(play);
                int startPosition = play.getInitialPoint() + 1;
                String playerName = play.getPlayer().getName();

                gameInfoPanel.addMessage(
                        playerName + " bore off a checker from point " +
                                startPosition + "."
                );

                clearLogicalSelection();
                drawCheckers();

                gameController.movePlayed();
                return;
            }
        }
    }

    /**
     * Clears the currently selected checker and all visual highlights
     */
    public void clearLogicalSelection() {
        clearVisualHighlights();
        selectedPoint = -1;
        selectedFromBar = false;
        bearOffAvailable = false;
    }

    /**
     * Show all legal destinations for the currently selected checker
     */
    private void showLegalMoves() {

        clearVisualHighlights();
        bearOffAvailable = false;
        fogOfWar.clearRevealPoints();

        int[] moves = round.getMoves();

        if (moves == null || moves.length == 0) {
            return;
        }

        List<Play> legalPlays = round.getRequiredPlays(moves);


        for (Play play : legalPlays) {
            boolean correctStart;

            if(selectedFromBar){
                correctStart = play.getInitialPoint() == -1;
            }else{
                correctStart = play.getInitialPoint() == selectedPoint;
            }

            if(correctStart) {
                int destination = play.getForwardPoint();
                /*
                 * Highlight legal board destination
                 */
                if (destination >= 0 && destination < 24) {
                    triangles[destination].setStroke(Color.YELLOW);
                    triangles[destination].setStrokeWidth(4);


                    /*
                     * Fog of War
                     * Reveal an opponent checker if there is exactly one
                     */

                    if (fogEnabled && destination >= 0 && destination < 24) {
                        Player.Color currentPlayer = round.getCurrentPlayer().getColor();
                        Point destinationPoint = round.getCurrentBoard().getPoints().get(destination);

                        fogOfWar.revealIfNeeded(destinationPoint, currentPlayer, destination);
                    }
                }


                /*
                 * Highlight the bear off area
                 */
                if (!selectedFromBar && play.getInitialPoint() == selectedPoint && play.getForwardPoint() == -1) {
                    bearOffAvailable = true;
                    offBoard.setStyle("-fx-background-color: gray;" +
                            "-fx-border-color: yellow;" +
                            "-fx-border-width: 4px;");
                }
            }
        }
        if(fogEnabled){
            drawCheckers();
        }
    }

    /**
     * Removes all visual move highlights from the board
     */
    private void clearVisualHighlights(){
        for (int i = 0; i < 24; i++) {
            triangles[i].setStroke(Color.BLACK);
            triangles[i].setStrokeWidth(1);
        }

        offBoard.setStyle("-fx-background-color: #C4A078;" +
                "-fx-border-color: #8B6F47;" +
                "-fx-border-width: 3px;");

    }

    /**
     * Sets the game controller used to notify the game when a move
     * has been made
     * @param gameController game controller
     */
    public void setGameController(GameController gameController) {
        this.gameController = gameController;
    }

    /**
     * Resets the board selection and redraws the checkers
     */
    public void resetBoard(){
        selectedPoint = -1;
        selectedFromBar = false;
        clearLogicalSelection();
        drawCheckers();
    }

    /**
     * Sets the game info panel used to display move messages
     * @param gameInfoPanel game info panel
     */
    public void setGameInfoPanel(GameInfoPanel gameInfoPanel){
        this.gameInfoPanel = gameInfoPanel;
    }

    /**
     * Sets the fog of war used to play fog of war
     * @param fogEnabled fog of war
     */
    public void setFogOfWar(boolean fogEnabled){
        this.fogEnabled = fogEnabled;
    }
}