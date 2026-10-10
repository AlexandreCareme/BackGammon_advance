package gameproject.game.elements;

import gameproject.player.Player;
import gameproject.player.Strategy;

import java.util.ArrayList;
import java.util.List;

/**
 * This class represents one round in a game of backgammon
 *
 * @author Kilian Littan
 */
public class Round {

    /**
     * total number of checkers for each player
     */
    public static final int totalNumberCheckersEach = 15;

    /**
     * the number of the current move
     */
    private int turnNumber;

    /**
     * Indicating if the round is still active, that is no player has won or conceded
     */
    private boolean active;

    /**
     * player that is playing with the black checkers
     */
    private Player black;

    /**
     * player that is playing with the white checkers
     */
    private Player white;

    /**
     * player whos turn it is
     */
    private Player currentPlayer;

    /**
     * The doubling factor in play
     */
    private int doublingFactor;
    private Player doublingCubeOwner;
    private Player doubleOfferedBy;
    private boolean doubleOffered;
    private Player winner;

    /**
     * The value for a certain type of win
     */
    private int winTypeValue;

    /**
     * Represents the dice being used in the game
     */
    private Dice roundDice;

    /**
     * The current snapshot of the board in play
     */
    private Board currentBoard;

    /**
     * moves still playable in the round
     */
    private int[] moves;

    /**
     * number of points required to win the match
     */
    private int targetScore = 1;


    /**
     * This constructor stores the players that play the round and assigns them a color, the first being white and
     * the second black.
     * It creates the board with a standard starting position and dice objects used.
     * The move number is set to 0 and the rounds status to active.
     * The doubling factor and value of the win type is indicated as 1.
     *
     * @param playerWhite player that gets white checkers
     * @param playerBlack player that gets black checkers
     * -JAVADOC- @param dice missing
     */
    public Round(Player playerWhite, Player playerBlack, Dice dice) {
        this.turnNumber = 0;
        this.active = true;
        this.white = playerWhite;
        this.black = playerBlack;
        this.doublingFactor = 1;
        this.doublingCubeOwner = null;
        this.doubleOfferedBy = null;
        this.doubleOffered = false;
        this.winner = null;
        this.winTypeValue = 1;
        this.roundDice = dice;
        this.currentBoard = new Board();
        this.currentPlayer = null;
    }

    /** -JAVADOC- */
    public Round(Player playerWhite, Player playerBlack){
        this(playerWhite, playerBlack, new Dice());
    }

    /**
     * Creates an independent copy of a round.
     * Used for the AI implementation to obtain all possible moves in a turn
     *
     * @param round the round to copy
     */
    public Round(Round round) {
        // References which can be kept
        this.turnNumber = round.turnNumber;
        this.active = round.active;
        this.white = round.white;
        this.black = round.black;
        this.currentPlayer = round.currentPlayer;
        this.doublingFactor = round.doublingFactor;
        this.doublingCubeOwner = round.doublingCubeOwner;
        this.doubleOfferedBy = round.doubleOfferedBy;
        this.doubleOffered = round.doubleOffered;
        this.winner = round.winner;
        this.winTypeValue = round.winTypeValue;
        this.targetScore = round.targetScore;

        // New objects which require new references
        this.roundDice = new Dice(round.roundDice.getDice1(), round.roundDice.getDice2());
        this.currentBoard = new Board(round.currentBoard);
        if (round.moves == null) {
            this.moves = null;
        } else {
            this.moves = round.moves.clone();
        }
    }

    /**
     * Another round constructor taking new dice as input.
     * Used in advancedAI agent for simulating turns.
     *
     * @param round the round to be copied
     * @param dice the new dice to be added
     */
    public Round(Round round, Dice dice) {
        this(round);

        // Update dice and moves
        this.roundDice = dice;
        this.moves = allMoves(dice);
    }

    // VOID METHODS

