package gameproject.game.elements;

import gameproject.player.Player;

/**
 * Represents a Play that is being made by either a player or AI agent. It consists of an initial position and forward position to move to.
 * It has a PlayType which represents which certain type of play is being made. It has getter methods to obtain information about the play being made.
 * As well as being able to compare Plays to look if there are no duplicates.
 */
public final class Play {

    /**
     * The Three Types of plays that can be made
     * NORMAL: A move of a checker which starts at an initial point and moves towards another point
     * BEAR_OFF: A move from which a checker moves from an initial point towards the bear off position.
     * OFF_BAR: A move from which the checker moves from the bar to a forward point on the board.
     */
    public enum Type {
        NORMAL,
        BEAR_OFF,
        OFF_BAR
    }

    /**
     * A play consists of an initial point from which a checker moves (Except moves from the bar),
     */
    private final int initialPoint;

    /**
     * A play consists of an forward point to which a checker moves (Except bearing off moves)
     */
    private final int forwardPoint;

    /**
     * A play has a certain type
     */
    private final Type type;

    /**
     * A play is being made by a certain player (White or Black)
     */
    private final Player player;

    /**
     * The die that is used to make this play
     */
    private final int dieUsed;

    /**
     * A play is initiated by setting the starting and end point of a checker as well as the type and player who
     * has made the play.
     * Note: Used for normal board moves as they have an initial and forward point that is being used.
     *
     * @param initialPoint starting point
     * @param forwardPoint new point the checker ends up at
     * @param type type of play that is being made
     * @param player player who makes the play
     * @param dieUsed the value of the die used to make the play
     */
    public Play(int initialPoint, int forwardPoint, Type type, Player player, int dieUsed) {
        this.initialPoint = initialPoint;
        this.forwardPoint = forwardPoint;
        this.type = type;
        this.player = player;
        this.dieUsed = dieUsed;
    }

    /**
     * A play is initiated by setting the target point which is used as well as the type and player who has made
     * the play.
     * Note: Used for Bear off and off bar moves as they only have one point which is being used.
     *
     * @param targetPoint point used for the play
     * @param type type of play that is being made
     * @param player player who makes the play
     * @param dieUsed the value of the die used to make the play
     */
    public Play(int targetPoint, Type type, Player player, int dieUsed) {
        this.type = type;
        this.player = player;
        this.dieUsed = dieUsed;

        if (type == Type.BEAR_OFF) {
            this.initialPoint = targetPoint;
            this.forwardPoint = -1;

        } else if (type == Type.OFF_BAR) {
            this.forwardPoint = targetPoint;
            this.initialPoint = -1;

        } else {
            throw new RuntimeException("targetPoint not correctly identified");
        }
    }

    /**
     * Gives the point on the board from which a checker moves
     * Note: Value equals -1 if it's off bar move.
     *
     * @return the starting(initial) point of the checker played
     */
    public int getInitialPoint() {
        return initialPoint;
    }

    /**
     * Gives the point on the board towards a checker moves
     * Note: Value equals -1 if it's a Bear off move.
     *
     * @return the new(forword) point of the checker played
     */
    public int getForwardPoint() {
        return forwardPoint;
    }

    /**
     * Gives the type of play that is being made
     *
     * @return the type of play
     */
    public Type getType() {
        return type;
    }

    /**
     * Gives the current player that makes the play
     *
     * @return the player which made the play
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * Gives the current player color  that makes the play
     *
     * @return the player color which made the play
     */
    public Player.Color getColor(){
        return player.getColor();
    }

    /**
     * Gives the value of the die that was used to make this play.
     *
     * @return the die value used
     */
    public int getDieUsed() {
        if(dieUsed== -1){
            throw new IllegalStateException(
                    "Play Object doesn't come from legalPlays but was constructed in App without a die value associated");
        }
        return this.dieUsed;
    }

    /**
     * Allows to check for equal Plays
     *
     * @param obj the reference object with which to compare.
     * @return if the two objects are equal
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof Play)) {
            return false;
        }

        Play other = (Play) obj;

        return initialPoint == other.initialPoint && forwardPoint == other.forwardPoint && type == other.type && player == other.player && dieUsed == other.dieUsed;
    }
    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();

        sb.append(this.type)
                .append("by: ")
                .append(player.getColor())
                .append(":")
                .append(initialPoint)
                .append("-->")
                .append(forwardPoint)
                .append("| die used: ")
                .append(dieUsed);

        return sb.toString();
    }
}