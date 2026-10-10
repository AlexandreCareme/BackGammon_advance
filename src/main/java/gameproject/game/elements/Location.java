package gameproject.game.elements;

import gameproject.player.Player;

/**
 * Abstract class to model a position for checkers, bar,offboard and position extend this
 */
public abstract class Location {

    private final int max;
    private final int min;
    private int[] amountWhiteBlack; // {white, black}

    /** -JAVADOC- */
    public Location(){
        this.max = Board.checkersEach;
        this.min = 0;
        this.amountWhiteBlack = new int[]{0,0}; // white = 0, black = 0
    }

    /** -JAVADOC- */
    public Location(Location other) {
        this();

        this.amountWhiteBlack[0] = other.getCheckers(Player.Color.WHITE);
        this.amountWhiteBlack[1] = other.getCheckers(Player.Color.BLACK);

    }

    /**
     * adds a checker of the given color to this location
     * @param color
     * -JAVADOC- @param color empty
     */
    public void addChecker(Player.Color color) {

        switch (color) {
            case WHITE -> this.addCheckerWhite();
            case BLACK -> this.addCheckerBlack();
        }

    }

    /** -JAVADOC- */
    public void addCheckers(Player.Color color, int numberToAdd){
        for (int i = 0; i < numberToAdd; i++) {
            this.addChecker(color);
        }
    }

    /**
     * adds a white checker to the location
     */
    private void addCheckerWhite() {
        if (isFull(Player.Color.WHITE)) {
            throw new IllegalStateException(
                    "Cannot add white checker: more than total checkers"
            );
        }
        this.amountWhiteBlack[0] += 1;
    }

    /**
     * adds a black checker to this location
     */
    private void addCheckerBlack() {
        if (isFull(Player.Color.BLACK)) {
            throw new IllegalStateException(
                    "Cannot add black checker: more than total checkers"
            );
        }
        this.amountWhiteBlack[1] += 1;
    }

    /**
     * removes 1 checker of the given color at this location
     * @param color
     * -JAVADOC- @param color empty
     */
    public void removeChecker(Player.Color color ){

        switch (color) {
            case WHITE -> this.removeCheckerWhite();
            case BLACK -> this.removeCheckerBlack();
        }
    }

    /**
     * removes a white checker off this location
     */
    private void removeCheckerWhite() {
        if(this.isEmpty(Player.Color.WHITE)){
            throw new IllegalStateException(
                    "Cannot remove white checker: no white checker at this point"
            );
        }
        this.amountWhiteBlack[0] -= 1;
    }

    /**
     * removes a black checker off this location
     */
    private void removeCheckerBlack() {
        if(this.isEmpty(Player.Color.BLACK)){
            throw new IllegalStateException(
                    "Cannot remove black checker: no black checker at this point"
            );
        }
        this.amountWhiteBlack[1] -= 1;
    }

    /**
     * used in Advanced Agent to undo move, Do not use it in game mechanic,
     * this is only for efficiently searching board states
     *
     * @param white number of white checkers to be set to
     * @param black number of black checkers to be set to
     */
    public void restoreCounts(int white, int black){
        this.amountWhiteBlack[0] = white;
        this.amountWhiteBlack[1] = black;

    }

    /**
     * @param color
     * @return number of checkers of given color
     * -JAVADOC- @param color empty
     */
    public int getCheckers(Player.Color color){

        return switch (color) {
            case WHITE -> this.getCheckersWhite();
            case BLACK -> this.getCheckersBlack();
        };
    }

    /**
     * @return number of white checkers at this location
     */
    private int getCheckersWhite(){
        return this.amountWhiteBlack[0];
    }

    /**
     * @return number of black checkers at this location
     */
    private int getCheckersBlack(){
        return this.amountWhiteBlack[1];
    }

    /**
     * @param color
     * @return true if max number of checkers of given color at this location
     * -JAVADOC- @param color empty
     */
    public boolean isFull(Player.Color color) {
        return switch (color){
            case WHITE ->getCheckers(Player.Color.WHITE) == this.max;
            case BLACK ->getCheckers(Player.Color.BLACK) == this.max;
        };
    }
    /**
     * @param color
     * @return true if no checkers of given color at this location
     * -JAVADOC- @param color empty
     */
    public boolean isEmpty(Player.Color color){
        return switch (color){
            case WHITE -> getCheckers(Player.Color.WHITE) == this.min;
            case BLACK -> getCheckers(Player.Color.BLACK) == this.min;
        };
    }
    /** -JAVADOC- */
    public boolean hasChecker(Player.Color color){
        return !isEmpty(color);
    }
    /**
     * @return a string representation of a Location with the number of checkers displayed as
     * [white,black]
     */
    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();

        sb.append("[")
                .append(this.getCheckers(Player.Color.WHITE))
                .append(",")
                .append(this.getCheckers(Player.Color.BLACK))
                .append("]");

        return sb.toString();
    }

}
