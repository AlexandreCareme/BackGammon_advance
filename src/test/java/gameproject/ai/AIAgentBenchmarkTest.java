package gameproject.ai;

// AI-assisted (GitHub Copilot Chat), reviewed and adapted by Kilian.
// If committed, disclose this assistance with:
// AI-Assisted-By: GitHub Copilot Chat

import gameproject.game.elements.Dice;
import gameproject.game.elements.Play;
import gameproject.game.elements.Round;
import gameproject.player.Player;
import gameproject.player.Strategy;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Timing benchmark for the AI strategies on the standard board.
 *
 * benchmarkAndAppend runs every dice outcome for one algorithm, depth and label,
 * skips outcomes already recorded in output.csv, and appends one row per outcome.
 * It is diagnostic: it records timings and search metrics instead of asserting thresholds.
 */
class AIAgentBenchmarkTest {

    private static final int WARMUP_RUNS = 2;
    private static final int MEASURED_RUNS = 5;
    private static final int DEPTH_TWO_WARMUP_RUNS = 0;
    private static final int DEPTH_TWO_MEASURED_RUNS = 1;
    private static final int FULL_WARMUP_RUNS = 2;
    private static final int FULL_MEASURED_RUNS = 5;
    private static final int DEPTH_THREE_WARMUP_RUNS = 0;
    private static final int DEPTH_THREE_MEASURED_RUNS = 1;
    private static final Path OUTPUT_FILE =
            Path.of("src/test/java/gameproject/ai/output.csv");
    private static final String CSV_HEADER =
            "algorithm,depth,improvements,board,currentPlayer,dice,"
                    + "probability,legalFirstPlays,chosenMoves,nodeVisits,"
                    + "leafEvaluations,generatedTurns,duplicateTurns,roundCopies,boardCopies,"
                    + "evaluatorCalls,cacheHits,cacheMisses,minNs,medianNs,"
                    + "averageNs,maxNs";

    enum Algorithm {
        EASY,
        ADVANCED
    }

    @Test
    void benchmarkDepthTwoThenDepthThree() throws IOException {
        benchmarkAdvancedDepthTwoFullAndAppendToCsv();
        benchmarkAdvancedDepthThreeAndAppendToCsv();
    }

    /**
     * Move filter at depth 2 for K = 3, 5 and 8: all 21 rolls, 2 warmup and 5 measured
     * runs each, appended under "depth2-move-filter-kK". Compare with "depth2-full-5-runs".
     * Override with -Dbenchmark.moveFilter.warmupRuns / measuredRuns.
     */
    @Test
    void benchmarkMoveFilterDepthTwoAndAppendToCsv() throws IOException {
        for (int k : new int[]{3, 5, 8}) {
            benchmarkAndAppend(
                    Algorithm.ADVANCED,
                    2,
                    "depth2-move-filter-k" + k,
                    "benchmark.moveFilter.warmupRuns",
                    "benchmark.moveFilter.measuredRuns",
                    FULL_WARMUP_RUNS,
                    FULL_MEASURED_RUNS,
                    dice -> true,
                    k
            );
        }
    }

    /**
     * Move filter K = 5 at depth 3: one measured run per roll, non-doubles first,
     * appended under "depth3-move-filter-k5". Without the filter depth 3 takes days;
     * with it, a decision should take seconds to about a minute.
     * Override with -Dbenchmark.depth3.warmupRuns / measuredRuns.
     */
    @Test
    void benchmarkMoveFilterDepthThreeAndAppendToCsv() throws IOException {
        for (boolean doubles : new boolean[]{false, true}) {
            benchmarkAndAppend(
                    Algorithm.ADVANCED,
                    3,
                    "depth3-move-filter-k5",
                    "benchmark.depth3.warmupRuns",
                    "benchmark.depth3.measuredRuns",
                    DEPTH_THREE_WARMUP_RUNS,
                    DEPTH_THREE_MEASURED_RUNS,
                    dice -> (dice.die1() == dice.die2()) == doubles,
                    5
            );
        }
    }


    /**
     * Full depth-2 benchmark of the current AdvancedAgent: all 21 rolls with
     * 2 warmup and 5 measured runs each, appended under "depth2-full-5-runs".
     * Expect roughly 20-30 minutes. Already recorded rolls are skipped.
     * Override with -Dbenchmark.depth2.full.warmupRuns / measuredRuns.
     */