    /**
     * Method which starts the round and runs till one of the players has won.
     */
    public void startRound() {
        determineFirstPlayer();

        while (active) {
            startTurn();
            playTurn();

            if (checkWinConditions()) {
                endRound();
            } else {
                endTurn();
            }
        }
    }


    /**
     * Method which determines the first player off the round.
     */
    public void determineFirstPlayer() {

        do {
            this.roundDice.throwDice();
        } while (this.roundDice.isDouble());

        if (this.roundDice.getDice1() > this.roundDice.getDice2()) {
            this.currentPlayer = this.white;
        } else {
            this.currentPlayer = this.black;
        }
    }

    /**
     * Sets the target score
     * @param targetScore total number of points required to win the match
     */
    public void setTargetScore(int targetScore) {
        this.targetScore = targetScore;
    }


    /**
     * Method which starts every turn by increasing the turn number and throwing the dice as well.
     */
    public void startTurn() {

        if(turnNumber!= 0){
            roundDice.throwDice();
        }

        turnNumber++;
        moves = allMoves(roundDice);
    }


    /**
     * Method at the end of each turn which clears the moves that can be made and switches the player
     */
    public void endTurn() {
        moves = new int[0];
        switchPlayer();

    }


    /**
     * Method at the end of the round which indicates the round is done
     */
    public void endRound() {
        active = false;
    }


    /**
     * Method which plays a whole turn of one player.
     * Only plays that satisfy the mandatory dice rules can be chosen.
     */
    private void playTurn() {
        while (moves.length != 0) {

            List<Play> legalPlays = getRequiredPlays(moves);

            if (legalPlays.isEmpty()) {
                break;
            }

            Play play = choosePlay(legalPlays).getFirst();

            executePlay(play);
        }
    }


    /**
     * Method which switches the current player.
     */
    public void switchPlayer() {
        if (currentPlayer == white) {
            currentPlayer = black;
        } else if (currentPlayer == black) {
            currentPlayer = white;
        } else {
            throw new RuntimeException("Issue with switching players ");
        }
    }


    // Objects

    /**
     * Returns all eyes that can be used. In case of doubles the number of eyes is times 4.
     * In case of non-double dies we have 2 moves.
     *
     * @param inputDice the current dice
     * @return the moves that can be made
     */
    public int[] allMoves(Dice inputDice) {

        int face1 = inputDice.getDice1();
        int face2 = inputDice.getDice2();

        if (face1 == face2) {
            return new int[]{face1, face1, face1, face1};
        } else {
            return new int[]{face1, face2};
        }
    }

    /**
     * Returns the target score
     * @return the number of points required to win the match
     */
    public int getTargetScore() {
        return targetScore;
    }


    /**
     * Checks all possible plays from a certain position and number of moves left.
     * Checks if the play that needs to be made is from the bar or a bear-off move or a normal board move.
     *
     * If the current player has a checker on the bar, only moves from the bar are allowed.
     * If bearing off is possible, both normal moves inside the home board and bearing-off moves can be made.
     *
     * @param moves remaining moves that can be used
     * @return list of possible plays that can be made
     */
    public List<Play> possiblePlays(int[] moves) {
        List<Play> possible = new ArrayList<>();

        if (moves.length == 0) {
            return possible;
        }

        for (int die : moves) {

            if (barOccupied()) {
                possible.addAll(moveFromBar(die));

            } else {
                possible.addAll(moveBoard(die));

                if (checkBearOff()) {
                    possible.addAll(moveBearOff(die));
                }
            }
        }

        return removeDuplicates(possible);
    }


