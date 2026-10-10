package gameproject.GUI;
import gameproject.game.logic.GameController;
import gameproject.game.logic.GameMode;
import gameproject.game.elements.Round;
import gameproject.player.Human;
import gameproject.player.Player;


import javafx.scene.layout.Pane;
import javafx.scene.shape.Line;
import javafx.scene.paint.Color;
import javafx.scene.Parent;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.layout.StackPane;
import javafx.geometry.Insets;

import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

import javafx.stage.Stage;


import gameproject.ai.AdvancedAgent;
import gameproject.ai.BoardEvaluator;
import gameproject.ai.FogEasyAgent;
import javafx.util.Duration;

/**
 * Main JavaFX application for the Backgammon game. This class is responsible
 * for creating and switching between the different screens of the application.
 *
 * The main game rules and game state are handled by the model classes, while this class
 * mainly handles screen layout and user interface
 */
public class App extends javafx.application.Application {

    /**
     * Starts the JavaFX application and displays the main menu
     * @param stage the primary stage for this application, onto which
     * the application scene can be set.
     * Applications may create other stages, if needed, but they will not be
     * primary stages.
     */

    private static final String GAME_VERSION = "1.0.3";
    @Override
    public void start(Stage stage) {

        //Main menu title

        Label title = new Label("BACKGAMMON");
        title.setStyle(UIStyle.mainTitleStyle());


        //Button for starting a two player game
        Button friendButton = new Button("Play a Friend");
        UIStyle.styleButton(friendButton);
        friendButton.setPrefWidth(220);

        friendButton.setOnAction(event->{
            showPlayerNamesScreen(stage, GameMode.FRIEND);
        });

        //Button for starting a game against the computer
        Button aiButton = new Button("Play vs AI");
        UIStyle.styleButton(aiButton);
        aiButton.setPrefWidth(220);

        aiButton.setOnAction(event->{
            showPlayerNamesScreen(stage, GameMode.VS_AI);
        });

        //Button for opening the tutorial
        Button tutorialButton = new Button("How to Play");
        UIStyle.styleButton(tutorialButton);
        tutorialButton.setPrefWidth(220);

        tutorialButton.setOnAction(event->{
            TutorialView tutorialView = new TutorialView();

            changeScreen(
                    stage,
                    tutorialView.getLayout(
                            () -> start(stage),
                            () -> showDemoGame(stage)),
                    "Backgammon - How to Play"
            );
        });

        //Button for closing the application
        Button exitButton = new Button("Exit");
        UIStyle.styleButton(exitButton);
        exitButton.setPrefWidth(220);

        exitButton.setOnAction(event->{
            System.exit(0);
        });


        //Button for opening Fog Of War mode
        Button fogButton = new Button("Fog Of War");
        fogButton.setPrefWidth(220);
        //Keep the normal button size/font, then override the colours
        String fogNormalStyle = UIStyle.buttonStyle() +
                "-fx-background-color: transparent;" +
                "-fx-text-fill: #800020;" +
                "-fx-border-color: gray;" +
                "-fx-border-width: 2px;" +
                "-fx-border-radius: 8px;" +
                "-fx-background-radius: 8px;";
        fogButton.setStyle(fogNormalStyle);

        //Hide the button until the mouse is over it
        fogButton.setOpacity(0);
        fogButton.setOnMouseEntered(event->{
            FadeTransition fadeIn = new FadeTransition(Duration.millis(200), fogButton);
            fadeIn.setToValue(1);
            fadeIn.play();
        });
        fogButton.setOnMouseExited(event->{
            FadeTransition fadeOut = new FadeTransition(Duration.millis(300), fogButton);
            fadeOut.setToValue(0);
            fadeOut.play();
        });
        fogButton.setOnAction(event->{
            showPlayerNamesScreen(stage, GameMode.VS_AI_FOG);
        });

        //Put the title and menu buttons in the centre of the screen
        VBox buttonsBox = new VBox(25);
        buttonsBox.getChildren().addAll(
                title, friendButton, aiButton,
                tutorialButton, exitButton, fogButton
        );
        buttonsBox.setAlignment(Pos.CENTER);

        // Small About button in the bottom-left corner
        Button aboutButton = new Button("About");
        UIStyle.styleButton(aboutButton);
        aboutButton.setPrefWidth(130);
        aboutButton.setMinWidth(130);

        aboutButton.setOnAction(event -> {
            showAboutScreen(stage);
        });

        HBox bottomLeft = new HBox(aboutButton);
        bottomLeft.setAlignment(Pos.CENTER_LEFT);
        bottomLeft.setPadding(new Insets(0, 0, 15, 15));

        // Combine the main buttons and the About button
        BorderPane mainMenu = new BorderPane();
        mainMenu.setCenter(buttonsBox);
        mainMenu.setBottom(bottomLeft);
        mainMenu.setBackground(getBackground(null));

        // Display the main menu
        changeScreen(stage, mainMenu, "Backgammon");

    }