    void benchmarkAdvancedDepthTwoFullAndAppendToCsv() throws IOException {
        benchmarkAndAppend(
                Algorithm.ADVANCED,
                2,
                "depth2-full-5-runs",
                "benchmark.depth2.full.warmupRuns",
                "benchmark.depth2.full.measuredRuns",
                FULL_WARMUP_RUNS,
                FULL_MEASURED_RUNS
        );
    }

    /**
     * Depth-3 benchmark of the current AdvancedAgent: one measured run per roll,
     * appended under "depth3-single-run". Non-doubles run first (faster), then doubles.
     *
     * Warning: depth 3 is about 500x the work of depth 2, so a single roll can take
     * from tens of minutes (non-doubles) to hours (doubles); the full set may take
     * more than a day. Every finished roll is appended immediately and recorded rolls
     * are skipped, so it is safe to stop and restart.
     * Override with -Dbenchmark.depth3.warmupRuns / measuredRuns.
     */

    void benchmarkAdvancedDepthThreeAndAppendToCsv() throws IOException {
        benchmarkAndAppend(
                Algorithm.ADVANCED,
                3,
                "depth3-single-run",
                "benchmark.depth3.warmupRuns",
                "benchmark.depth3.measuredRuns",
                DEPTH_THREE_WARMUP_RUNS,
                DEPTH_THREE_MEASURED_RUNS,
                dice -> dice.die1() != dice.die2()
        );
        benchmarkAndAppend(
                Algorithm.ADVANCED,
                3,
                "depth3-single-run",
                "benchmark.depth3.warmupRuns",
                "benchmark.depth3.measuredRuns",
                DEPTH_THREE_WARMUP_RUNS,
                DEPTH_THREE_MEASURED_RUNS,
                dice -> dice.die1() == dice.die2()
        );
    }

    /**
     * Generic append-only benchmark entry point for teammates. It runs every
     * unordered dice outcome, skips already recorded keys, and appends one row
     * immediately after each completed outcome.
     */
    static void benchmarkAndAppend(
            Algorithm algorithm,
            int depth,
            String improvementLabel,
            String warmupProperty,
            String measuredProperty,
            int defaultWarmupRuns,
            int defaultMeasuredRuns) throws IOException {
        benchmarkAndAppend(
                algorithm,
                depth,
                improvementLabel,
                warmupProperty,
                measuredProperty,
                defaultWarmupRuns,
                defaultMeasuredRuns,
                dice -> true
        );
    }

    /**
     * Same as above, but only benchmarks the dice outcomes accepted by diceFilter.
     *
     * @param diceFilter returns true for the dice outcomes to benchmark
     */
    static void benchmarkAndAppend(
            Algorithm algorithm,
            int depth,
            String improvementLabel,
            String warmupProperty,
            String measuredProperty,
            int defaultWarmupRuns,
            int defaultMeasuredRuns,
            Predicate<Dice.DiceOutcome> diceFilter) throws IOException {
        benchmarkAndAppend(
                algorithm,
                depth,
                improvementLabel,
                warmupProperty,
                measuredProperty,
                defaultWarmupRuns,
                defaultMeasuredRuns,
                diceFilter,
                AdvancedAgent.NO_FILTER
        );
    }

    /**
     * Same as above, with a move filter: AdvancedAgent searches only the best
     * moveFilter turns deeper (AdvancedAgent.NO_FILTER = all turns).
     *
     * @param moveFilter the K passed to new AdvancedAgent(evaluator, depth, K)
     */
    static void benchmarkAndAppend(
            Algorithm algorithm,
            int depth,
            String improvementLabel,
            String warmupProperty,
            String measuredProperty,
            int defaultWarmupRuns,
            int defaultMeasuredRuns,
            Predicate<Dice.DiceOutcome> diceFilter,
            int moveFilter) throws IOException {

        int warmupRuns = Integer.getInteger(
                warmupProperty,
                defaultWarmupRuns
        );
        int measuredRuns = Integer.getInteger(
                measuredProperty,
                defaultMeasuredRuns
        );

        if (warmupRuns < 0 || measuredRuns < 1) {
            throw new IllegalArgumentException(
                    "Benchmark requires warmupRuns >= 0 and measuredRuns >= 1"
            );
        }

        double[] coefficients = equalCoefficients();
        appendHeaderIfNeeded();
        Set<String> existingRows = existingOutputKeys();

        for (Dice.DiceOutcome dice : Dice.getDiceOutcomes()) {
            if (!diceFilter.test(dice)) {
                continue;
            }
            String key = outputKey(
                    algorithm.name(),
                    depth,
                    improvementLabel,
                    dice
            );
            if (existingRows.contains(key)) {
                continue;
            }

            BenchmarkResult result = benchmarkCase(
                    algorithm,
                    depth,
                    dice.die1(),
                    dice.die2(),
                    coefficients,
                    warmupRuns,
                    measuredRuns,
                    moveFilter
            );
            appendCsvRow(
                    algorithm,
                    dice,
                    result,
                    depth,
                    improvementLabel
            );
            existingRows.add(key);
        }
    }

