package gameproject.ai;

// Written with Claude (Anthropic), reviewed by Kilian.
// If committed, disclose this assistance with:
// AI-Assisted-By: Claude

import gameproject.game.elements.Dice;
import gameproject.game.elements.Play;
import gameproject.game.elements.Round;
import gameproject.player.Player;
import gameproject.player.Strategy;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Supplier;

/**
 * Long strength experiments that fill the gaps in the visualization, in one test that can
 * run overnight. Every game is appended to strength.csv (same format as AIAgentStrengthTest),
 * so the visualizer picks the results up automatically.
 *
 * <ol>
 *   <li>Weights vs search: depth 0 with the tuned-best weights against depth 1 with equal weights,
 *       and against depth 1 with the tuned-best weights; the same two with the new weights.</li>
 *   <li>New coefficients: the team's latest weights against tuned-best (depth 2, K = 5).</li>
 *   <li>Difficulty levels for the GUI: candidates for Easy, Medium and Advanced play each other,
 *       and the Advanced candidate is timed against the 3 s budget (written to output.csv).</li>
 *   <li>Final check after the colour tie-break fix: the champion against the old agent and
 *       against depth 1 (only when the fix is in the code).</li>
 *   <li>Fair K comparison: every K plays depth 1 on the same dice (games 0-499).</li>
 * </ol>
 *
 * The test checks itself whether the tie-break fix in Round is present. Results with the fix
 * get " (fixed)" at the end of the match name, so they are never mixed with older games.
 * Finished games are skipped, so after an interruption just run it again.
 * To add an experiment later, add a line in {@link #plan(boolean)}.
 *
 * Options (VM options in IntelliJ): -Dovernight.hours=10, -Dovernight.threads=N
 * (default: half the logical processors).
 */
class OvernightExperimentsTest {

    private static final double HOURS = Double.parseDouble(System.getProperty("overnight.hours", "10"));
    private static final int THREADS = Integer.getInteger("overnight.threads",
            Math.max(1, Runtime.getRuntime().availableProcessors() / 2));

    private static final Path STRENGTH_FILE = Path.of("src/test/java/gameproject/ai/strength.csv");
    private static final Path BENCHMARK_FILE = Path.of("src/test/java/gameproject/ai/output.csv");
    private static final String BENCHMARK_HEADER = "algorithm,depth,improvements,board,currentPlayer,dice,probability,"
            + "legalFirstPlays,chosenMoves,nodeVisits,leafEvaluations,generatedTurns,duplicateTurns,roundCopies,"
            + "boardCopies,evaluatorCalls,cacheHits,cacheMisses,minNs,medianNs,averageNs,maxNs";
    private static final String TIMING_BOARD = "night-20-positions";   // same board name as NightTournamentTest
    private static final int TIMING_POSITIONS = 20;
    private static final double ADVANCED_BUDGET_MS = 3000;             // time budget of the Advanced level
    private static final String STRENGTH_HEADER =
            "match,game,agentA,depthA,agentB,depthB,colorA,winner,points,turns,msPerMoveA,msPerMoveB";
    private static final int MAX_TURNS = 2000;
    private static final long PROGRESS_EVERY_MS = 30_000;

    // Weights in BoardEvaluator order (declared before the agents below).
    private static final double[] EQUAL = {0.125, 0.125, 0.125, 0.125, 0.125, 0.125, 0.125, 0.125};
    private static final double[] TUNED_BEST = {0.224, 0.152, 0.227, 0.041, 0.034, 0.088, 0.133, 0.101};
    private static final double[] TUNED_NEW = {0.225, 0.217, 0.119, 0.057, 0.038, 0.006, 0.091, 0.247};
    /** Medium level in App: halfway between equal and tuned-new. */
    private static final double[] TUNED_BLEND = {0.175, 0.171, 0.122, 0.091, 0.0815, 0.0655, 0.108, 0.186};
    /** A weaker Medium candidate: three quarters equal, one quarter tuned-new. */
    private static final double[] TUNED_QUARTER = {0.15, 0.148, 0.1235, 0.108, 0.10325, 0.09525, 0.1165, 0.1555};

