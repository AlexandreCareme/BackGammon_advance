package gameproject.GUI;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.control.Label;
import javafx.util.Duration;

/**
 * Handles the visual display of the dice
 */

/** -JAVADOC- */
public class DiceView {

    private final Label dice1;
    private final Label dice2;
    private final Label dice3;
    private final Label dice4;

    private final String[] diceSymbols = {"⚀", "⚁", "⚂", "⚃", "⚄", "⚅"};

    /**
     * creates the dice display
     * @param dice1 first die
     * @param dice2 second die
     * @param dice3 third die
     * @param dice4 fourth die
     */
    public DiceView(Label dice1, Label dice2, Label dice3, Label dice4){
        this.dice1 = dice1;
        this.dice2 = dice2;
        this.dice3 = dice3;
        this.dice4 = dice4;
    }

    /**
     * Resets the dice appearance
     */
    public void reset(){
        dice1.setText("-");
        dice2.setText("-");
        dice3.setText("");
        dice4.setText("");

        dice1.setOpacity(1);
        dice2.setOpacity(1);
        dice3.setOpacity(1);
        dice4.setOpacity(1);

        dice3.setVisible(false);
        dice4.setVisible(false);
    }

    /**
     * Displayes the result of the dice roll
     * @param value1 first die value
     * @param value2 second die value
     */
    public void showRoll(int value1, int value2){
        dice1.setText(getDiceSymbol(value1));
        dice2.setText(getDiceSymbol(value2));

        if (value1 == value2){
            dice3.setText(getDiceSymbol(value1));
            dice4.setText(getDiceSymbol(value1));

            dice3.setVisible(true);
            dice4.setVisible(true);
        }else{
            dice3.setVisible(false);
            dice4.setVisible(false);
        }
        dice1.setOpacity(1);
        dice2.setOpacity(1);
        dice3.setOpacity(1);
        dice4.setOpacity(1);
    }

    /**
     * Makes used die faded
     * @param remainingMoves the dice values that can still be used
     * @param value1 first die value
     * @param value2 second die value
     */

    /** -JAVADOC- */
    public void updateUsedDice(int[] remainingMoves, int value1, int value2){
        if (remainingMoves == null){
            return;
        }

        if (value1 == value2) {
            if (remainingMoves.length < 4) {
                dice1.setOpacity(0.4);
            } else {
                dice1.setOpacity(1);
            }

            if (remainingMoves.length < 3) {
                dice2.setOpacity(0.4);
            } else {
                dice2.setOpacity(1);
            }

            if (remainingMoves.length < 2) {
                dice3.setOpacity(0.4);
            } else {
                dice3.setOpacity(1);
            }

            if (remainingMoves.length < 1) {
                dice4.setOpacity(0.4);
            } else {
                dice4.setOpacity(1);
            }
        }else{
            boolean firstDieRemaining = false;
            boolean secondDieRemaining = false;

            for (int move : remainingMoves) {
                if (move == value1 && !firstDieRemaining){
                    firstDieRemaining = true;
                }else if (move == value2 && !secondDieRemaining){
                    secondDieRemaining = true;
                }
            }

            if (firstDieRemaining){
                dice1.setOpacity(1);
            }else{
                dice1.setOpacity(0.4);
            }
            if (secondDieRemaining){
                dice2.setOpacity(1);
            }else{
                dice2.setOpacity(0.4);
                }
        }
    }

    /**
     * Animates the dice and displays the actual result when finished
     * @param value1  first die value
     * @param value2 second die value
     * @param onFinished code to run after the animation
     */

    /** -JAVADOC- */
    public void rollAnimation(int value1, int value2, Runnable onFinished){

        dice3.setVisible(false);
        dice4.setVisible(false);

        reset();

        Timeline animation = new Timeline();

        for (int i = 0; i < 10; i++) {
            KeyFrame frame = new KeyFrame(Duration.millis(i * 100), e -> {
                int randomDice1 = 1 + (int) (Math.random() * 6);
                int randomDice2 = 1 + (int) (Math.random() * 6);

                dice1.setText(getDiceSymbol(randomDice1));
                dice2.setText(getDiceSymbol(randomDice2));
            });

            animation.getKeyFrames().add(frame);

        }
        animation.setOnFinished(e -> {
            showRoll(value1, value2);
            onFinished.run();
        });
        animation.play();

    }

    /**
     * Converts a die value into its visual symbol
     * @param value die value
     * @return returns a sting of the die representation
     */

    /** -JAVADOC- */
    private String getDiceSymbol(int value){
        return diceSymbols[value -1];
    }
}