    /**
     * Determines which of the possible plays are actually allowed according to the dice rules.
     * The player has to use the maximum number of dice possible.
     * If only one of two different dice can be used, the higher die must be used when possible.
     *
     * @param moves remaining moves that can be used
     * @return list of plays which may legally be chosen
     */
    public List<Play> getRequiredPlays(int[] moves) {

        List<Play> legalPlays = possiblePlays(moves);
        List<Play> requiredPlays = new ArrayList<>();

        if (legalPlays.isEmpty()) {
            return requiredPlays;
        }

        Board originalBoard = currentBoard;
        int maxMoves = 0;

        for (Play play : legalPlays) {

            // Simulate the first play
            currentBoard = new Board(this, play);

            // Remove the die used by this play
            int[] remainingMoves = removeMove(moves, play.getDieUsed());

            // Count this play and the maximum amount of plays still possible
            int totalMoves = 1 + maxMovesPossible(remainingMoves);

            // Restore the original board
            currentBoard = originalBoard;

            if (totalMoves > maxMoves) {
                maxMoves = totalMoves;
                requiredPlays.clear();
                requiredPlays.add(play);

            } else if (totalMoves == maxMoves) {
                requiredPlays.add(play);
            }
        }

        /*
         * If only one of two different dice can be used,
         * the higher die must be used if possible.
         */
        if (maxMoves == 1 && moves.length == 2 && moves[0] != moves[1]) {

            int higherDie = Math.max(moves[0], moves[1]);
            List<Play> higherDiePlays = new ArrayList<>();

            for (Play play : requiredPlays) {
                if (play.getDieUsed() == higherDie) {
                    higherDiePlays.add(play);
                }
            }

            if (!higherDiePlays.isEmpty()) {
                return higherDiePlays;
            }
        }

        return requiredPlays;
    }


    /**
     * Recursively calculates the maximum number of the remaining dice which can still be played.
     * This is used to ensure that a player always uses as many dice as possible.
     *
     * @param remainingMoves dice values which have not yet been used
     * @return maximum number of moves that can still be made
     */
    private int maxMovesPossible(int[] remainingMoves) {

        if (remainingMoves.length == 0) {
            return 0;
        }

        List<Play> legalPlays = possiblePlays(remainingMoves);

        if (legalPlays.isEmpty()) {
            return 0;
        }

        Board originalBoard = currentBoard;
        int max = 0;

        for (Play play : legalPlays) {

            // Simulate the play
            currentBoard = new Board(this, play);

            // Remove the die used in the simulated play
            int[] nextMoves = removeMove(remainingMoves, play.getDieUsed());

            int total = 1 + maxMovesPossible(nextMoves);

            // Restore the board for the next possible play
            currentBoard = originalBoard;

            if (total > max) {
                max = total;
            }
        }

        return max;
    }


    /**
     * Checks the list of plays and filters duplicates
     *
     * @param possible the list of plays possibly containing duplicates
     * @return a list of plays which do not contain duplicates
     */
    public List<Play> removeDuplicates(List<Play> possible) {
        List<Play> unique = new ArrayList<>();

        for (Play play : possible) {
            if (!unique.contains(play)) {
                unique.add(play);
            }
        }

        return unique;
    }

    /**
     * Method which determine which play is going to be made of all possible play
     *
     * @param legalPlays list of all possible plays
     * @return play that is chosen to be executed
     */
    public List<Play> choosePlay(List<Play> legalPlays) {
        Strategy strategy = currentPlayer.getStrategy();
        return strategy.chooseMove(legalPlays, this);
    }

    /**
     * Updates the moves left after a play has being made
     *
     * @param play the play that was made
     * @return list of moves still left to play
     */
    public int[] updateMoves(Play play) {
        return removeMove(moves, play.getDieUsed());
    }

    /**
     * Removes one occurrence of a specific die value from an array of moves.
     *
     * @param currentMoves moves that are currently still available
     * @param die the die value that needs to be removed
     * @return new array without one occurrence of the die
     */
    private int[] removeMove(int[] currentMoves, int die) {

        int[] newMoves = new int[currentMoves.length - 1];

        boolean removed = false;
        int newIndex = 0;

        for (int move : currentMoves) {

            if (move == die && !removed) {
                removed = true;
                continue;
            }

            newMoves[newIndex] = move;
            newIndex++;
        }

        return newMoves;
    }