    private static Set<String> existingOutputKeys() throws IOException {
        Set<String> keys = new HashSet<>();
        if (Files.notExists(OUTPUT_FILE)) {
            return keys;
        }

        for (String line : Files.readAllLines(OUTPUT_FILE)) {
            if (line.startsWith("algorithm,") || line.isBlank()) {
                continue;
            }
            String[] columns = line.split(",", -1);
            if (columns.length >= 6) {
                keys.add(String.join(
                        "|",
                        columns[0],
                        columns[1],
                        columns[2],
                        columns[5]
                ));
            }
        }
        return keys;
    }

    private static String outputKey(
            String algorithm,
            int depth,
            String improvementLabel,
            Dice.DiceOutcome dice) {
        return String.join(
                "|",
                algorithm,
                Integer.toString(depth),
                improvementLabel,
                dice.die1() + "-" + dice.die2()
        );
    }

    private static BenchmarkResult benchmarkCase(
            Algorithm algorithm,
            int depth,
            int die1,
            int die2,
            double[] coefficients,
            int warmupRuns,
            int measuredRuns,
            int moveFilter) {

        Round benchmarkRound = createRound(algorithm, depth, coefficients, die1, die2, moveFilter);
        List<Play> possiblePlays =
                benchmarkRound.getRequiredPlays(benchmarkRound.getMoves());

        for (int i = 0; i < warmupRuns; i++) {
            Strategy strategy = createStrategy(
                    algorithm,
                    new BoardEvaluator(coefficients),
                    depth,
                    moveFilter
            );
            List<Play> warmupResult =
                    strategy.chooseMove(possiblePlays, benchmarkRound);
            assertNotNull(warmupResult);
        }

        long[] samples = new long[measuredRuns];
        List<Play> chosenMoves = List.of();
        long nodeVisits = 0;
        long leafEvaluations = 0;
        long generatedTurns = 0;
        long duplicateTurns = 0;
        long roundCopies = 0;
        long boardCopies = 0;
        long evaluatorCalls = 0;

        for (int i = 0; i < measuredRuns; i++) {
            Round freshRound = createRound(
                    algorithm,
                    depth,
                    coefficients,
                    die1,
                    die2,
                    moveFilter
            );
            List<Play> freshPossiblePlays =
                    freshRound.getRequiredPlays(freshRound.getMoves());
            Strategy strategy = freshRound.getCurrentPlayer().getStrategy();

            long start = System.nanoTime();
            chosenMoves = strategy.chooseMove(freshPossiblePlays, freshRound);
            samples[i] = System.nanoTime() - start;

            if (strategy instanceof AdvancedAgent advancedAgent) {
                AdvancedAgent.Metrics metrics =
                        advancedAgent.getLastMetrics();
                nodeVisits += metrics.nodeVisits();
                leafEvaluations += metrics.leafEvaluations();
                generatedTurns += metrics.generatedTurns();
                duplicateTurns += metrics.duplicateTurns();
                roundCopies += metrics.roundCopies();
                boardCopies += metrics.boardCopies();
                evaluatorCalls += metrics.evaluatorCalls();
            }
        }

        return new BenchmarkResult(
                benchmarkRound.getCurrentPlayer().getName(),
                possiblePlays.size(),
                chosenMoves.size(),
                nodeVisits / measuredRuns,
                leafEvaluations / measuredRuns,
                generatedTurns / measuredRuns,
                duplicateTurns / measuredRuns,
                roundCopies / measuredRuns,
                boardCopies / measuredRuns,
                evaluatorCalls / measuredRuns,
                minimum(samples),
                median(samples),
                average(samples),
                maximum(samples)
        );
    }

