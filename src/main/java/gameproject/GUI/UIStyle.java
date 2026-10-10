
package gameproject.GUI;

import javafx.scene.control.Button;

/**
 * Contains shared JavaFX styles used throughout the application.
 */
public class UIStyle {

    /**
     * Returns the standard style used for application buttons.
     */
    public static String buttonStyle() {
        return "-fx-font-family: 'Georgia';" +
                "-fx-font-size: 17px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #FFF5E3;" +
                "-fx-background-color: #425B3D;" +
                "-fx-background-radius: 4px;" +
                "-fx-border-color: #30452D;" +
                "-fx-border-width: 1px;" +
                "-fx-border-radius: 4px;" +
                "-fx-padding: 10px 25px;" +
                "-fx-cursor: hand;";
    }

    /**
     * Applies the standard style and subtle hover effects to a button.
     */
    public static void styleButton(Button button) {
        button.setStyle(buttonStyle());

        button.setOnMouseEntered(event -> {
            button.setStyle(buttonStyle()
                    .replace("#425B3D", "#536F4B")
                    .replace("#30452D", "#3D5637"));
        });

        button.setOnMouseExited(event -> {
            button.setStyle(buttonStyle());
        });


        button.setOnMousePressed(event -> {
            button.setStyle(buttonStyle()
                    .replace("#425B3D", "#30452D"));
        });


        button.setOnMouseReleased(event -> {
            if (button.isHover()) {
                button.setStyle(buttonStyle()
                        .replace("#425B3D", "#536F4B")
                        .replace("#30452D", "#3D5637"));
            } else {
                button.setStyle(buttonStyle());
            }
        });
    }



    /**
     * Returns the style for the main menu title.
     */
    public static String mainTitleStyle() {
        return "-fx-font-family: 'Georgia';" +
                "-fx-font-size: 50px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #FFF5E3;";
    }

    /**
     * Returns the style for the title displayed during a game.
     */
    public static String gameTitleStyle() {
        return "-fx-font-family: 'Georgia';" +
                "-fx-font-size: 40px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #FFF5E3;";
    }


    /**
     * Returns the style for instructions displayed on a screen.
     */
    public static String instructionStyle() {
        return "-fx-font-family: 'Georgia';" +
                "-fx-font-size: 22px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #FFF5E3;";
    }

    /**
     * Returns the style for labels on the player setup screen.
     */
    public static String setupLabelStyle() {
        return "-fx-font-family: 'Georgia';" +
                "-fx-font-size: 17px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #FFF5E3;";
    }


    /**
     * Returns the style for text fields on the setup screen.
     */
    public static String textFieldStyle() {
        return "-fx-font-family: 'Georgia';" +
                "-fx-font-size: 15px;" +
                "-fx-text-fill: #382719;" +
                "-fx-prompt-text-fill: #927B63;" +
                "-fx-background-color: #FFF5E3;" +
                "-fx-background-radius: 3px;" +
                "-fx-border-color: #8B6F47;" +
                "-fx-border-width: 1.5px;" +
                "-fx-border-radius: 3px;" +
                "-fx-padding: 8px;";
    }

    /**
     * Returns the style for dropdowns on the setup screen.
     */
    public static String comboBoxStyle() {
        return "-fx-font-family: 'Georgia';" +
                "-fx-font-size: 15px;" +
                "-fx-background-color: #FFF5E3;" +
                "-fx-border-color: #8B6F47;" +
                "-fx-border-width: 1.5px;" +
                "-fx-border-radius: 3px;" +
                "-fx-background-radius: 3px;";
    }

    public static String gameInfoTitleStyle() {
        return "-fx-font-family: 'Georgia';" +
                "-fx-font-size: 22px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #FFF5E3;";
    }

    public static String gameInfoSectionStyle() {
        return "-fx-font-family: 'Georgia';" +
                "-fx-font-size: 15px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #F1D9B5;";
    }

    public static String gameInfoValueStyle() {
        return "-fx-font-family: 'Georgia';" +
                "-fx-font-size: 17px;" +
                "-fx-text-fill: #FFF5E3;";
    }

    public static String gameInfoDiceStyle() {
        return "-fx-font-family: 'Georgia';" +
                "-fx-font-size: 28px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #FFF5E3;";
    }

    public static String gameInfoPanelStyle() {
        return "-fx-background-color: #A17B52;" +
                "-fx-border-color: #4B3020;" +
                "-fx-border-width: 2px;";
    }

    public static String gameInfoLogStyle() {
        return "-fx-control-inner-background: #382719;" +
                "-fx-text-fill: #FFF5E3;" +
                "-fx-prompt-text-fill: #BDA789;" +
                "-fx-highlight-fill: #8B6F47;" +
                "-fx-highlight-text-fill: #FFF5E3;" +
                "-fx-font-family: 'Georgia';" +
                "-fx-font-size: 12px;";
    }


    public static String playerScoreStyle() {
        return "-fx-font-family: 'Georgia';" +
                "-fx-font-size: 18px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #FFF5E3;";
    }


    public static String playerNameStyle() {
        return "-fx-font-family: 'Georgia';" +
                "-fx-font-size: 20px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #FFF5E3;";
    }





}
