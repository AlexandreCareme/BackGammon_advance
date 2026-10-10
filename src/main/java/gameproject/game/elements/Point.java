package gameproject.game.elements;

import gameproject.player.Player;

/**
 * Class to model one point(one of the 24 triangle shaped position a checker can be at) on a backgammon board.
 * It provides methods to add and remove a checker as well as methods to check if the point is occupied by a plyer with 2
 * or more checkers and a method to get the number of checkers at this point.
 */
public class Point extends Location {

    /**
     * The position of the point
     */
    private final int position;
    /**
     * The minimum numbers off checkers needed to block that point. By default, 2
     */
    private final int blockNumber = 2;

    private final int blotNumber = 1;

    /**
     * Creates a new point with a given position and no checkers of any color
     * @param position of the point
     */
    public Point(int position) {
        super();
        this.position = position;

    }

    /**
     * Creates a new Point object identical to the one that is being passed.
     *
     * @param point the point to be copied
     */
    public Point(Point point) {
        super(point);
        this.position = point.getPosition();

    }

    /**
     *Adds a checker of the given color to the point and throws illegal state when point blocked
     * @param color
     * -JAVADOC- @param color empty
     */
    @Override
    public void addChecker(Player.Color color){
        if(this.isBlocked(color.opponent())){

            throw new IllegalStateException(
                    "Cannot add"+ color.name() + "checker: spot is blocked by other color"
            );
        }
        super.addChecker(color);
    }
    @Override
    public void addCheckers(Player.Color color, int numberToAdd){
        for (int i = 0; i < numberToAdd; i++) {
            this.addChecker(color);
        }
    }

    /**
     *
     * @param color
     * @return true if given color occupies this point with {@code this.blockNumber} or more checkers
     * -JAVADOC- @param color empty
     */
    public boolean isBlocked(Player.Color color){

        return super.getCheckers(color) >= this.blockNumber;
    }

    /** -JAVADOC- */
    public boolean isBlot(Player.Color color){
        return super.getCheckers(color) == this.blotNumber;
    }

    /**
     * @return true if white occupies this point with 2 or more checkers
     */
    public boolean isBlockedWhite() {
        return super.getCheckers(Player.Color.WHITE) >= this.blockNumber;
    }
    /**
     * @return true if black occupies this point with 2 or more checkers
     */
    public boolean isBlockedBlack() {
        return super.getCheckers(Player.Color.BLACK) >= this.blockNumber;
    }

    /**
     * @return position assigned to this point
     */
    public int getPosition() {
        return position;
    }
}