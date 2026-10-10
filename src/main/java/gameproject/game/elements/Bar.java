package gameproject.game.elements;

/**
 * Class that models the bar of a backgammon board with the amount of white's and black's checkers on it stored.
 * Method to add and remove checkers for both players and a Method to get number of checker for each and to check if it is empty
 */

/** -JAVADOC- */
public class Bar extends Location {

    /**
     * Sets up the bar with the standard starting state with no checkers on it
     */
    public Bar() {
        super();
    }
    /**
     * Creates an independent copy of the bar
     * @param other the bar to copy
     */
    public Bar(Bar other) {
        super(other);
    }

}