    /**
     * Writes the header to a new or empty CSV. For an existing CSV it checks that
     * the header matches CSV_HEADER, so rows are never appended under different columns.
     *
     * @throws IOException if the file cannot be read or written
     * @throws IllegalStateException if the existing header differs from CSV_HEADER
     */
    private static void appendHeaderIfNeeded() throws IOException {
        if (Files.notExists(OUTPUT_FILE) || Files.size(OUTPUT_FILE) == 0) {
            Files.writeString(
                    OUTPUT_FILE,
                    CSV_HEADER + System.lineSeparator(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
            return;
        }

        String existingHeader;
        try (java.io.BufferedReader reader = Files.newBufferedReader(OUTPUT_FILE)) {
            existingHeader = reader.readLine();
        }
        if (!CSV_HEADER.equals(existingHeader)) {
            throw new IllegalStateException(
                    "output.csv has different columns than CSV_HEADER. "
                            + "Migrate the file (add the new columns to the header "
                            + "and every row) before appending new benchmark rows."
            );
        }
    }

    private static void appendCsvRow(
            Algorithm algorithm,
            Dice.DiceOutcome dice,
            BenchmarkResult result,
            int depth,
            String improvementLabel) throws IOException {

        String row = String.format(
                Locale.ROOT,
                "%s,%d,%s,standard,%s,%d-%d,%.6f,"
                        + "%d,%d,%d,%d,%d,%d,%d,%d,%d,,,"   // cacheHits, cacheMisses: no longer measured
                        + "%d,%d,%.0f,%d%n",
                algorithm,
                depth,
                improvementLabel,
                result.currentPlayer(),
                dice.die1(),
                dice.die2(),
                dice.probability(),
                result.legalFirstPlays(),
                result.chosenMoves(),
                result.nodeVisits(),
                result.leafEvaluations(),
                result.generatedTurns(),
                result.duplicateTurns(),
                result.roundCopies(),
                result.boardCopies(),
                result.evaluatorCalls(),
                result.minNs(),
                result.medianNs(),
                result.averageNs(),
                result.maxNs()
        );
        Files.writeString(
                OUTPUT_FILE,
                row,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
        );
    }

    private static Strategy createStrategy(
            Algorithm algorithm,
            BoardEvaluator evaluator,
            int depth,
            int moveFilter) {

        return switch (algorithm) {
            case EASY -> new EasyAgent(evaluator);
            case ADVANCED -> new AdvancedAgent(evaluator, depth, moveFilter);
        };
    }

    private static Round createRound(
            Algorithm algorithm,
            int depth,
            double[] coefficients,
            int die1,
            int die2,
            int moveFilter) {

        BoardEvaluator evaluator = new BoardEvaluator(coefficients);
        Strategy strategy = createStrategy(algorithm, evaluator, depth, moveFilter);

        Player white = new Player("White", Player.Color.WHITE, strategy);
        Player black = new Player("Black", Player.Color.BLACK, strategy);

        Random openingRandom = new FixedRandom(0, 1);
        Round round = new Round(
                white,
                black,
                new Dice(1, 2, openingRandom)
        );
        round.determineFirstPlayer();

        return new Round(round, new Dice(die1, die2));
    }

    private static double[] equalCoefficients() {
        double[] coefficients = new double[8];
        double equalWeight = 1.0 / coefficients.length;
        double assigned = 0.0;

        for (int i = 0; i < coefficients.length - 1; i++) {
            coefficients[i] = equalWeight;
            assigned += coefficients[i];
        }

        coefficients[coefficients.length - 1] = 1.0 - assigned;
        return coefficients;
    }

    private static long minimum(long[] values) {
        long result = values[0];
        for (long value : values) {
            result = Math.min(result, value);
        }
        return result;
    }

    private static long maximum(long[] values) {
        long result = values[0];
        for (long value : values) {
            result = Math.max(result, value);
        }
        return result;
    }

    private static double average(long[] values) {
        long total = 0;
        for (long value : values) {
            total += value;
        }
        return (double) total / values.length;
    }

    private static long median(long[] values) {
        long[] sorted = values.clone();
        java.util.Arrays.sort(sorted);
        return sorted[sorted.length / 2];
    }

    private record BenchmarkResult(
            String currentPlayer,
            int legalFirstPlays,
            int chosenMoves,
            long nodeVisits,
            long leafEvaluations,
            long generatedTurns,
            long duplicateTurns,
            long roundCopies,
            long boardCopies,
            long evaluatorCalls,
            long minNs,
            long medianNs,
            double averageNs,
            long maxNs) {
    }

    private static final class FixedRandom extends Random {
        private final int[] values;
        private int index;

        private FixedRandom(int... values) {
            this.values = values;
        }

        @Override
        public int nextInt(int bound) {
            if (index >= values.length) {
                throw new IllegalStateException("Opening roll requested too many values");
            }
            return values[index++];
        }
    }
}