    /**
     * Creates and displays the main game screen
     * @param stage main JavaFX window
     * @param whitePlayerName name of the white player
     * @param blackPlayerName name of the black player
     * @param mode selected game mode
     * @param difficulty the selected AI difficulty
     * @param pointToWin the number of points required to win the match
     */
    private void showGameScreen(Stage stage,
                                String whitePlayerName, String blackPlayerName,
                                GameMode mode, String difficulty, int pointToWin) {



        /*
         * Create the main game layout and the game info panel. The GameInfoPanel
         * displays info about the current turn, dice, remaining moves and doubling factor
         */
        GameInfoPanel gameInfoPanel = new GameInfoPanel();
        gameInfoPanel.setFogOfWar(mode == GameMode.VS_AI_FOG);

        BorderPane layout = new BorderPane();
        if(mode == GameMode.VS_AI_FOG){
            layout.setStyle("-fx-background-color: linear-gradient(" +
                    "to bottom, #596168, #343A40);");
        }else{
            layout.setStyle( "-fx-background-color: linear-gradient(" +
                    "to bottom, #C69C6D, #A87545);");
        }

        //Add the fog effect for Fog of War
        StackPane gameRoot = new StackPane();
        gameRoot.getChildren().add(layout);

        /*
         *Fog of War visual Effects
         * A large semi-transparent texture is placed over the game board to create
         * the appearance of moving fog
         */
        if (mode == GameMode.VS_AI_FOG){


            Image fogTexture = new Image(getClass().getResource("/gameproject/images/fog_texture.jpg").toExternalForm());
            ImageView fogView = new ImageView(fogTexture);

            /*
             * The fog is made larger than the game window so that its edges are not visible
             * when the fog moves or the window is resized
             */

            fogView.setPreserveRatio(false);
            fogView.fitWidthProperty().bind(gameRoot.widthProperty().multiply(1.2));
            fogView.fitHeightProperty().bind(gameRoot.heightProperty());
            fogView.setOpacity(0.4);
            fogView.setMouseTransparent(true);


            gameRoot.getChildren().add(fogView);

            /*
             * Move the fog slowly from side to side
             * AutoReverse makes the fog move back and forth continuously
             */
            TranslateTransition fogAnimation =
                    new TranslateTransition(Duration.seconds(20), fogView);

            fogAnimation.setFromX(-100);
            fogAnimation.setToX(100);
            fogAnimation.setCycleCount(TranslateTransition.INDEFINITE);
            fogAnimation.setAutoReverse(true);
            fogAnimation.play();

            /*
             * Slowly change the fog opacity to make the fog density appear
             * to change over time
             */
            FadeTransition fadeTransition = new FadeTransition(Duration.seconds(10), fogView);
            fadeTransition.setFromValue(0.35);
            fadeTransition.setToValue(0.55);
            fadeTransition.setAutoReverse(true);
            fadeTransition.setCycleCount(TranslateTransition.INDEFINITE);
            fadeTransition.play();
        }

        /*
         * Top: title, score and home button
         */

        Label title = new Label("BACKGAMMON");
        title.setStyle(UIStyle.gameTitleStyle());


        Label whiteScore = new Label(whitePlayerName + ": 0");
        Label blackScore = new Label(blackPlayerName + ": 0");

        whiteScore.setStyle(UIStyle.playerNameStyle());
        blackScore.setStyle(UIStyle.playerNameStyle());


        HBox scoreBox = new HBox(40);
        scoreBox.getChildren().addAll(whiteScore, blackScore);
        scoreBox.setAlignment(Pos.CENTER);
        Button homeButton = new Button("⌂");
        UIStyle.styleButton(homeButton);
        homeButton.setPrefWidth(50);

        VBox titleAndScore = new VBox(5);
        titleAndScore.getChildren().addAll(title, scoreBox);
        titleAndScore.setAlignment(Pos.CENTER);

        BorderPane top = new BorderPane();
        top.setCenter(titleAndScore);
        top.setRight(homeButton);

        BorderPane.setAlignment(homeButton, Pos.TOP_RIGHT);
        BorderPane.setMargin(homeButton, new Insets(10, 15, 0, 0));
        top.setPrefHeight(100);

        /*
         * Game model: create the players and the round
         */
        Player white =null;
        Player black = null;

        if (mode == GameMode.FRIEND){
            white = createHumanPlayer(whitePlayerName, Player.Color.WHITE);
            black = createHumanPlayer(blackPlayerName, Player.Color.BLACK);
        }else if (mode == GameMode.VS_AI){
            white = createHumanPlayer(whitePlayerName, Player.Color.WHITE);
            black = createAIPlayer(blackPlayerName, Player.Color.BLACK, difficulty);
        }else if (mode == GameMode.VS_AI_FOG){
            // The Fog of War AI only evaluates its own checkers, so it cannot see the hidden ones either
            white = createHumanPlayer(whitePlayerName, Player.Color.WHITE);
            black = createFogAIPlayer(blackPlayerName, Player.Color.BLACK);

        }else if (mode == GameMode.DEMO){
            white = createAIPlayer("Easy AI", Player.Color.WHITE, "Easy");
            black = createAIPlayer("Advanced AI", Player.Color.BLACK, "Advanced");
        }

        Round round = new Round(white, black);
        round.setTargetScore(pointToWin);


        /*
         * Left: player info
         */
        PlayerView playerView = new PlayerView(whitePlayerName, blackPlayerName);
        VBox left = playerView.getLayout();


        /*
         * Update the player highlight and the turn shown in GameInfoPanel.
         * This is passed to the GameController because the controller needs to
         * update the GUI after each turn and moves.
         */
        Runnable updateHighlight = () -> {
            Player currentPlayer = round.getCurrentPlayer();
            if(currentPlayer == null){
                return;
            }
            playerView.updateHighlight(currentPlayer);
            gameInfoPanel.setTurn(round.getCurrentPlayer().getName());
        };

        /*
         * Center: board and off-board area
         */
        Pane offBoard = new Pane();

        offBoard.setMinSize(80, 550);
        offBoard.setPrefSize(80, 550);
        offBoard.setMaxSize(80, 550);
        offBoard.setStyle( "-fx-background-color: #D2B48C;" +
                "-fx-border-color: #8B6F47;" +
                "-fx-border-width: 3px;");

        BoardView board = new BoardView(round, offBoard);
        board.setFogOfWar(mode == GameMode.VS_AI_FOG);
        board.setPrefSize(800, 550);


        /*
         * Create the four dice labels. The 3rd and 4th dice are only
         * visible when doubles are rolled because doubles give you four moves
         */
        Label dice1 = new Label("-");
        Label dice2 = new Label("-");
        Label dice3 = new Label("-");
        Label dice4 = new Label("-");


        dice1.setStyle(
                diceLabelStyle());

        dice2.setStyle(
                diceLabelStyle());

        dice3.setStyle(
                diceLabelStyle());
        dice4.setStyle(
                diceLabelStyle());

        HBox diceBox = new HBox(25);
        diceBox.getChildren().addAll(dice1, dice2, dice3, dice4);
        diceBox.setAlignment(Pos.CENTER);
        dice3.setVisible(false);
        dice4.setVisible(false);

        /*
         * Bottom: game control buttons
         */
        Button rollButton = new Button("Roll Dice");
        rollButton.setDisable(true);
        UIStyle.styleButton(rollButton);

        Button startButton = new Button("Start Game");
        UIStyle.styleButton(startButton);

        Button doubleButton = new Button("Double");
        doubleButton.setDisable(true);
        UIStyle.styleButton(doubleButton);


        HBox gameButtons = new HBox(40);
        gameButtons.getChildren().addAll(startButton, rollButton, doubleButton);
        gameButtons.setAlignment(Pos.CENTER);


        VBox bottom = new VBox(10);
        bottom.getChildren().addAll(diceBox, gameButtons);
        bottom.setAlignment(Pos.CENTER);
        bottom.setPrefHeight(130);


        /*
         * Put the board, off-board and game info panel together
         * in the centre of the screen. The two regions
         * provide flexible and spacing around the board
         */
        HBox center = new HBox(20);
        Region leftSpace = new Region();
        Region rightSpace = new Region();

        HBox.setHgrow(leftSpace, Priority.ALWAYS);
        HBox.setHgrow(rightSpace, Priority.ALWAYS);

        center.getChildren().addAll(leftSpace, offBoard, board, gameInfoPanel.getLayout(), rightSpace);

        center.setAlignment(Pos.CENTER);
        layout.setCenter(center);


        /*
         * Create the diceView.
         * It will control the visual behaviour of the four dice.
         */

        DiceView diceView = new DiceView(dice1, dice2, dice3, dice4);

        /*
         * Create the GameController.
         * The controller will handle the game flow while App mainly handles the different
         * GUI components.
         */
        GameController gameController = new GameController(
                round,
                board,
                rollButton,
                doubleButton,
                startButton,
                whiteScore,
                blackScore,
                layout,
                diceView,
                updateHighlight,
                mode == GameMode.VS_AI_FOG);


        /*
         * Actions that allow the GameController to switch screens
         * or display the doubling dialog
         */
        Runnable showMainMenu = () ->{
            start(stage);
        };

        gameController.setShowMainMenu(showMainMenu);
        gameController.setShowDoubleAlert(() -> {
            showDoubleAlert(gameController, round, gameRoot);
        });

        /*
         * Connect the controller and game information panel to the board
         */
        board.setGameController(gameController);
        board.setGameInfoPanel(gameInfoPanel);
        gameController.setGameInfoPanel(gameInfoPanel);


        /*
         * Set all parts of the main game layout
         */
        layout.setTop(top);
        layout.setLeft(left);
        layout.setBottom(bottom);


        /*
         * BUTTONS Action
         */

        //Roll the dice when te Roll Dice button is pressed
        rollButton.setOnAction(event -> {gameController.rollDice();});

        //Start the game when Start Game button is pressed
        startButton.setOnAction(event -> {
            startButton.setDisable(true);
            gameController.startGame();
        });

        /*
         * The demo is controlled entirely by the two AI players, so it
         * starts automatically without pressing Start Game
         */
        if(mode == GameMode.DEMO){
            startButton.setDisable(true);
            gameController.startGame();
        }

        //Return to the main menu when pressed
        homeButton.setOnAction(event -> {
            gameController.stop();
            showMainMenu.run();
        });

        //Offers the other player a double when pressed
        doubleButton.setOnAction(event -> {
            gameController.handleDoubleOffer();
        });


        /*
         * Create and display the game scene
         */
        changeScreen(stage, gameRoot, "Backgammon");
    }