    /** -JAVADOC- */
    public void restoreMoves(int[] restoreMoves){
        this.moves = restoreMoves;
    }
    /**
     * Calculates all possible moves that are being made from the bar as start position
     *
     * @param die the distance that can be moved of a checker
     * @return list of all possible bar moves with corresponding distance
     */
    public ArrayList<Play> moveFromBar(int die) {
        ArrayList<Play> possible = new ArrayList<>();

        int toIndex;

        switch(currentPlayer.getColor()){
            case WHITE -> toIndex = 24 - die;
            case BLACK -> toIndex = die - 1;
            default -> throw new IllegalStateException("Unexpected value: " + currentPlayer.getColor());
        }

        Point target = this.currentBoard.getPoints().get(toIndex);

        boolean blocked  = target.isBlocked(currentPlayer.getColor().opponent());

        if (!blocked) {
            possible.add(
                    new Play(toIndex, Play.Type.OFF_BAR, currentPlayer, die));
        }

        return possible;
    }

    /**
     * Calculates all possible moves that are being made on the board.
     * Which thus have an initial start point and end point.
     *
     * @param die the distance that can be moved of a checker
     * @return list of all possible board moves with corresponding distance
     */
    public ArrayList<Play> moveBoard(int die) {
        ArrayList<Play> possible = new ArrayList<>();

        for (int step = 0; step < 24; step++) {
            // look at the checkers furthest from home first, for both colors
            int from = (currentPlayer.getColor() == Player.Color.WHITE) ? 23 - step : step;

            Point source = this.currentBoard.getPoints().get(from);

            boolean empty = source.isEmpty(currentPlayer.getColor());

            if (empty) {
                continue;
            }

            int to;

            switch (currentPlayer.getColor()){
                case WHITE -> to = from - die;
                case BLACK -> to = from + die;
                default -> throw new IllegalStateException("Unexpected value: " + currentPlayer.getColor());
            }

            if (to < 0 || to > 23) {
                continue;
            }

            Point target = this.currentBoard.getPoints().get(to);

            boolean blocked = target.isBlocked(currentPlayer.getColor().opponent());


            if (!blocked) {
                possible.add(
                        new Play(
                                from,
                                to,
                                Play.Type.NORMAL,
                                currentPlayer,
                                die
                        )
                );
            }
        }

        return possible;
    }


    /**
     * Calculates all possible moves that result in bearing off a checker.
     * A checker can always be borne off with the exact die.
     * A higher die may only be used when there is no checker further away from bearing off.
     *
     * @param die the distance that can be moved of a checker
     * @return list of all possible moves that bear off a checker with corresponding distance
     */
    public ArrayList<Play> moveBearOff(int die) {
        ArrayList<Play> possible = new ArrayList<>();

        for (int step = 0; step < 24; step++) {
            // look at the checkers furthest from home first, for both colors
            int from = (currentPlayer.getColor() == Player.Color.WHITE) ? 23 - step : step;

            Point source = this.currentBoard.getPoints().get(from);

            boolean hasChecker;

            hasChecker = !source.isEmpty(currentPlayer.getColor());

            if (!hasChecker) {
                continue;
            }

            int to;

            switch(currentPlayer.getColor()){
                case WHITE -> to = from - die;
                case BLACK -> to = from + die;
                default -> throw new IllegalStateException("Unexpected value: " + currentPlayer.getColor());
            }

            /*
             * Oversized bearing-off move.
             */
            if (to < -1 || to > 24) {

                if (hasCheckerFurtherFromOff(from)) {
                    continue;
                }

                possible.add(
                        new Play(
                                from,
                                Play.Type.BEAR_OFF,
                                currentPlayer,
                                die
                        )
                );

                /*
                 * Exact bearing-off move.
                 */
            } else if (to == -1 || to == 24) {

                possible.add(
                        new Play(
                                from,
                                Play.Type.BEAR_OFF,
                                currentPlayer,
                                die
                        )
                );
            }
        }

        return possible;
    }