    /** The Advanced candidate: timed against the 3 s budget before any games are played. */
    private static final Agent ADVANCED_CANDIDATE = Agent.of("tuned-new", TUNED_NEW, 2, 10);

    // Rough average time per decision in a game (ms), only used for the time estimate.
    private static double estimatedMsPerDecision(int depth, int moveFilter) {
        if (depth == 0) {
            return 1;
        }
        if (depth == 1) {
            return 15;
        }
        return switch (moveFilter) {
            case 2 -> 60;
            case 3 -> 130;
            case 5 -> 330;
            case 8 -> 780;
            default -> 1100;
        };
    }

    /**
     * One agent, named like the visualizer expects: "depth2-move-filter-k5" for equal weights,
     * "tuned-best-depth2-move-filter-k5" for other weights, "depth1-full" without a filter.
     */
    record Agent(String name, int depth, int moveFilter, Supplier<Strategy> factory) {

        static Agent of(String weightsName, double[] weights, int depth, int moveFilter) {
            String prefix = weightsName.equals("equal") ? "" : weightsName + "-";
            String filter = moveFilter == AdvancedAgent.NO_FILTER ? "-full" : "-move-filter-k" + moveFilter;
            return new Agent(prefix + "depth" + depth + filter, depth, moveFilter,
                    () -> new AdvancedAgent(new BoardEvaluator(weights.clone()), depth, moveFilter));
        }

        double estimatedMsPerDecision() {
            return OvernightExperimentsTest.estimatedMsPerDecision(depth, moveFilter);
        }
    }

    /**
     * Games fromGame .. toGame-1 of the match "a vs b" (+ tag). Game g uses the dice seed
     * seedOffset + g / 2 with the colours swapped between g and g + 1, like AIAgentStrengthTest.
     */
    record Experiment(String title, Agent a, Agent b, int fromGame, int toGame, long seedOffset, String tag) {

        String match() {
            return a.name() + " vs " + b.name() + tag;
        }

        /** Seconds per game on one thread, from about 30 decisions per agent. */
        double estimatedSecondsPerGame() {
            return 30 * (a.estimatedMsPerDecision() + b.estimatedMsPerDecision()) / 1000;
        }
    }

