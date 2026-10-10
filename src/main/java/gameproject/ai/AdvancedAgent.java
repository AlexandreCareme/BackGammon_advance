package gameproject.ai;

import gameproject.game.elements.*;
import gameproject.player.Player;

import java.util.*;

/**
 * Class which represents the more advanced agent.
 * It uses expectiminimax to determine the best complete turn.
 */
public class AdvancedAgent extends AIAgent {
    /**
     * The depth till which the agent searches ahead
     */
    private final int searchDepth;
    private final List<Dice.DiceOutcome> diceOutcomes;
    private long nodeVisits;
    private long leafEvaluations;
    private long generatedTurns;
    // Turns skipped because an earlier turn reached the same final board.
    private long duplicateTurns;
    private long roundCopies;
    //boardCopies is currently unavailable and remains zero.
    //roundCopies is the active copy-allocation metric.
    private long boardCopies;
    private long evaluatorCalls;
    private Metrics lastMetrics = new Metrics(
            0, 0, 0, 0, 0, 0, 0
    );


    /** this branches on every turn at every node( the standard with no move filter)  */
    public static final int NO_FILTER = Integer.MAX_VALUE;

    /** How many of the best turns are searched deeper (for nodes who's children aren't leaves). */
    private final int moveFilter;

    /**
     * Time limit for one decision when searching deeper than depth 1. If the deep search is not
     * finished in time, the agent plays the depth-1 answer instead, so it stays playable on slow computers.
     */
    public static final long TIME_LIMIT_NANOS = 4_000_000_000L;

    /** System.nanoTime() at which the running deep search must stop (no limit outside a deep search). */
    private long deadline = Long.MAX_VALUE;

    /** True if the last decision ran out of time and used the depth-1 answer. */
    private boolean lastDecisionTimedOut;

    /** Thrown inside search() when the time is up; every applied turn is undone by the finally blocks. */
    private static final class TimeUp extends RuntimeException {
        TimeUp() {
            super(null, null, false, false);    // no stack trace needed, which keeps it cheap
        }
    }

    /**
     * Allows to create an advanced AI which uses the evaluator to evaluate boards
     * and has a certain depth till which it searches
     * @param evaluator evaluate boards objective function value
     * @param depth the depth to which the expected decision tree will be searched
     *              Note: all decisions are capped at 4s and the fallback decision  is depth 1
     */
    public AdvancedAgent(BoardEvaluator evaluator, int depth) {
        this(evaluator, depth, NO_FILTER); //old constructor without move Filter
    }
    /**
     * Allows to create an advanced AI which uses the evaluator to evaluate boards
     * and has a certain depth till which it searches
     * @param evaluator evaluate boards objective function value
     * @param depth the depth to which the expected decision tree will be searched
     *              Note: all decisions are capped at 4s and the fallback decision  is depth 1
     * @param moveFilter restricts the branching of the decision tree to the k best moves with ordering by the objective function.
     *                   The leafs are all evaluated.
     */
    public AdvancedAgent(BoardEvaluator evaluator, int depth, int moveFilter) {
        super(evaluator);
        if (moveFilter < 1) {
            throw new IllegalArgumentException("moveFilter must be at least 1");
        }
        this.searchDepth = depth;
        this.diceOutcomes = Dice.getDiceOutcomes();
        this.moveFilter = moveFilter;
    }

    /**
     * Chooses a move or sequence of moves from the available legal moves.
     * Searches deeper than depth 1 get TIME_LIMIT_NANOS, if that runs out, the depth-1 answer is played.
     *
     * @param possiblePlays the possible legal plays
     * @param round the current round that is played
     * @return the chosen turn (empty if there is no legal play)
     */
    @Override
    public List<Play> chooseMove(List<Play> possiblePlays, Round round) {
        lastDecisionTimedOut = false;
        if (searchDepth <= 1) {
            return bestTurn(possiblePlays, round, searchDepth);       // fast, no time limit needed
        }
        deadline = System.nanoTime() + TIME_LIMIT_NANOS;
        try {
            return bestTurn(possiblePlays, round, searchDepth);
        } catch (TimeUp timeUp) {
            lastDecisionTimedOut = true;
            deadline = Long.MAX_VALUE;
            return bestTurn(possiblePlays, round, 1);                  // depth 1 takes only milliseconds
        } finally {
            deadline = Long.MAX_VALUE;
        }
    }