    // Checks

    /**
     * Checks if there are other checkers further from bearing off as these should then be moved first
     *
     * @param from the position to compare against
     * @return if there are checkers further behind
     */
    private boolean hasCheckerFurtherFromOff(int from) {


        Player.Color currentColor = currentPlayer.getColor();

        switch(currentColor){
            case WHITE -> {
                for (int i = from + 1; i <= 23; i++) {

                    if (pointHasChecker(i, currentColor)) {

                        return true;
                    }
                }
            }
            case BLACK -> {
                for (int i = from - 1; i >= 0; i--) {

                    if (pointHasChecker(i,currentColor)) {

                        return true;
                    }
                }
            }
        }
        return false;
    }


    /**
     * Checks if the game is ended as one player has born off all his checkers
     *
     * @return if the game has ended
     */
    public boolean checkWinConditions() {
        return this.currentBoard.getOffBoard().isFull(Player.Color.BLACK)
                || this.currentBoard.getOffBoard().isFull(Player.Color.WHITE);
    }

    /** -JAVADOC- */
    public int getWinTypeValue(){
        Player.Color winner;
        Player.Color opponenent;

        //Find the winner
        if (currentBoard.getOffBoard().isFull(Player.Color.WHITE)){
            winner = Player.Color.WHITE;
            opponenent = Player.Color.BLACK;
        }else{
            winner = Player.Color.BLACK;
            opponenent = Player.Color.WHITE;
        }

        //Opponent has already borne off at least one checker
        if (currentBoard.getOffBoard().hasChecker(opponenent)){
            return 1;
        }

        //Opponent has a checker on the bar
        if (currentBoard.getBar().hasChecker(opponenent)){
            return 3;
        }
        //Check whether oppoenent has a checker in the winner's home board
        if (winner == Player.Color.WHITE){
            //White home board = point 1-6
            for (int i = 0; i <= 5; i++) {
                if (currentBoard.getPoints().get(i).hasChecker(opponenent)){
                    return 3;
                }
            }
        }else{
            // Black home board = points 19-24
            for (int i = 18; i <= 23; i++) {
                if (currentBoard.getPoints().get(i).hasChecker(opponenent)){
                    return 3;
                }
            }
        }
        //Opponent has borne off non, hut has no checker on the bar or in the winner's home board.
        return 2;
    }

    /** -JAVADOC- */
    public Player getWinner(){

        if (winner != null){
            return winner;
        }
        if(currentBoard.getOffBoard().isFull(Player.Color.WHITE)){
            return white;
        }if(currentBoard.getOffBoard().isFull(Player.Color.BLACK)){
            return black;
        }
        return null;
    }


    /**
     * Checks if the bar is occupied
     *
     * @return if the bar is occupied
     */
    public boolean barOccupied() {
        return this.currentBoard.getBar().hasChecker(currentPlayer.getColor());
    }


    /**
     * Checks if bearing off is allowed in the current setting of the board
     *
     * @return if bearing off is allowed
     */
    public boolean checkBearOff() {

        if (barOccupied()) {
            return false;
        }

        Player.Color currentColor = currentPlayer.getColor();

        switch(currentColor){

            case WHITE ->{
                for (int i = 6; i <= 23; i++) {

                    if (pointHasChecker(i,currentColor)) {

                        return false;
                    }
                }
            }
            case BLACK -> {
                for (int i = 17; i >= 0; i--) {

                    if (pointHasChecker(i,currentColor)) {

                        return false;
                    }
                }
            }
        }

        return true;
    }

    /**
     * @param i index of point
     * @param color of given player
     * @return true if point has checker of that color
     */
    private boolean pointHasChecker(int i, Player.Color color ){
        return this.getCurrentBoard()
                .getPoints()
                .get(i)
                .hasChecker(color);
    }