    /**
     * The experiments, in the order they run (most important first). Add new ones here.
     *
     * @param fixed whether the colour tie-break fix is in Round
     */
    private static List<Experiment> plan(boolean fixed) {
        String tag = fixed ? " (fixed)" : "";
        List<Experiment> plan = new ArrayList<>();

        // 1. Do good weights make up for one level of search? Depth 0 is cheap, so this runs first.
        plan.add(new Experiment("Depth 0 tuned-best vs depth 1 equal",
                Agent.of("tuned-best", TUNED_BEST, 0, AdvancedAgent.NO_FILTER),
                Agent.of("equal", EQUAL, 1, AdvancedAgent.NO_FILTER), 0, 600, 50_000, tag));
        // What one level of search is worth when both sides use the good weights (same dice).
        plan.add(new Experiment("Depth 0 tuned-best vs depth 1 tuned-best",
                Agent.of("tuned-best", TUNED_BEST, 0, AdvancedAgent.NO_FILTER),
                Agent.of("tuned-best", TUNED_BEST, 1, AdvancedAgent.NO_FILTER), 0, 600, 50_000, tag));
        // The same two matches with the team's newest weights.
        plan.add(new Experiment("Depth 0 tuned-new vs depth 1 equal",
                Agent.of("tuned-new", TUNED_NEW, 0, AdvancedAgent.NO_FILTER),
                Agent.of("equal", EQUAL, 1, AdvancedAgent.NO_FILTER), 0, 600, 50_000, tag));
        plan.add(new Experiment("Depth 0 tuned-new vs depth 1 tuned-new",
                Agent.of("tuned-new", TUNED_NEW, 0, AdvancedAgent.NO_FILTER),
                Agent.of("tuned-new", TUNED_NEW, 1, AdvancedAgent.NO_FILTER), 0, 600, 50_000, tag));

        // 2. New coefficients against the best so far, same search setting for both.
        plan.add(new Experiment("New coefficients vs tuned-best",
                Agent.of("tuned-new", TUNED_NEW, 2, 5), Agent.of("tuned-best", TUNED_BEST, 2, 5),
                0, 600, 30_000, tag));

        // 3. Difficulty levels for the GUI (fresh dice). The plan in App:
        //    Easy: depth 0 equal | Medium: depth 1 blend | Advanced: depth 2, K 10 tuned-new.
        //    Other medium candidates (depth 1 tuned-new / equal) are kept for comparison.
        //    Each level should beat the one below clearly; Medium should sit in the middle.
        Agent easy = Agent.of("equal", EQUAL, 0, AdvancedAgent.NO_FILTER);
        Agent easyTuned = Agent.of("tuned-new", TUNED_NEW, 0, AdvancedAgent.NO_FILTER);
        Agent medium = Agent.of("tuned-new", TUNED_NEW, 1, AdvancedAgent.NO_FILTER);
        Agent mediumEqual = Agent.of("equal", EQUAL, 1, AdvancedAgent.NO_FILTER);
        Agent advanced = ADVANCED_CANDIDATE;
        Agent advancedBest = Agent.of("tuned-best", TUNED_BEST, 2, 10);
        Agent mediumBlend = Agent.of("tuned-blend", TUNED_BLEND, 1, AdvancedAgent.NO_FILTER);
        // The planned levels first: Medium (blend) vs Easy, then Advanced vs Medium (blend).
        plan.add(new Experiment("Levels: medium (blend) vs easy", mediumBlend, easy, 0, 600, 60_000, tag));
        plan.add(new Experiment("Levels: blend vs d1 new", mediumBlend, medium, 0, 600, 60_000, tag));
        plan.add(new Experiment("Levels: blend vs d1 equal", mediumBlend, mediumEqual, 0, 600, 60_000, tag));
        plan.add(new Experiment("Levels: advanced vs medium (blend)", advanced, mediumBlend, 0, 400, 60_000, tag));
        // Weaker Medium candidates, because Advanced beats the blend by only about 56%.
        Agent mediumQuarter = Agent.of("tuned-quarter", TUNED_QUARTER, 1, AdvancedAgent.NO_FILTER);
        Agent depthZeroBest = Agent.of("tuned-best", TUNED_BEST, 0, AdvancedAgent.NO_FILTER);
        Agent depthZeroBlend = Agent.of("tuned-blend", TUNED_BLEND, 0, AdvancedAgent.NO_FILTER);
        plan.add(new Experiment("Levels: d0 tuned-best vs easy", depthZeroBest, easy, 0, 600, 60_000, tag));
        plan.add(new Experiment("Levels: d1 quarter vs easy", mediumQuarter, easy, 0, 600, 60_000, tag));
        plan.add(new Experiment("Levels: d0 blend vs easy", depthZeroBlend, easy, 0, 600, 60_000, tag));
        plan.add(new Experiment("Levels: blend vs quarter", mediumBlend, mediumQuarter, 0, 600, 60_000, tag));
        plan.add(new Experiment("Levels: advanced vs d0 tuned-best", advanced, depthZeroBest, 0, 400, 60_000, tag));
        plan.add(new Experiment("Levels: advanced vs d1 quarter", advanced, mediumQuarter, 0, 400, 60_000, tag));
        plan.add(new Experiment("Levels: medium (d1 new) vs easy", medium, easy, 0, 600, 60_000, tag));
        plan.add(new Experiment("Levels: d1 equal vs easy", mediumEqual, easy, 0, 600, 60_000, tag));
        plan.add(new Experiment("Levels: d0 new vs easy", easyTuned, easy, 0, 600, 60_000, tag));
        plan.add(new Experiment("Levels: d1 new vs d1 equal", medium, mediumEqual, 0, 600, 60_000, tag));
        plan.add(new Experiment("Levels: advanced vs easy", advanced, easy, 0, 200, 60_000, tag));
        plan.add(new Experiment("Levels: advanced vs medium (d1 new)", advanced, medium, 0, 400, 60_000, tag));
        plan.add(new Experiment("Levels: advanced vs d1 equal", advanced, mediumEqual, 0, 400, 60_000, tag));
        plan.add(new Experiment("Levels: advanced weights new vs best", advanced, advancedBest, 0, 400, 60_000, tag));

        // 4. The night champion again, now with the fixed tie-break (same dice as the night run).
        if (fixed) {
            Agent champion = Agent.of("tuned-best", TUNED_BEST, 2, 10);
            plan.add(new Experiment("Champion vs old agent (after fix)",
                    champion, Agent.of("equal", EQUAL, 2, 2), 0, 300, 10_000, tag));
            plan.add(new Experiment("Champion vs depth 1 (after fix)",
                    champion, Agent.of("equal", EQUAL, 1, AdvancedAgent.NO_FILTER), 0, 300, 10_000, tag));
        }

        // 5. Fair K comparison: all K against depth 1 on the same dice (seeds 0..249).
        Agent depthOne = Agent.of("equal", EQUAL, 1, AdvancedAgent.NO_FILTER);
        if (fixed) {
            for (int k : new int[]{2, 3, 5, 8}) {
                plan.add(new Experiment("K = " + k + " vs depth 1 (after fix)",
                        Agent.of("equal", EQUAL, 2, k), depthOne, 0, 300, 0, tag));
            }
        } else {
            // K = 2 and K = 3 already have games 0-499; K = 5 has 0-199 and K = 8 has 0-99.
            plan.add(new Experiment("K = 5 vs depth 1, games 200-499",
                    Agent.of("equal", EQUAL, 2, 5), depthOne, 200, 500, 0, ""));
            plan.add(new Experiment("K = 8 vs depth 1, games 100-499",
                    Agent.of("equal", EQUAL, 2, 8), depthOne, 100, 500, 0, ""));
        }
        return plan;
    }

