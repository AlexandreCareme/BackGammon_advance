package gameproject.game.elements;

/**
 * Class to model the off board, that is the position where checkers are placed once they are borne off,
 * this includes methods to add white or black checkers, check if the game is won and a getter method to get the number
 * of white or black checkers borne off.
 */
public class OffBoard extends Location {

    /**
     * Creates a new off board with the standard starting state off no checkers of any color borne off
     */
    public OffBoard() {
        super();
    }

    /**
     * Creates a new off board identical to the one being passed.
     *
     * @param other the OffBoard position to be copied
     */
    public OffBoard(OffBoard other) {
        super(other);
    }

}