    /**
     * Displays information about the game and its developers.
     *
     * @param stage main JavaFX window
     */
    private void showAboutScreen(Stage stage) {

        // Screen title
        Label title = new Label("ABOUT BACKGAMMON");
        title.setStyle(UIStyle.mainTitleStyle());
        title.setMinWidth(650);
        title.setAlignment(Pos.CENTER);

        // Game version
        Label version = new Label("Version " + GAME_VERSION);
        version.setStyle(
                "-fx-font-family: 'Georgia';" +
                        "-fx-font-size: 18px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: #FFF5E3;"
        );

        // Game description
        Label description = new Label(
                "A classic board game, designed and developed from scratch."
        );
        description.setWrapText(true);
        description.setMaxWidth(650);
        description.setAlignment(Pos.CENTER);
        description.setStyle(
                "-fx-font-family: 'Georgia';" +
                        "-fx-font-size: 16px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: #FFF5E3;"
        );

        // Credits heading
        Label creditsTitle = new Label("DEVELOPMENT TEAM");
        creditsTitle.setStyle(
                "-fx-font-family: 'Georgia';" +
                        "-fx-font-size: 22px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: #FFF5E3;"
        );

        // Developer names
        Label credits = new Label(
                "Alexandre Carême\n" +
                        "Diederick Folbert\n" +
                        "Sven Krijnsen\n" +
                        "Kilian Littan"
        );
        credits.setAlignment(Pos.CENTER);
        credits.setStyle(
                "-fx-font-family: 'Georgia';" +
                        "-fx-font-size: 17px;" +
                        "-fx-text-fill: #FFF5E3;" +
                        "-fx-line-spacing: 6px;"
        );

        // Back button
        Button backButton = new Button("Back to Menu");
        UIStyle.styleButton(backButton);
        backButton.setPrefWidth(220);
        backButton.setOnAction(event -> start(stage));

        // Arrange the text
        VBox content = new VBox(
                22,
                title,
                version,
                description,
                creditsTitle,
                credits,
                backButton
        );
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(40));