    // ---------- run ----------

    private final long start = System.currentTimeMillis();
    private final long deadline = start + (long) (HOURS * 3_600_000);
    private long lastProgress;
    private int gamesDone;
    private double secondsDone;      // estimated cost of the games already played in this run
    private double secondsTotal;     // estimated cost of all games this run has to play

    private record Result(boolean aIsWhite, boolean aWon, int points, int turns, double msA, double msB) {
    }

    @Test
    void runOvernightExperiments() throws Exception {
        checkHeader();
        boolean fixed = tieBreakIsSymmetric();
        List<Experiment> plan = plan(fixed);
        say("Tie-break fix " + (fixed ? "found: results are stored as \"(fixed)\" matches"
                : "NOT found: results are stored with the old code"));
        say("Plan (" + THREADS + " threads, at most " + HOURS + " h):");

        List<List<Integer>> todo = new ArrayList<>();
        for (Experiment e : plan) {
            List<Integer> missing = missingGames(e);
            todo.add(missing);
            secondsTotal += missing.size() * e.estimatedSecondsPerGame() / THREADS;
            say(String.format(Locale.ROOT, "  %-40s %4d games to play (%d already done)",
                    e.title(), missing.size(), (e.toGame() - e.fromGame()) - missing.size()));
        }
        say("Estimated time: " + duration((long) secondsTotal) + " plus about 5 min of timing (a rough guess, it gets better while running)");

        timeAgent(ADVANCED_CANDIDATE);

        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        try {
            for (int i = 0; i < plan.size(); i++) {
                if (System.currentTimeMillis() > deadline) {
                    say("Time is up, stopping. Run the test again to continue.");
                    break;
                }
                runExperiment(pool, plan.get(i), todo.get(i), i + 1, plan.size());
            }
        } finally {
            pool.shutdownNow();
        }
        say("Done. Regenerate the page with visualize_benchmark.py.");
    }

    private void runExperiment(ExecutorService pool, Experiment e, List<Integer> games,
                               int number, int count) throws Exception {
        say(String.format(Locale.ROOT, "[%d/%d] %s: %s", number, count, e.title(), e.match()));
        List<Future<Result>> running = new ArrayList<>();
        for (int game : games) {
            running.add(pool.submit(() -> playAndRecord(e, game)));
        }
        for (Future<Result> future : running) {
            future.get();
        }
        printSummary(e);
    }