    /**
     * Searches every turn from the current position to the given depth and returns the best one.
     *
     * @param possiblePlays the possible legal plays
     * @param round the current round that is played (left unchanged)
     * @param depth the search depth
     * @return the turn with the highest expected value
     */
    private List<Play> bestTurn(List<Play> possiblePlays, Round round, int depth) {

        resetMetrics();
        List<Play> bestMoves = List.of();

        // If no possible first moves exist return an empty List.
        if (possiblePlays.isEmpty()) {
            saveMetricsSnapshot();
            return bestMoves;
        }

        //Copy round object once so we don't mutate original round


        // Get all possible Turns from the starting position
        Player.Color aiColor = round.getCurrentPlayer().getColor();
        List<List<Play>> possibleTurns = turnsToSearch(round, depth);

        // Compare which Turn has the best expected value
        double bestValue = Double.NEGATIVE_INFINITY;
        for (List<Play> turn : possibleTurns) {
            Deque<UndoPlay> undoRecords = applyTurnForSearch(round, turn);

            double value;
            // Find the expected value of that Turn Branch
            try {
                value = search(round, aiColor, depth);
            } finally {
                undoTurnForSearch(round,undoRecords);
            }

            if (value > bestValue) {
                bestValue = value;
                bestMoves = turn;
            }
        }

        saveMetricsSnapshot();
        return bestMoves;
    }

    /**
     * Represents the ExpectMinMax algorithm which searches branches and nodes till it end in leafs and passes it's expected value back up.
     * Till it reaches the beginning node and passes it's expected value of that node
     *
     * @param round the snapshot of the round we're currently in
     * @param aiColor the color of the AI agent
     * @param depth the current depth left at which we search
     * @return the expected value at the current node
     */
    private double search(Round round, Player.Color aiColor, int depth) {

        incrementNodeVisits();
        Board board = round.getCurrentBoard();

        // Leaf Nodes:
        // AI has won (assign high value)
        if (board.getOffBoard().isFull(aiColor)) {
            incrementLeafEvaluations();
            return 1000 + depth;
        }

        // Opponent has won (assign low value)
        if (board.getOffBoard().isFull(aiColor.opponent())) {
            incrementLeafEvaluations();
            return -1000 - depth;
        }

        // Maximum search depth reached
        if (depth == 0) {
            incrementLeafEvaluations();
            incrementEvaluatorCalls();
            return getEvaluator().evaluate(board, aiColor);
        }

        // Non-Leaf Nodes: stop the deep search when its time is up (only set for depth >= 2)
        if (System.nanoTime() > deadline) {
            throw new TimeUp();
        }
        double expectedValue = 0.0;

        // Try every possible future dice throw
        for (Dice.DiceOutcome outcome : diceOutcomes) {

            // Create new rounds/dice after the throws
            Dice dice = new Dice(outcome.die1(), outcome.die2());
            incrementRoundCopies();
            Round diceRound = new Round(round, dice);

            // search the possible turns from that board position.(here the moveFilter is applied)
            List<List<Play>> possibleTurns = turnsToSearch(diceRound, depth - 1);

            double value;

            // No legal moves (Skip turn)
            if (possibleTurns.isEmpty()) {
                diceRound.endTurn();
                // We search further down
                value = search(diceRound, aiColor, depth - 1);
            }

            // If there are legal moves play turn
            else {

                // Ai maximizes its own value. (Max-part)
                if (diceRound.getCurrentPlayer().getColor() == aiColor) {
                    value = Double.NEGATIVE_INFINITY;
                    for (List<Play> turn : possibleTurns) {

                        Deque<UndoPlay> undoRecords = applyTurnForSearch(diceRound,turn);
                        try {
                            double childValue = search(diceRound, aiColor, depth - 1);
                            if (childValue > value) {
                            value = childValue;
                            }
                        }finally {
                            undoTurnForSearch(diceRound,undoRecords);
                        }
                    }
                }

                // Opponent minimizes the value of you. (Min-part)
                else {
                    value = Double.POSITIVE_INFINITY;
                    for (List<Play> turn : possibleTurns) {

                        Deque<UndoPlay> undoRecords = applyTurnForSearch(diceRound,turn);
                        try {
                            double childValue = search(diceRound, aiColor, depth - 1);
                            if (childValue < value) {
                                value = childValue;
                            }
                        }finally {
                            undoTurnForSearch(diceRound,undoRecords);
                        }
                    }
                }
            }
            // Calculates the expected value of this current leaf and passes it up.
            expectedValue += outcome.probability() * value;
        }
        return expectedValue;
    }

