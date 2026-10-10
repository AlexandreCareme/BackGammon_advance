package gameproject.GUI;

import javafx.geometry.Pos;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.geometry.Insets;

/**
 * Displays the tutorial screen explaining the basic rules of Backgammon
 * The tutorial contains info about moving checkers, hitting, the bar, bearing off,
 * doubling and match scoring. It also provides buttons for returning to the main menu
 * and watching an AI demonstration.
 */
public class TutorialView {

    /**
     * creates the tutorial screen
     * @param backAction action to perform when the Back button is pressed
     * @param demoAction action to perform when the watch demo button is pressed
     * @returnlayout containing the tutorial
     */
    public VBox getLayout(Runnable backAction, Runnable demoAction) {

        /*
         * Title
         */
        Label title = new Label("How to play Backgammon");
        title.setAlignment(Pos.CENTER);
        title.setMaxWidth(Double.MAX_VALUE);
        title.setStyle("-fx-font-size: 35px;" +
                "-fx-font-weight: bold;");

        /*
         * Goal
         */
        Label goal = new Label(
                "Goal\n" +
                "Move all 15 of your checkers around the board, bring them into your home board, and bear them off before your opponent!");

        goal.setWrapText(true);
        goal.setStyle("-fx-font-size: 18px;");

        /*
         * Moving
         */
        Label moving = new Label("Moving\n" +
                "Roll two dice. Each dice can be used for one move. You can use the dice on different checkers or on the same checker. If you roll doubles, you get four moves." );

        moving.setWrapText(true);
        moving.setStyle("-fx-font-size: 18px;");

        /*
         * Hitting
         */
        Label hitting = new Label("Hitting\n" +
                "If you land on a point containing one opponent checker, that checker is hit and moved to the bar.");

        hitting.setWrapText(true);
        hitting.setStyle("-fx-font-size: 18px;");

        /*
         * The bar
         */
        Label bar = new Label("The Bar\n" +
                "If one of your checkers is hit, it is placed on the bar. You must move a checker from the bar back onto the board before you can make any other moves. ");
        bar.setWrapText(true);
        bar.setStyle("-fx-font-size: 18px;");

        /*
         * Bearing-off
         */
        Label bearingOff = new Label("Bearing Off\n" +
                "Once all your checkers are in your home board, you can start bearing them off the board.");
        bearingOff.setWrapText(true);
        bearingOff.setStyle("-fx-font-size: 18px;");

        /*
         * Doubling
         */
        Label doubling = new Label("Doubling\n" +
                "You can offer a double during the game. Your opponent can accept or decline it. Declining will give the player who offered the double the points.");
        doubling.setWrapText(true);
        doubling.setStyle("-fx-font-size: 18px;");


        /*
         * Match Score
         */
        Label match = new Label("Match Score\n" +
                "Before starting, choose the target score for the match. Games won add points to the match score. The first player to reach the target score wins the match.");
        match.setWrapText(true);
        match.setStyle("-fx-font-size: 18px;");

        /*
         * Fog Of War
         */
        Label fog = new Label("Fog Of War\n" +
                "You can see your checkers. You can see the dice.\n" +
                "But your opponent's checkers? Not so much. \n" +
                "Trust your strategy... and question your eyesight.\n" +
                "       Hint: Not everything on the main menu is meant to be visible");
        fog.setWrapText(true);
        fog.setStyle("-fx-font-size: 18px;");


        /*
         * Tutorial Content
         */
        VBox content = new VBox(20, title, goal, moving, hitting, bar, bearingOff, doubling, match, fog);


        content.setAlignment(Pos.TOP_LEFT);
        content.setPadding(new javafx.geometry.Insets(30));
        content.setMaxWidth(750);

        /*
         * Scroll pane
         */
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefSize(800, 650);
        scrollPane.setStyle("-fx-background: #F3E3C3; " +
                "-fx-background-color: #F3E3C3;");

        /*
         * Buttons
         */
        Button backButton = new Button("Back");
        backButton.setStyle(UIStyle.buttonStyle());
        backButton.setOnAction(event -> {backAction.run();});

        Button demoButton = new Button("Watch Demo");
        demoButton.setStyle(UIStyle.buttonStyle());
        demoButton.setOnAction(event -> {demoAction.run();});


        HBox buttons = new HBox(20, demoButton, backButton);
        buttons.setAlignment(Pos.CENTER);

        /*
         * Main Layout
         */
        VBox layout = new VBox(15, scrollPane, buttons);
        layout.setAlignment(Pos.CENTER);
        layout.setBackground(new Background(new BackgroundFill(Color.web("#F3E3C3"),
                CornerRadii.EMPTY,
                Insets.EMPTY)));

        return layout;

    }
}