    private Result playAndRecord(Experiment e, int game) throws IOException {
        if (System.currentTimeMillis() > deadline) {
            return null;
        }
        Result r = playGame(e, game);
        String row = String.format(Locale.ROOT, "%s,%d,%s,%d,%s,%d,%s,%s,%d,%d,%.3f,%.3f%n",
                e.match(), game, e.a().name(), e.a().depth(), e.b().name(), e.b().depth(),
                r.aIsWhite() ? "WHITE" : "BLACK", r.aWon() ? "A" : "B", r.points(), r.turns(), r.msA(), r.msB());
        synchronized (this) {
            Files.writeString(STRENGTH_FILE, row, StandardOpenOption.APPEND);
            gamesDone++;
            secondsDone += e.estimatedSecondsPerGame() / THREADS;
            if (System.currentTimeMillis() - lastProgress > PROGRESS_EVERY_MS) {
                lastProgress = System.currentTimeMillis();
                printProgress();
            }
        }
        return r;
    }

    /** One full game, played like the GUI (one chooseMove per turn). */
    private static Result playGame(Experiment e, int game) {
        boolean aIsWhite = game % 2 == 0;
        Strategy agentA = e.a().factory().get();
        Strategy agentB = e.b().factory().get();
        Player white = new Player("White", Player.Color.WHITE, aIsWhite ? agentA : agentB);
        Player black = new Player("Black", Player.Color.BLACK, aIsWhite ? agentB : agentA);
        Round round = new Round(white, black, new Dice(new Random(e.seedOffset() + game / 2)));
        round.determineFirstPlayer();

        long nanosA = 0, nanosB = 0;
        int movesA = 0, movesB = 0;
        for (int turn = 1; turn <= MAX_TURNS; turn++) {
            round.startTurn();
            List<Play> legal = round.getRequiredPlays(round.getMoves());
            if (!legal.isEmpty()) {
                Strategy mover = round.getCurrentPlayer().getStrategy();
                long t0 = System.nanoTime();
                List<Play> chosen = mover.chooseMove(legal, round);
                long nanos = System.nanoTime() - t0;
                if (mover == agentA) {
                    nanosA += nanos;
                    movesA++;
                } else {
                    nanosB += nanos;
                    movesB++;
                }
                for (Play play : chosen) {
                    round.executePlay(play);
                }
            }
            if (round.checkWinConditions()) {
                boolean aWon = (round.getWinner() == white) == aIsWhite;
                return new Result(aIsWhite, aWon, round.getWinTypeValue(), turn,
                        nanosA / 1e6 / Math.max(1, movesA), nanosB / 1e6 / Math.max(1, movesB));
            }
            round.endTurn();
        }
        throw new IllegalStateException("Game " + game + " did not finish within " + MAX_TURNS + " turns");
    }

    // ---------- timing of the Advanced candidate ----------