    /**
     * Creates every complete possible turn from the given possible first plays.
     * Each complete turn consists of all legal plays that can be made using
     * the dice available in the current turn.
     *
     * @param round the current snapshot of the round
     * @return all possible complete sequences of plays
     * @param scoreTurns passed to collectSequences(...) to turn on costly scoring
     */
    List<Candidate> generateTurns(Round round, boolean scoreTurns) {
        incrementRoundCopies();
        Round searchRound = new Round(round);
        List<Candidate> longest = new ArrayList<>();
        collectSequences(searchRound, new ArrayList<>(), longest, scoreTurns); //this adds to longest by calling recordSequence(...)

        if (longest.isEmpty()) {
            return new ArrayList<>();                       // no legal play at all
        }

        // Rule 2: only one of two different dice can be used -> use the higher one if possible.
        int[] dice = round.getMoves();
        if (longest.get(0).turn().size() == 1 && dice.length == 2 && dice[0] != dice[1]) {
            int higherDie = Math.max(dice[0], dice[1]);
            List<Candidate> withHigherDie = new ArrayList<>();
            for (Candidate candidate : longest) {
                if (candidate.turn().get(0).getDieUsed() == higherDie) {
                    withHigherDie.add(candidate);
                }
            }
            if (!withHigherDie.isEmpty()) {
                longest = withHigherDie;
            }
        }

        // Dedup: keep one turn per final board (after rule 2, see beloww)
        Set<PositionKey> seenBoards = new HashSet<>();
        List<Candidate> uniqueCandidates = new ArrayList<>();
        for (Candidate candidate : longest) {
            if (seenBoards.add(candidate.finalBoard())) {
                uniqueCandidates.add(candidate);
                incrementGeneratedTurns();
            } else {
                incrementDuplicateTurns();
            }
        }
        return uniqueCandidates;
    }

    /** All legal turns, unordered (no scoring cost). Used where order doesn't matter. And backwards compatibility
     * @param round round to get turns on
     * @return list of list of plays containing all legal turns
     */
    List<List<Play>> getPossibleTurns(Round round) {
        return turnsOf(generateTurns(round, false));
    }

    /** All legal turns, best first for the player to move. Ordered by objective value from evaluator
     * @param round round to get turns on
     * @return ordered list of list of plays containing all legal turns
     */
    List<List<Play>> getOrderedTurns(Round round) {
        List<Candidate> candidates = generateTurns(round, true);
        candidates.sort(BEST_FIRST);
        return turnsOf(candidates);
    }

    /**
     * The turns to search from this position. If the children are leaves (childDepth == 0),
     * all turns are needed anyway, because evaluating them is the search. Otherwise only
     * the best moveFilter turns for the player to move are kept.
     *
     * @param round the position with the dice already set
     * @param childDepth the depth that will be passed to search for each child
     * @return list of k(fromMove filter)  best turn candidates
     */
    private List<List<Play>> turnsToSearch(Round round, int childDepth) {
        if (moveFilter == NO_FILTER || childDepth == 0) {
            return getPossibleTurns(round);             // cheap version without scoring
        }
        List<List<Play>> ordered = getOrderedTurns(round);
        if (ordered.size() <= moveFilter) {
            return ordered;
        }
        return ordered.subList(0, moveFilter);          //the best k( where k = int moveFilter))  turns are kept
    }