        // Root layout keeps the original background image
        StackPane aboutLayout = new StackPane();
        aboutLayout.setBackground(getBackground(null));

        // Darken the entire image, not just the area behind the text
        Region backgroundTint = new Region();
        backgroundTint.setStyle(
                "-fx-background-color: rgba(35, 24, 16, 0.55);"
        );
        backgroundTint.setMouseTransparent(true);

        // Layer the tint over the background and the text over the tint
        aboutLayout.getChildren().addAll(backgroundTint, content);

        changeScreen(stage, aboutLayout, "Backgammon - About");
    }





    private void changeScreen(Stage stage, Parent root, String title) {

        boolean wasShowing = stage.isShowing();
        boolean wasFullScreen = stage.isFullScreen();
        boolean wasMaximized = stage.isMaximized();

        double oldWidth = stage.getWidth();
        double oldHeight = stage.getHeight();
        double oldX = stage.getX();
        double oldY = stage.getY();

        Scene scene;

        if (wasShowing) {
            scene = new Scene(root, oldWidth, oldHeight);
        } else {
            scene = new Scene(root, 1000, 750);
        }


        stage.setTitle(title);

// Set the new scene before showing it.
        stage.setScene(scene);

        if (wasShowing && !wasFullScreen && !wasMaximized) {
            stage.setX(oldX);
            stage.setY(oldY);
            stage.setWidth(oldWidth);
            stage.setHeight(oldHeight);
        }

        if (wasFullScreen) {
            stage.setFullScreen(true);
        } else if (wasMaximized) {
            stage.setMaximized(true);
        }

        stage.show();

    }



    /**
     * Starts the AI demonstration game from the tutorial. The white
     * player uses the Easy AI and the black player uses the Advanced AI
     * @param stage the main JavaFX window
     */
    private void showDemoGame(Stage stage) {
        showGameScreen(stage, "Easy AI", "Advanced AI", GameMode.DEMO, "Advanced", 1);
    }





    /**
     * Displays a classic backgammon doubling panel over the game board.
     *
     * @param gameController controller responsible for the response
     * @param round current game round
     * @param gameRoot root containing the game screen
     */
    private void showDoubleAlert(GameController gameController,
                                 Round round,
                                 StackPane gameRoot) {

        Player currentPlayer = round.getCurrentPlayer();
        Player opponent = round.getOpponent(currentPlayer);

        // Transparent overlay keeps the board visible.
        StackPane overlay = new StackPane();
        overlay.setPickOnBounds(true);

        // Classic ivory doubling cube.
        Label cubeValue = new Label(
                String.valueOf(round.getDoublingFactor())
        );

        cubeValue.setStyle(
                "-fx-font-family: 'Georgia';" +
                        "-fx-font-size: 30px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: #382719;"
        );

        StackPane cube = new StackPane(cubeValue);
        cube.setPrefSize(64, 64);
        cube.setMinSize(64, 64);
        cube.setMaxSize(64, 64);

        cube.setStyle(
                "-fx-background-color: #F2E5C9;" +
                        "-fx-border-color: #6B4B2A;" +
                        "-fx-border-width: 2px;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.18), 4, 0, 1, 2);"
        );

        // Classic heading.
        Label heading = new Label("DOUBLE");
        heading.setStyle(
                "-fx-font-family: 'Georgia';" +
                        "-fx-font-size: 25px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: #382719;"
        );

        // Thin decorative divider.
        Region divider = new Region();
        divider.setPrefHeight(1);
        divider.setMaxHeight(1);
        divider.setStyle("-fx-background-color: #B9A27A;");

        Label offer = new Label(
                currentPlayer.getName() + " offers the double to\n" +
                        opponent.getName()
        );

        offer.setWrapText(true);
        offer.setAlignment(Pos.CENTER);

        offer.setStyle(
                "-fx-font-family: 'Georgia';" +
                        "-fx-font-size: 17px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: #FFF5E3;" +
                        "-fx-text-alignment: center;"
        );


        Label factor = new Label(
                "Current doubling value: " + round.getDoublingFactor()
        );


        factor.setStyle(
                "-fx-font-family: 'Georgia';" +
                        "-fx-font-size: 15px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: #FFF5E3;"
        );

        // Traditional green Accept button.
        Button acceptButton = new Button("Accept");
        acceptButton.setPrefWidth(130);
        acceptButton.setPrefHeight(38);
        acceptButton.setStyle(
                "-fx-font-family: 'Georgia';" +
                        "-fx-font-size: 15px;" +
                        "-fx-text-fill: #FFF9ED;" +
                        "-fx-background-color: #425B3D;" +
                        "-fx-background-radius: 2px;" +
                        "-fx-border-color: #30452D;" +
                        "-fx-border-width: 1px;" +
                        "-fx-border-radius: 2px;" +
                        "-fx-cursor: hand;"
        );

        // Muted burgundy Decline button.
        Button declineButton = new Button("Decline");
        declineButton.setPrefWidth(130);
        declineButton.setPrefHeight(38);
        declineButton.setStyle(
                "-fx-font-family: 'Georgia';" +
                        "-fx-font-size: 15px;" +
                        "-fx-text-fill: #FFF9ED;" +
                        "-fx-background-color: #793E3B;" +
                        "-fx-background-radius: 2px;" +
                        "-fx-border-color: #60302D;" +
                        "-fx-border-width: 1px;" +
                        "-fx-border-radius: 2px;" +
                        "-fx-cursor: hand;"
        );

        HBox buttons = new HBox(12);
        buttons.getChildren().addAll(acceptButton, declineButton);
        buttons.setAlignment(Pos.CENTER);

        // Arrange the panel contents.
        VBox card = new VBox(13);
        card.getChildren().addAll(
                cube,
                heading,
                divider,
                offer,
                factor,
                buttons
        );

        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(22));

        // Keep the panel at a fixed size.
        card.setPrefSize(370, 340);
        card.setMinSize(370, 340);
        card.setMaxSize(370, 340);

        // Flat ivory surface with a simple walnut border.


        card.setStyle(
                "-fx-background-color: #A17B52;" +
                        "-fx-border-color: #4B3020;" +
                        "-fx-border-width: 3px;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.35), 12, 0, 3, 4);"
        );


        Pane woodGrain = new Pane();
        woodGrain.setMouseTransparent(true);
        woodGrain.setPickOnBounds(false);
        woodGrain.setPrefSize(370, 340);

        String[] grainColors = {
                "#896744",
                "#68472C",
                "#997653",
                "#5D3D25",
                "#896744",
                "#A0805D",
                "#68472C",
                "#997653"
        };

        for (int i = 0; i < grainColors.length; i++) {
            Line grain = new Line(
                    0, 20 + i * 42,
                    370, 20 + i * 42
            );

            grain.setStroke(Color.web(grainColors[i]));
            grain.setStrokeWidth(1.0);
            grain.setOpacity(0.55);

            woodGrain.getChildren().add(grain);
        }

        woodGrain.setMaxSize(370, 340);



        // Center the panel initially.
        overlay.getChildren().add(card);
        StackPane.setAlignment(card, Pos.CENTER);

        // Remember the starting position when dragging.
        final double[] dragStart = new double[4];

        card.setOnMousePressed(event -> {
            dragStart[0] = event.getSceneX();
            dragStart[1] = event.getSceneY();
            dragStart[2] = card.getTranslateX();
            dragStart[3] = card.getTranslateY();
        });

        card.setOnMouseDragged(event -> {
            double newX = dragStart[2]
                    + event.getSceneX() - dragStart[0];

            double newY = dragStart[3]
                    + event.getSceneY() - dragStart[1];

            // Keep the panel within the game window.
            double minX = -card.getLayoutX();
            double maxX = gameRoot.getWidth()
                    - card.getLayoutX() - card.getWidth();

            double minY = -card.getLayoutY();
            double maxY = gameRoot.getHeight()
                    - card.getLayoutY() - card.getHeight();

            newX = Math.max(minX, Math.min(newX, maxX));
            newY = Math.max(minY, Math.min(newY, maxY));

            card.setTranslateX(newX);
            card.setTranslateY(newY);
        });

        // Display the panel over the board.
        gameRoot.getChildren().add(overlay);

        // Accept the double.
        acceptButton.setOnAction(event -> {
            gameRoot.getChildren().remove(overlay);
            gameController.acceptDouble();
        });

        // Decline the double.
        declineButton.setOnAction(event -> {
            gameRoot.getChildren().remove(overlay);
            gameController.declineDouble();
        });
    }




    /**
     * Create a human player
     * @param name player's name
     * @param color player's checker colour
     * @return a new human player
     */
    private Player createHumanPlayer(String name, Player.Color color) {
        return new Player(name, color, new Human());
    }

    /** equal weights: every feature of the evaluator counts the same for easy agent */
    private static final double[] EQUAL_WEIGHTS = {0.125, 0.125, 0.125, 0.125, 0.125, 0.125, 0.125, 0.125};

    /** tuned weights for advanced agent  */
    private static final double[] TUNED_WEIGHTS = {0.225, 0.217, 0.119, 0.057, 0.038, 0.006, 0.091, 0.247};

    /**
     * Weights for the medium agent: the first tuned set from the team's Coefficients tournament.
     * At depth 0 they beat Easy in 60% of the games, while Advanced beats them in 66%,
     * so Medium sits in the middle (OvernightExperimentsTest).
     */
    private static final double[] MEDIUM_WEIGHTS = {0.224, 0.152, 0.227, 0.041, 0.034, 0.088, 0.133, 0.101};

    /**
     * Creates an AI player using the selected difficulty.
     * Easy:     depth 0 (only its own turn) with equal weights.
     * Medium:   depth 0 with the first tuned weights (better evaluation, same search as Easy).
     * Advanced: depth 2 with move filter K = 10 and the tuned weights; it falls back to its
     *           depth-1 answer when a decision takes longer than AdvancedAgent.TIME_LIMIT_NANOS.
     *
     * @param name player's name
     * @param color player's checker colour
     * @param difficulty selected AI difficulty
     * @return a new AI player
     */
    private Player createAIPlayer(String name, Player.Color color, String difficulty) {
        AdvancedAgent agent;
        if ("Easy".equals(difficulty)) {
            agent = new AdvancedAgent(new BoardEvaluator(EQUAL_WEIGHTS), 0);
        } else if ("Medium".equals(difficulty)) {
            agent = new AdvancedAgent(new BoardEvaluator(MEDIUM_WEIGHTS), 0);
        } else {
            agent = new AdvancedAgent(new BoardEvaluator(TUNED_WEIGHTS), 2, 10);
        }
        return new Player(name, color, agent);
    }

    /**
     * Creates the AI player for Fog of War. It uses the FogEasyAgent, which only evaluates its
     * own checkers (plus a bonus for hitting a visible blot), so it cannot see the hidden ones either.
     *
     * @param name player's name
     * @param color player's checker colour
     * @return a new Fog of War AI player
     */
    private Player createFogAIPlayer(String name, Player.Color color) {
        return new Player(name, color, new FogEasyAgent(new BoardEvaluator(EQUAL_WEIGHTS)));
    }


    /**
     * Displays the screen where players enter their names and select the
     * game setting.
     * @param stage main JavaFX window
     * @param mode selected game mode
     */
    private void showPlayerNamesScreen(Stage stage,GameMode mode){

        /*
         * Screen title and instructions
         */

        Label title;

        if (mode == GameMode.VS_AI_FOG) {
            title = new Label("FOG OF WAR");
            title.setStyle(
                    "-fx-font-family: 'Georgia';" +
                            "-fx-font-size: 45px;" +
                            "-fx-font-weight: bold;" +
                            "-fx-text-fill: #800020;"
            );
        } else {
            title = new Label("BACKGAMMON");
            title.setStyle(UIStyle.mainTitleStyle());
        }


        Label instruction;
        if(mode == GameMode.VS_AI_FOG){
            instruction = new Label("Game Setup");
        }else {
            instruction =new Label("Enter Players Names");
        }


        instruction.setStyle(UIStyle.instructionStyle());



        /*
         * White player
         */
        Label whiteLabel;
        if(mode == GameMode.VS_AI_FOG){
            whiteLabel = new Label("Your Name");
        }else{
            whiteLabel = new Label("White Player");
        }

        whiteLabel.setStyle(UIStyle.setupLabelStyle());



        TextField whiteName = new TextField();
        whiteName.setPromptText("Enter your name");
        whiteName.setPrefWidth(250);
        whiteName.setMaxWidth(250);
        whiteName.setStyle(UIStyle.textFieldStyle());



        /*
         * Black player
         */
        Label blackLabel = new Label("Black Player");

        blackLabel.setStyle(UIStyle.setupLabelStyle());



        TextField blackName = new TextField();
        blackName.setPromptText("Enter your name");
        blackName.setMaxWidth(250);
        blackName.setPrefWidth(250);
        blackName.setStyle(UIStyle.textFieldStyle());



        /*
         * AI difficulty
         * This is only shown when playing against the AI
         */
        Label difficultyLabel = new Label("AI Difficulty");

        difficultyLabel.setStyle(UIStyle.setupLabelStyle());



        ComboBox<String> difficultyBox = new ComboBox<>();
        difficultyBox.getItems().addAll("Easy", "Medium", "Advanced");
        difficultyBox.setValue("Easy");
        difficultyBox.setPrefWidth(250);
        difficultyBox.setStyle(UIStyle.comboBoxStyle());


        /*
         * Change the player settings depending on the selected game mode
         */
        if (mode == GameMode.VS_AI){
            blackLabel.setText("Computer");
            blackName.setText(difficultyBox.getValue() + " AI");
            blackName.setDisable(true);

            // The computer is named after its level, e.g. "Medium AI"
            difficultyBox.setOnAction(event -> blackName.setText(difficultyBox.getValue() + " AI"));

        }else if (mode == GameMode.VS_AI_FOG){
            blackLabel.setText("Computer");
            blackName.setText("Easy AI");
            blackName.setDisable(true);

            //Fog of war always uses easy AI
            difficultyBox.setVisible(false);
            difficultyBox.setManaged(false);

            difficultyLabel.setVisible(false);
            difficultyLabel.setManaged(false);
        }else{
            difficultyBox.setVisible(false);
            difficultyBox.setManaged(false);

            difficultyLabel.setVisible(false);
            difficultyLabel.setManaged(false);
        }

        /*
         * Target score
         */
        Label targetScoreLabel;

        if(mode == GameMode.VS_AI_FOG){
            targetScoreLabel = new Label("Points to Win");
        }else{
            targetScoreLabel = new Label("Play to: ");
        }

        targetScoreLabel.setStyle(UIStyle.setupLabelStyle());


        TextField targetScoreField = new TextField("3");
        targetScoreField.setMaxWidth(250);
        targetScoreField.setPrefWidth(250);
        targetScoreField.setStyle(UIStyle.textFieldStyle());


        /*
         * Navigate Buttons
         */
        Button startButton = new Button("Start Game");
        Button backButton = new Button("Back");

        UIStyle.styleButton(startButton);
        UIStyle.styleButton(backButton);

        /*
         * Arrange the players settings and buttons
         */
        VBox names = new VBox(10);
        names.getChildren().addAll(whiteLabel, whiteName, blackLabel,blackName, difficultyLabel,difficultyBox,targetScoreLabel, targetScoreField);
        names.setAlignment(Pos.CENTER);

        HBox buttons = new HBox(20);
        buttons.getChildren().addAll(startButton, backButton);
        buttons.setAlignment(Pos.CENTER);

        VBox layout = new VBox(25);
        layout.getChildren().addAll(title, instruction, names, buttons);
        layout.setAlignment(Pos.CENTER);

        layout.setBackground(getBackground(mode));

        /*
         * Start button
         * The target score is checked here before the game screen
         * is created
         */
        startButton.setOnAction(event ->{

            String whitePlayerName = whiteName.getText().trim();
            String blackPlayerName = blackName.getText().trim();

            // Both player names are required when playing with a friend.
            if (whitePlayerName.isEmpty()) {
                return;
            }

            if (mode == GameMode.FRIEND && blackPlayerName.isEmpty()) {
                return;
            }

            int targetScore;

            try {
                targetScore = Integer.parseInt(targetScoreField.getText().trim());
            }catch (NumberFormatException e){
                return;
            }

            String difficulty = difficultyBox.getValue();

            //Fog of war always uses easy AI
            if(mode == GameMode.VS_AI_FOG){
                difficulty = "Easy";
            }

            //The AI player gets the name of its level, which is shown everywhere in the game
            if (mode == GameMode.VS_AI || mode == GameMode.VS_AI_FOG){
                blackPlayerName = difficulty + " AI";
            }
            showGameScreen(stage, whitePlayerName, blackPlayerName, mode, difficulty, targetScore);
        });

        /*
         * Back button
         * To return to the main menu
         */
        backButton.setOnAction(event ->{
            start(stage);
        });

        /*
         * Displays the player setup screen
         */
        changeScreen(stage, layout, "Backgammon");

    }



    /**
     * Returns the style used for the four dice labels.
     * @return CSS style for a dice label
     */

    private static String diceLabelStyle() {
        return "-fx-font-family: 'Georgia';" +
                "-fx-font-size: 42px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #382719;" +
                "-fx-background-color: #E8D2B2;"  +
                "-fx-background-radius: 3px;" +
                "-fx-border-color: #8B6F47;" +
                "-fx-border-width: 2px;" +
                "-fx-border-radius: 3px;" +
                "-fx-padding: 5px 12px;";
    }



    /**
     * Creates a background used by the main menu and player setup screen
     * @return JavaFX background containing the game's background image
     * -JAVADOC- @param mode missing
     */
    private Background getBackground(GameMode mode){
        String backgroundPath;
        if(mode == GameMode.VS_AI_FOG){
            backgroundPath = "/gameproject/images/fog.jpeg";
        }else{
            backgroundPath = "/gameproject/images/background.jpeg";;
        }

        BackgroundImage backgroundImage = new BackgroundImage(
                new Image(getClass().getResource(
                        backgroundPath
                ).toExternalForm()),
                BackgroundRepeat.NO_REPEAT,
                BackgroundRepeat.NO_REPEAT,
                BackgroundPosition.CENTER,
                new BackgroundSize(100, 100, true, true,false, true));
        return new Background(backgroundImage);
    }


    /**
     * Main entry point of the app
     * @param args command line arguments
     */
    public static void main(String[] args){
        launch(args);
    }
}