    /**
     * Times one decision per position and roll, single-threaded (before the games start, so
     * nothing else is running), and adds one row per roll to output.csv on board
     * "night-20-positions": min / median / average / max over the positions. Skipped if the
     * agent already has timing rows there.
     */
    private void timeAgent(Agent agent) throws IOException {
        if (Files.notExists(BENCHMARK_FILE) || !Files.readString(BENCHMARK_FILE).startsWith(BENCHMARK_HEADER)) {
            say("Timing skipped: output.csv is missing or has other columns.");
            return;
        }
        for (String line : Files.readAllLines(BENCHMARK_FILE)) {
            String[] f = line.split(",", -1);
            if (f.length == 22 && f[2].equals(agent.name()) && f[3].equals(TIMING_BOARD)) {
                say("Timing of " + agent.name() + " already in output.csv, skipped.");
                return;
            }
        }
        say("Timing " + agent.name() + " on " + TIMING_POSITIONS + " positions x 21 rolls (single-threaded)...");
        List<Round> positions = samplePositions(TIMING_POSITIONS);
        Strategy warmup = Agent.of("equal", EQUAL, 1, AdvancedAgent.NO_FILTER).factory().get();
        for (Round position : positions) {                 // let the JIT compile the search first
            timeDecision(warmup, position, new Dice.DiceOutcome(3, 1, 0));
        }

        Map<Dice.DiceOutcome, List<Long>> byRoll = new LinkedHashMap<>();
        long slowest = 0;
        for (int p = 0; p < positions.size(); p++) {
            for (Dice.DiceOutcome roll : Dice.getDiceOutcomes()) {
                long nanos = timeDecision(agent.factory().get(), positions.get(p), roll);
                if (nanos >= 0) {
                    byRoll.computeIfAbsent(roll, r -> new ArrayList<>()).add(nanos);
                    slowest = Math.max(slowest, nanos);
                }
            }
            say(String.format(Locale.ROOT, "  position %d/%d done, slowest move so far %.2f s",
                    p + 1, positions.size(), slowest / 1e9));
        }

        StringBuilder rows = new StringBuilder();
        for (Map.Entry<Dice.DiceOutcome, List<Long>> entry : byRoll.entrySet()) {
            Dice.DiceOutcome roll = entry.getKey();
            List<Long> nanos = entry.getValue();
            nanos.sort(null);
            double average = nanos.stream().mapToLong(Long::longValue).average().orElse(0);
            rows.append(String.format(Locale.ROOT, "ADVANCED,%d,%s,%s,mixed,%d-%d,%.6f,,,,,,,,,,,,%d,%d,%.0f,%d%n",
                    agent.depth(), agent.name(), TIMING_BOARD, roll.die1(), roll.die2(), roll.probability(),
                    nanos.getFirst(), nanos.get(nanos.size() / 2), average, nanos.getLast()));
        }
        Files.writeString(BENCHMARK_FILE, rows.toString(), StandardOpenOption.APPEND);
        say(String.format(Locale.ROOT, "Slowest move %.2f s, budget %.1f s: %s", slowest / 1e9, ADVANCED_BUDGET_MS / 1000,
                slowest / 1e6 <= ADVANCED_BUDGET_MS ? "within budget" : "OVER budget (the depth-1 fallback would be used)"));
    }

    /** Time of one chooseMove call in nanoseconds, or -1 if the roll has no legal move. */
    private static long timeDecision(Strategy strategy, Round position, Dice.DiceOutcome roll) {
        Round round = new Round(position, new Dice(roll.die1(), roll.die2()));
        List<Play> legal = round.getRequiredPlays(round.getMoves());
        if (legal.isEmpty()) {
            return -1;
        }
        long t0 = System.nanoTime();
        strategy.chooseMove(legal, round);
        return System.nanoTime() - t0;
    }

    /**
     * Positions to time: the opening and then every 6th turn of games between depth-1 agents,
     * stored before the dice are thrown (same method and seeds as NightTournamentTest).
     */
    private static List<Round> samplePositions(int count) {
        List<Round> positions = new ArrayList<>();
        for (int seed = 1; positions.size() < count; seed++) {
            Player white = new Player("White", Player.Color.WHITE, new AdvancedAgent(new BoardEvaluator(EQUAL.clone()), 1));
            Player black = new Player("Black", Player.Color.BLACK, new AdvancedAgent(new BoardEvaluator(EQUAL.clone()), 1));
            Round round = new Round(white, black, new Dice(new Random(15_000 + seed)));
            round.determineFirstPlayer();
            for (int turn = 0; turn < MAX_TURNS && positions.size() < count; turn++) {
                if (turn % 6 == 0) {
                    positions.add(new Round(round));
                }
                round.startTurn();
                List<Play> legal = round.getRequiredPlays(round.getMoves());
                if (!legal.isEmpty()) {
                    for (Play play : round.getCurrentPlayer().getStrategy().chooseMove(legal, round)) {
                        round.executePlay(play);
                    }
                }
                if (round.checkWinConditions()) {
                    break;
                }
                round.endTurn();
            }
        }
        return positions;
    }

    // ---------- tie-break check ----------