    /**
     * Helper method to turn List of turn candidates into list of list of plays, removing scoring and key
     * @param candidates
     * @return list of list of plays from candidates list
     */
    private List<List<Play>> turnsOf(List<Candidate> candidates) {
        List<List<Play>> turns = new ArrayList<>(candidates.size());
        for (Candidate candidate : candidates) {
            turns.add(candidate.turn());
        }
        return Collections.unmodifiableList(turns);
    }

    /**
     * This method collects the legal sequences that make up a turn. This method improves efficiecy
     * of continueTurn() by not calling costly getRequiredPlays() but instead handling required Play rules.
     * Rule 2.) is handled by getPossibleTurns() this calls this method and 1.) by recordSequence() which this method calls
     * (1. use most number of dice and 2. if only one move legal use highest die)
     * @param round round to collect sequence on
     * @param movesMade plays already made in this sequence
     * @param scoreTurns passed to recordSequence(...) to diable costly ordering for move filter
     * @param longest list of Plays is modified such that only longest sequence(i.e. most number of dice) used
     */
    private void collectSequences(Round round, List<Play> movesMade, List<Candidate> longest, boolean scoreTurns) {
        List<Play> plays = round.possiblePlays(round.getMoves());   // no getRequiredPlays!

        if (plays.isEmpty()) {                  // no die can be used any more bc sequence is finished
            recordSequence(round, movesMade, longest,scoreTurns);
            return;
        }
        for (Play play : plays) {
            UndoPlay undo = applyPlayForSearch(round, play);
            movesMade.add(play);
            try {
                collectSequences(round, movesMade, longest,scoreTurns);
            } finally {
                movesMade.remove(movesMade.size() - 1);
                undoPlayForSearch(round, undo);
            }
        }
    }

    /**
     * storing Sequence found (along with score and key) in record object while applying rule 1.) that most number
     * of moves required to be chosen
     * @param round round state where this sequence takes place
     * @param movesMade list of plays that have been made
     * @param longest List storing longest sequences, i.e. with most moves
     * @param scoreTurns scoring the sequence for ordering for move filter and leaf evaluation
     */
    private void recordSequence(Round round, List<Play> movesMade, List<Candidate> longest, boolean scoreTurns) {
        if (movesMade.isEmpty()) {
            return;
        }
        int currentMax = longest.isEmpty() ? 0 : longest.get(0).turn().size();
        if (movesMade.size() < currentMax) {
            return;                                 // shorter than what we already have: illegal
        }
        if (movesMade.size() > currentMax) {
            longest.clear();                        // found a longer one: drop all shorter ones
        }
        double score = 0.0;
        if (scoreTurns) {
            incrementEvaluatorCalls();
            // The Board is  in this turn's final position and the Player has not
            // switched yet.So this scores the turn for the Player That made it
            score = getEvaluator().evaluate(round.getCurrentBoard(), round.getCurrentPlayer().getColor());
        }
        longest.add(new Candidate(List.copyOf(movesMade), createKey(round, 0), score));
    }

    /**
     * Record that stores relevant information to undo a play. this is used to mutate a board object
     * such that costly board copying can be prevented
     * @param play to undo
     * @param initialWB snapshot of initial location before play
     * @param forwardWB snapshot of forward location before play
     * @param barWB snapshot of bar before play
     * @param offWB snapshot of offBar before play
     * @param remainingMoves number of remaining moves to do, that is die faces to use
     */
    private record UndoPlay(
            Play play,
            LocationSnapshot initialWB,
            LocationSnapshot forwardWB,
            LocationSnapshot barWB,
            LocationSnapshot offWB,
            int[] remainingMoves
    ) {}

    /** A complete play sequence together with the board it ends on and a score determined by objective off evaluator. */
    private record Candidate(List<Play> turn, PositionKey finalBoard, double score) {
    }
    /** Comparator that puts Highest score first */
    private static final Comparator<Candidate> BEST_FIRST =
            Comparator.comparingDouble(Candidate::score).reversed(); //comparingDouble avoids boxing doubles into Double objects(more efficent)


    /**
     * Record of a Location that stores number of white and black checkers
     * @param white number of white checkers
     * @param black number of black checkers
     */
    private record LocationSnapshot(int white, int black) {
    }

