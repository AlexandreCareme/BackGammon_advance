package gameproject.game.elements;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Class to model two simultaneous dice throws. It stores the result and provides methods to throw the dice again,
 * to get the value of the two faces and a method to check if a double was thrown.
 */
public class Dice {

    private int dice1;
    private int dice2;
    private Random random;


    /**
     * Creates a new dice object with two die without them being thrown
     */
    public Dice() {
        this.random = new Random();
    }

    /** creates a new dice with a set face and Random seed for deterministically testing*/
    public Dice(int dice1, int dice2, Random random) {
        this.dice1 = dice1;
        this.dice2 = dice2;
        this.random = random;
    }

    /**
     * Creates a new dice object with two specified values for testing
     * -JAVADOC- @param dice1 missing; @param dice2 missing
     */
    public Dice(int dice1, int dice2){
        this(dice1,dice2,new Random());
    }


    /**
     * Creates a new dice object with a specified Random object for testing
     * -JAVADOC- @param random missing
     */
    public Dice(Random random){
        this.random = random;
    }

    /**
     * Method to throw the dice
     */
    public void throwDice() {
        this.dice1 = random.nextInt(6)+1;
        this.dice2 = random.nextInt(6)+1;
    }

    /**
     * @return face value of die 1
     */
    public int getDice1(){
        return this.dice1;
    }

    /**
     * @return face value of die 2
     */
    public int getDice2() {
        return this.dice2;
    }

    /**
     * @return true if faces show double and false else
     */
    public boolean isDouble() {
        return this.dice1 == this.dice2;
    }

    /**
     * Represents a throw with two die values and stores the probability of that throw
     * Used in the Tree algorithm in evaluate-node
     *
     * @param die1 first die thrown
     * @param die2 second die thrown
     * @param probability the corresponding probability of throwing die1 and die2
     */
    public record DiceOutcome(int die1, int die2, double probability) {
    }

    /** -JAVADOC- */
    public static List<DiceOutcome> getDiceOutcomes() {
        List<DiceOutcome> outcomes = new ArrayList<>();

        // All possible die combinations
        for (int die1 = 1; die1 <= 6; die1++) {
            for (int die2 = die1; die2 <= 6; die2++) {

                // Assign probability based on doubles or non-doubles
                double probability;
                if (die1 == die2) {
                    probability = 1.0 / 36.0;
                }
                else {
                    probability = 2.0 / 36.0;
                }

                // Add the possible outcome to the final list
                outcomes.add(new DiceOutcome(die1, die2, probability));
            }
        }
        return outcomes;
    }
}