    /**
     * True if both colours look at their checkers in the same order (furthest from home
     * first, or nearest first), i.e. the colour tie-break fix is in Round. Uses the opening
     * position with a die of 1: the first generated play shows where the loop starts.
     */
    private static boolean tieBreakIsSymmetric() {
        Player white = new Player("White", Player.Color.WHITE, null);
        Player black = new Player("Black", Player.Color.BLACK, null);
        Round round = new Round(white, black, new Dice(new Random(1)));
        round.determineFirstPlayer();
        int whiteFirst = -1, blackFirst = -1;
        for (int i = 0; i < 2; i++) {
            List<Play> plays = round.possiblePlays(new int[]{1});
            int from = plays.getFirst().getInitialPoint();
            if (round.getCurrentPlayer() == white) {
                whiteFirst = from;
            } else {
                blackFirst = from;
            }
            round.switchPlayer();
        }
        // Point i for white is the mirror of point 23 - i for black.
        return whiteFirst == 23 - blackFirst;
    }

    // ---------- files and output ----------

    private static void checkHeader() throws IOException {
        if (Files.notExists(STRENGTH_FILE)) {
            Files.writeString(STRENGTH_FILE, STRENGTH_HEADER + System.lineSeparator());
            return;
        }
        String content = Files.readString(STRENGTH_FILE);
        if (!content.startsWith(STRENGTH_HEADER)) {
            throw new IllegalStateException("strength.csv has other columns than expected; nothing was written.");
        }
        if (!content.endsWith("\n")) {
            // a row was cut off when a run was stopped: end it, that game is played again
            Files.writeString(STRENGTH_FILE, System.lineSeparator(), StandardOpenOption.APPEND);
        }
    }

    /** Game numbers of this experiment that are not yet in strength.csv. */
    private static List<Integer> missingGames(Experiment e) throws IOException {
        Set<Integer> done = new HashSet<>();
        for (String line : Files.readAllLines(STRENGTH_FILE)) {
            String[] f = line.split(",", -1);
            if (f.length == 12 && f[0].equals(e.match())) {
                try {
                    done.add(Integer.parseInt(f[1]));
                } catch (NumberFormatException cutOffRow) {
                    // ignored: that game is played again
                }
            }
        }
        List<Integer> missing = new ArrayList<>();
        for (int g = e.fromGame(); g < e.toGame(); g++) {
            if (!done.contains(g)) {
                missing.add(g);
            }
        }
        return missing;
    }

    /** Win rate of A over all games of the match in strength.csv, with its 95% range. */
    private void printSummary(Experiment e) throws IOException {
        int games = 0, wins = 0;
        for (String line : Files.readAllLines(STRENGTH_FILE)) {
            String[] f = line.split(",", -1);
            if (f.length == 12 && f[0].equals(e.match())) {
                games++;
                wins += f[7].equals("A") ? 1 : 0;
            }
        }
        if (games == 0) {
            return;
        }
        double p = (double) wins / games, z = 1.96, z2 = z * z;
        double centre = (p + z2 / (2 * games)) / (1 + z2 / games);
        double margin = z * Math.sqrt(p * (1 - p) / games + z2 / (4.0 * games * games)) / (1 + z2 / games);
        String verdict = centre - margin > 0.5 ? "A is stronger" : centre + margin < 0.5 ? "B is stronger" : "no clear difference";
        say(String.format(Locale.ROOT, "      result: A wins %.1f%% of %d games (95%%: %.1f-%.1f%%) -> %s",
                100 * p, games, 100 * (centre - margin), 100 * (centre + margin), verdict));
    }

    /** One progress line: a bar for the whole run, games played and the time left. */
    private void printProgress() {
        double share = secondsTotal == 0 ? 1 : Math.min(1, secondsDone / secondsTotal);
        long elapsed = (System.currentTimeMillis() - start) / 1000;
        long left = share > 0.02 ? (long) (elapsed / share - elapsed) : (long) (secondsTotal - secondsDone);
        int filled = (int) Math.round(share * 30);
        say(String.format(Locale.ROOT, "[%s%s] %3.0f%%  %d games  elapsed %s  left ~%s",
                "#".repeat(filled), "-".repeat(30 - filled), 100 * share, gamesDone, duration(elapsed), duration(left)));
    }

    private static String duration(long seconds) {
        Duration d = Duration.ofSeconds(Math.max(0, seconds));
        return d.toHours() > 0 ? d.toHours() + " h " + d.toMinutesPart() + " min" : d.toMinutesPart() + " min";
    }

    private static void say(String message) {
        System.out.println(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "  " + message);
    }
}