    /**
     * Checks if the current players turn is white
     *
     * @return if it's white turn
     */
    public boolean checkWhiteTurn() {

        if (currentPlayer == white) {
            return true;
        }

        if (currentPlayer == black) {
            return false;
        }

        throw new RuntimeException("Can't identify player");
    }


    // GETTERS

    /**
     * Give the current state of the board
     *
     * @return current board
     */
    public Board getCurrentBoard() {
        return currentBoard;
    }


    /**
     * Gives the current player
     *
     * @return current player
     */
    public Player getCurrentPlayer() {
        return this.currentPlayer;
    }

    /**
     * Gives the current white player
     *
     * @return white player
     */
    public Player getWhite() {
        return this.white;
    }

    /**
     * Gives the current black player
     *
     * @return black player
     */
    public Player getBlack() {
        return this.black;
    }

    /**
     * Gives the current moves still left
     *
     * @return current moves left
     */
    public int[] getMoves() {
        return this.moves;
    }

    /**
     * Gives the current round dice
     *
     * @return round dice
     */
    public Dice getRoundDice() {
        return roundDice;
    }

    /** -JAVADOC- */
    public int getTurnNumber(){
        return this.turnNumber;
    }

    /**
     * Removes the die used by the given play from the moves still available.
     *
     * @param play play which has been made
     */
    public void removeUsedMove(Play play) {
        this.moves = updateMoves(play);
    }

    /**
     * Executes a play by creating the new board state and removing the die used in the play.
     *
     * @param play play which needs to be executed
     */
    public void executePlay(Play play) {
        this.currentBoard = new Board(this, play);
        this.moves = updateMoves(play);
    }

    /**
     * This method will help reset the round and this will be shown in the GUI via the rest button
     */
    public void resetRound(){

        this.turnNumber = 0;
        this.active = true;
        this.doublingFactor = 1;
        this.doublingCubeOwner = null;
        this.doubleOfferedBy = null;
        this.doubleOffered = false;
        this.winner = null;
        this.winTypeValue = 0;

        this.roundDice = new Dice();
        this.currentBoard = new Board();

        this.currentPlayer = null;
        this.moves = null;

    }
    /** -JAVADOC- */
    public int getDoublingFactor(){
        return doublingFactor;
    }

    /** -JAVADOC- */
    public Player getDoublingCubeOwner(){
        return doublingCubeOwner;
    }
    /** -JAVADOC- */
    public boolean isDoubleOffered(){
        return doubleOffered;
    }
    /** -JAVADOC- */
    public Player getDoubleOfferedBy(){
        return doubleOfferedBy;
    }

    /** -JAVADOC- */
    public boolean canDouble(){
        if (doubleOffered){
            return false;
        }

        if (currentPlayer == null){
            return false;
        }
        if (moves != null && moves.length > 0){
            return false;
        }

        if (doublingCubeOwner == null){
            return true;
        }

        return doublingCubeOwner == currentPlayer;
    }

    /** -JAVADOC- */
    public boolean offerDouble(){
        if(!canDouble()){
            return false;
        }
        doubleOffered = true;
        doubleOfferedBy = currentPlayer;
        return true;
    }

    /** -JAVADOC- */
    public void acceptDouble(){
        if(!doubleOffered){
            return;
        }
        doublingFactor *= 2;
        doublingCubeOwner = getOpponent(doubleOfferedBy);
        doubleOffered = false;
        doubleOfferedBy = null;
    }
    /** -JAVADOC- */
    public Player getOpponent(Player player){
        if (player == white){
            return black;
        }
        return white;
    }
    /** -JAVADOC- */
    public void declineDouble(){
        if(!doubleOffered){
            throw new IllegalStateException("no double offered");
        }
        winner = doubleOfferedBy;
        doubleOffered = false;
        doubleOfferedBy = null;
        active = false;
    }
    /** -JAVADOC- */
    public boolean hasLegalMoves(){
        if(moves == null || moves.length == 0){
            return  false;
        }
        return !getRequiredPlays(moves).isEmpty();
    }
}