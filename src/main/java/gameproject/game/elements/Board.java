package gameproject.game.elements;
import gameproject.player.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Class that models one snapshot of a board, storing how many checkers are stored in what position
 * for each of the players. It also provides methods to set up the board in the standard starting position
 * and placing checkers, getter methods for the points, bar and offboard. A toString method is also provided.
 * It is able to create a new Board object after a certain play has being made to create a new snapshot of the board.
 */
public class Board {

    /**
     * The points on the board (24 total)
     */
    private List<Point> points;

    /**
     * The location where checkers are stored after being hit off.
     */
    private Bar bar;
    /**
     * The location where checkers are stored after being born off.
     */
    private OffBoard offBoard;

    public static int checkersEach = Round.totalNumberCheckersEach;

    /**
     * Creates a new empty Board object with the correct amount of points, bar and offBoard positions
     */
    public Board() {
        points = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            points.add(new Point(i));
        }
        this.bar = new Bar();
        this.offBoard = new OffBoard();
        this.setupBoard();
    }

    /**
     * Creates an independent copy of the current board
     * using a board as input
     *
     * @param board the board to be copied
     */
    public Board(Board board) {
        this.bar = new Bar(board.bar);
        this.offBoard = new OffBoard(board.offBoard);

        this.points = new ArrayList<>();
        for (Point point : board.getPoints()) {
            this.points.add(new Point(point));
        }
    }

    /**
     * Creates a new Board object after a play has been made
     * using a round and a play
     *
     * @param round the initial round
     * @param play the play to be made
     */
    public Board(Round round, Play play){
        this(round.getCurrentBoard());
        makePlay(play);
    }

    /**
     * Creates a new Board object after a play has been made
     *using a board and a play instead
     *
     * @param board the initial board
     * @param play the play to be made
     */
    public Board(Board board, Play play){
        this(board);
        makePlay(play);
    }

    /**
     * Checks which type of play has to be made and calls the correct method to make that certain type of play.
     * Types are NORMAL, BEAR_OFF, OFF_BAR
     *
     * @param play the play that needs to be made
     */
    public void makePlay(Play play) {

        switch(play.getType()){
            case NORMAL -> makeNormalPlay(play);
            case BEAR_OFF -> makeBearOffPlay(play);
            case OFF_BAR -> makeOffBarPlay(play);
        }

        assertLegalBoardState(play.getColor());
    }

    /**
     * Checks if the play made ends the board in a legal board state that is with 15 checkers each
     * @param color to determine who is making the play
     */
    private void assertLegalBoardState(Player.Color color){


        int totalCheckers = this.bar.getCheckers(color) +
                this.offBoard.getCheckers(color);

        for(Point point : points){
            totalCheckers+= point.getCheckers(color);
        }

        if(totalCheckers!=checkersEach){
            throw new IllegalStateException("illegal board state:Total Checkers of"
                    + color.toString()+ " not equal " + checkersEach);
        }
    }
    /**
     * Updates the current board with the play that is made
     * The play is a normal move on the board from one point to another point.
     *
     * @param play the play needed to be made
     */
    private void makeNormalPlay(Play play) {

        requireValidPlay(play);

        Player.Color movingColor = play.getColor();
        Player.Color opponentColor = play.getColor().opponent();

        Point source = points.get(play.getInitialPoint());
        Point target = points.get(play.getForwardPoint());

        assertPlayIsLegal(source, target, movingColor);

        hitBlotIfPresent(target, opponentColor);
        source.removeChecker(movingColor);
        target.addChecker(movingColor);
    }

    /**
     * Updates the current board with the play that is made
     * The play is a bear off move. From a point to the bear off position
     *
     * @param play the play needed to be made
     */
    private void makeBearOffPlay(Play play) {

        requireValidPlay(play);

        Player.Color movingColor = play.getColor();
        Player.Color opponentColor = play.getColor().opponent();

        Point source = points.get(play.getInitialPoint());
        Location target = offBoard;

        assertPlayIsLegal(source, target, movingColor);

        source.removeChecker(movingColor);
        target.addChecker(movingColor);
    }

    /**
     * Updates the current board with the play that is made
     * The play is off the bar. From the bar to a point.
     *
     * @param play the play needed to be made
     */
    private void makeOffBarPlay(Play play) {

        requireValidPlay(play);

        Player.Color movingColor = play.getColor();
        Player.Color opponentColor = play.getColor().opponent();

        Bar source = this.bar;
        Point target = points.get(play.getForwardPoint());

        assertPlayIsLegal(source, target, movingColor);

        hitBlotIfPresent(target,opponentColor);
        bar.removeChecker(movingColor);
        target.addChecker(movingColor);

    }

    /**
     * helper method to check illegel argument if play or player null
     * @param play
     * -JAVADOC- @param play empty
     */
    private void requireValidPlay(Play play){
        if (play == null || play.getPlayer() == null) {
            throw new IllegalArgumentException("Play and player must not be null");
        }
    }

    /**
     * checking whether play is legal
     * @param source
     * @param target
     * @param movingColor
     * -JAVADOC- @param source empty
     */
    private void assertPlayIsLegal(Location source,
                                   Location target,
                                   Player.Color movingColor){
        if (source.isEmpty(movingColor)) {
            throw new IllegalStateException("Source has no checker of the moving color");
        }

        if (target instanceof Point point && point.isBlocked(movingColor.opponent())) {
            throw new IllegalStateException("Destination is blocked");
        }
    }

    /**
     * Hits a blot only if its present( does not add moving players checker!!
     * @param target
     * @param opponentColor
     * -JAVADOC- @param target empty; @param opponentColor empty
     */
    private void hitBlotIfPresent(Point target, Player.Color opponentColor){
        if (target.isBlot(opponentColor)){
            target.removeChecker(opponentColor);
            bar.addChecker(opponentColor);
        }
    }

    /**
     * Method to set up the board in the standard starting position.
     */
    private void setupBoard() {
        //Placing player white checkers
        Player.Color white = Player.Color.WHITE;
        Player.Color black = Player.Color.BLACK;

        placeCheckers(23, 2, white);  // point 24
        placeCheckers(12, 5, white);  // point 13
        placeCheckers(7, 3, white);   // point 8
        placeCheckers(5, 5, white);   // point 6

        //Placing player black checkers
        placeCheckers(0, 2, black);   // point 1
        placeCheckers(11, 5, black);  // point 12
        placeCheckers(16, 3, black);  // point 17
        placeCheckers(18, 5, black);  // point 19
    }

    /**
     * Method to place checkers on a board. Used for setting up the board quicker.
     *
     * @param index where checkers go
     * @param count how many checkers to place
     * @param color what player checkers to add
     */
    private void placeCheckers(int index, int count, Player.Color color) {
        points.get(index).addCheckers(color, count);

    }


    /**
     * Gives the location of checkers which are on the bar
     *
     * @return bar object
     */
    public Bar getBar() {
        return bar;
    }

    /**
     * Gives the location of checkers which are born-off
     *
     * @return offboard object
     */
    public OffBoard getOffBoard() {
        return offBoard;
    }

    /**
     * Gives the list of current points on the board
     *
     * @return List of points
     */
    public List<Point> getPoints() {
        return points;
    }

    /**
     * Prints an overview of the current board
     *
     * @return  representation of board
     */
    @Override
    public String toString(){

        StringBuilder sb = new StringBuilder();
        //Top row

        for (int i = 12; i < 18; i++) {
            sb.append(points.get(i).toString()).append(" ");
        }

        sb.append("|| ");
        for (int i = 18; i < 24; i++) {
            sb.append(points.get(i).toString()).append(" ");
        }

        sb.append("| Black ");
        sb.append(System.lineSeparator());

        //Bottom row
        for (int i = 11; i >= 6; i--) {
            sb.append(points.get(i).toString()).append(" ");
        }

        sb.append("|| ");

        for (int i = 5; i >= 0; i--) {
            sb.append(points.get(i).toString()).append(" ");
        }
        sb.append("| White ");
        sb.append(System.lineSeparator());
        sb.append("Bar: ").append(this.bar.toString()).append(" ");
        sb.append("  OffBoard: ").append(this.offBoard.toString());
        sb.append(" [White,Black]");


        return sb.toString();
    }

    /**
     * Returns all checker counts as one array: 2 per point (white, black), then bar and off-board.
     *
     * @return a new array of 52 counts
     */
    public int[] getCheckerCounts() {
        int[] counts = new int[52];                 // 24 points × 2 colours + bar × 2 + off × 2
        for (int i = 0; i < 24; i++) {
            counts[2 * i]     = points.get(i).getCheckers(Player.Color.WHITE);
            counts[2 * i + 1] = points.get(i).getCheckers(Player.Color.BLACK);
        }
        counts[48] = bar.getCheckers(Player.Color.WHITE);
        counts[49] = bar.getCheckers(Player.Color.BLACK);
        counts[50] = offBoard.getCheckers(Player.Color.WHITE);
        counts[51] = offBoard.getCheckers(Player.Color.BLACK);
        return counts;
    }
}