    /**
     * record that stores a board position with
     * @param counts for each location and color of a boards locations( 24 points and bar and offboard)
     * @param toMove color making the move
     * @param depth depth in decision search tree we are at rn
     */
    private record PositionKey(int[] counts, Player.Color toMove, int depth) {

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof PositionKey that)) {
                return false;
            }
            return depth == that.depth
                    && toMove == that.toMove
                    && Arrays.equals(counts, that.counts);
        }

        @Override
        public int hashCode() {
            int hash = Arrays.hashCode(counts);     //hash of the aray contents array object
            hash = 31 * hash + toMove.ordinal();    // Mix in side to move (white=0,black=1), ×31 keeps fields distinct
            hash = 31 * hash + depth;               // mix in depth the same way
            return hash;                            // equal keys -> equal hash
        }
    }

    /**
     * method to create a position key from
     *
     * @param round round state to create position key from
     * @param depth current depth in search tree
     * @return position key from input round
     */
    private PositionKey createKey(Round round, int depth) {
        return new PositionKey(
                round.getCurrentBoard().getCheckerCounts(),
                round.getCurrentPlayer().getColor(),
                depth
        );
    }

    /**
     *
     * @param location
     * @return
     */
    private LocationSnapshot snapshot(Location location) {
        return new LocationSnapshot(
                location.getCheckers(Player.Color.WHITE),
                location.getCheckers(Player.Color.BLACK)
        );
    }
    /** -JAVADOC- */
    private LocationSnapshot getBarSnap(Board board) {
        return snapshot(board.getBar());
    }

    /** -JAVADOC- */
    private LocationSnapshot getPointSnap(int index, Board board) {
        return snapshot(board.getPoints().get(index));
    }

    /** -JAVADOC- */
    private LocationSnapshot getOffBoardSnap(Board board){
        return snapshot(board.getOffBoard());
    }


    /** -JAVADOC- */
    private UndoPlay createUndoPlay(Round round, Play play) {
        Board board = round.getCurrentBoard();

        LocationSnapshot initial = null;
        LocationSnapshot forward = null;

        switch (play.getType()) {
            case NORMAL -> {
                initial = getPointSnap(play.getInitialPoint(), board);
                forward = getPointSnap(play.getForwardPoint(), board);
            }
            case BEAR_OFF -> {
                initial = getPointSnap(play.getInitialPoint(), board);
            }
            case OFF_BAR -> {
                forward = getPointSnap(play.getForwardPoint(), board);
            }
        }

        return new UndoPlay(
                play,
                initial,
                forward,
                getBarSnap(board),
                getOffBoardSnap(board),
                copyMoves(round.getMoves())
        );
    }

    /** -JAVADOC- */
    private int[] copyMoves(int[] moves) {
        return moves == null ? null : moves.clone();
    }

    /** -JAVADOC- */
    private UndoPlay applyPlayForSearch(Round round, Play play){
        UndoPlay undo = createUndoPlay(round, play);

        Board board = round.getCurrentBoard();

        switch(play.getType() ){
            case NORMAL -> {

                board.getPoints().get(play.getInitialPoint()).removeChecker(play.getColor());
                forwardAndHit( board, play);

            }
            case OFF_BAR -> {
                board.getBar().removeChecker(play.getColor());
                forwardAndHit(board,play);
            }
            case BEAR_OFF -> {
                board.getPoints().get(play.getInitialPoint()).removeChecker(play.getColor());
                board.getOffBoard().addChecker(play.getColor());
            }
        }

        round.removeUsedMove(play);

        return undo;
    }

    /** -JAVADOC- */
    private void forwardAndHit(Board board, Play play){

        Point forward = board.getPoints().get(play.getForwardPoint());

        if(forward.hasChecker(play.getColor().opponent())){
            forward.removeChecker(play.getColor().opponent());
            board.getBar().addChecker(play.getColor().opponent());
        }

        forward.addChecker(play.getColor());
    }

    /**
     * Method to undo a play
     * @param round round object to restore
     * @param undo undo specifications
     */
    private void undoPlayForSearch(Round round, UndoPlay undo){

        Board board = round.getCurrentBoard();
        Play play = undo.play();

        if (undo.initialWB() != null){

            //restore initial point counts
            board.getPoints().get(play.getInitialPoint()).restoreCounts(
                    undo.initialWB().white(),
                    undo.initialWB().black()
            );


        }

        if (undo.forwardWB() != null){
            board.getPoints().get(play.getForwardPoint()).restoreCounts(
                    undo.forwardWB().white(),
                    undo.forwardWB().black()
            );
        }

        //restore bar counts
        board.getBar().restoreCounts(
                undo.barWB().white(),
                undo.barWB().black()
        );
        //restore off-board counts
        board.getOffBoard().restoreCounts(
                undo.offWB().white(),
                undo.offWB().black()
        );
        //restore round.moves from undo.remainingMoves
        round.restoreMoves(
                undo.remainingMoves() == null
                        ? null
                        : undo.remainingMoves().clone()
        );

    }

    /** -JAVADOC- */
    private Deque<UndoPlay> applyTurnForSearch(Round round,List<Play> turn){

        Deque<UndoPlay> undoRecords = new ArrayDeque<>();

        for (Play play : turn) {
            undoRecords.push(applyPlayForSearch(round, play));
        }
        round.switchPlayer();
        return undoRecords;
    }

//    applyTurnForSearch(round, turn)
//    create one UndoPlay for each play
//    apply each play with applyPlayForSearch(...)
//    return all undo records
//
//    undoTurnForSearch(round, undoRecords)
//    undo the records in reverse order


    /** -JAVADOC- */
    private void undoTurnForSearch(Round round, Deque<UndoPlay> undoRecords){
        round.switchPlayer();
        while (!undoRecords.isEmpty()) {
            undoPlayForSearch( round, undoRecords.pop());
        }
    }
    /** -JAVADOC- */
    private void recordTurnCopyBased(Round round, List<Play> movesMade,
                            List<List<Play>> possibleTurns, Set<PositionKey> seenBoards) {
        PositionKey finalBoard = createKey(round, 0);
        if (seenBoards.add(finalBoard)) {
            possibleTurns.add(List.copyOf(movesMade));
            incrementGeneratedTurns();
        } else {
            incrementDuplicateTurns();   // same final board as an earlier turn, skipped
        }
    }
    //Metrics Record and method to improve efficiency
    /** -JAVADOC- */
    public record Metrics(long nodeVisits,
                          long leafEvaluations,
                          long generatedTurns,
                          long duplicateTurns,
                          long roundCopies,
                          long boardCopies,
                          long evaluatorCalls){

    }

    /** -JAVADOC- */
    private void resetMetrics(){
        nodeVisits = 0;
        leafEvaluations = 0;
        generatedTurns = 0;
        duplicateTurns = 0;
        roundCopies = 0;
        boardCopies = 0;
        evaluatorCalls = 0;
    }

    /** @return true if the last chooseMove ran out of time and played the depth-1 answer */
    public boolean lastDecisionTimedOut() {
        return lastDecisionTimedOut;
    }

    /** -JAVADOC- */
    public Metrics getLastMetrics() {
        return lastMetrics;
    }


    /** -JAVADOC- */
    private void incrementNodeVisits(){
        nodeVisits++;
    }
    /** -JAVADOC- */
    private void incrementLeafEvaluations(){
        leafEvaluations++;
    }
    /** -JAVADOC- */
    private void incrementGeneratedTurns(){
        generatedTurns++;
    }
    /** -JAVADOC- */
    private void incrementDuplicateTurns(){
        duplicateTurns++;
    }
    /** -JAVADOC- */
    private void incrementRoundCopies(){
        roundCopies++;
    }
    /** -JAVADOC- */
    private void incrementBoardCopies(){
        boardCopies++;
    }
    /** -JAVADOC- */
    private void incrementEvaluatorCalls(){
        evaluatorCalls++;
    }
    /** -JAVADOC- */
    private void saveMetricsSnapshot() {
        lastMetrics = new Metrics(
                nodeVisits,
                leafEvaluations,
                generatedTurns,
                duplicateTurns,
                roundCopies,
                boardCopies,
                evaluatorCalls
        );
    }


}