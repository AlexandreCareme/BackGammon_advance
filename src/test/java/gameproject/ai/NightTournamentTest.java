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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Overnight tournament that looks for the strongest AdvancedAgent whose slowest
 * decision stays under a time limit (default 4 s, doubles included), and collects
 * the evidence for it.
 *
 * <ol>
 *   <li>Timing: every move-filter setting is timed single-threaded on positions taken
 *       from a real game, for all 21 rolls. A setting is allowed only if no decision
 *       took longer than the limit.</li>
 *   <li>Coefficients: the tuned weights from Coefficients.java against equal weights,
 *       all at depth 2 with K = 2 (cheap, already proven stronger than depth 1).</li>
 *   <li>Settings ladder: every allowed setting, from cheap to expensive, plays the
 *       current champion. A challenger with a win rate above 50% becomes the champion.</li>
 *   <li>Final evidence: the champion against the starting agent and against depth 1.</li>
 *   <li>Champion timing on more positions.</li>
 * </ol>
 *
 * A match is played in batches of 50 games and stops early once the 99% confidence
 * interval no longer contains 50%. Games are paired (same dice, colours swapped) and
 * run in parallel threads; timing is always measured single-threaded.
 *
 * Results: night_games.csv (one row per game), night_timing.csv (one row per timed
 * decision) and night_report.md (summary, rewritten after every match). Finished
 * games are never played twice, so after an interruption just run the test again.
 *
 * The data also goes into the existing benchmark files, so the visualizer shows it:
 * every game is copied to strength.csv (match name ends with "(night)") and the timing
 * is added to output.csv as board "night-6-positions" (all settings) and
 * "night-20-positions" (champion), one row per roll with the worst case in maxNs.
 *
 * Options (VM options in IntelliJ): -Dnight.hours=8.5, -Dnight.maxMoveSeconds=4,
 * -Dnight.threads=N (default: half the logical processors).
 */
class NightTournamentTest {

    // ---------- options ----------

    private static final double HOURS = doubleProperty("night.hours", 8.5);
    private static final double LIMIT_MS = 1000 * doubleProperty("night.maxMoveSeconds", 4.0);
    private static final int THREADS = Integer.getInteger("night.threads",
            Math.max(1, Runtime.getRuntime().availableProcessors() / 2));

    private static final int COEFFICIENT_GAMES = Integer.getInteger("night.coefficientGames", 600);
    private static final int SETTING_GAMES = Integer.getInteger("night.settingGames", 600);
    private static final int DEPTH_THREE_GAMES = Integer.getInteger("night.depthThreeGames", 300);
    private static final int FINAL_GAMES = Integer.getInteger("night.finalGames", 600);
    private static final int BATCH = 50;                       // even: keeps the pairs together
    private static final int TIMING_POSITIONS = 6;             // x 21 rolls per setting
    private static final int CHAMPION_TIMING_POSITIONS = 20;

    private static final int SEED_OFFSET = 10_000;             // other dice than the earlier tests
    private static final int MAX_TURNS = 2000;
    private static final double Z_STOP = 2.576;                // 99%: stop a match early
    private static final double Z_REPORT = 1.96;               // 95%: reported intervals

    private static final Path FOLDER = Path.of("src/test/java/gameproject/ai");
    private static final Path GAMES_FILE = FOLDER.resolve("night_games.csv");
    private static final Path TIMING_FILE = FOLDER.resolve("night_timing.csv");
    private static final Path REPORT_FILE = FOLDER.resolve("night_report.md");
    private static final String GAMES_HEADER =
            "match,game,agentA,agentB,colorA,winner,points,turns,avgMsA,maxMsA,avgMsB,maxMsB";
    private static final String TIMING_HEADER = "run,setting,position,roll,ms";

    // existing files of AIAgentStrengthTest and AIAgentBenchmarkTest (written only if the header matches)
    private static final Path STRENGTH_FILE = FOLDER.resolve("strength.csv");
    private static final Path BENCHMARK_FILE = FOLDER.resolve("output.csv");
    private static final String STRENGTH_HEADER = "match,game,agentA,depthA,agentB,depthB,colorA,winner,points,turns,"
            + "msPerMoveA,msPerMoveB";
    private static final String BENCHMARK_HEADER = "algorithm,depth,improvements,board,currentPlayer,dice,probability,"
            + "legalFirstPlays,chosenMoves,nodeVisits,leafEvaluations,generatedTurns,duplicateTurns,roundCopies,"
            + "boardCopies,evaluatorCalls,cacheHits,cacheMisses,minNs,medianNs,averageNs,maxNs";
    private static final String SETTINGS_BOARD = "night-" + TIMING_POSITIONS + "-positions";
    private static final String CHAMPION_BOARD = "night-" + CHAMPION_TIMING_POSITIONS + "-positions";

    // ---------- candidates ----------

    /** Evaluator weights with a short name (same order as BoardEvaluator). */
    record Weights(String name, double[] values) {

        BoardEvaluator evaluator() {
            return new BoardEvaluator(normalized(values));
        }
    }

    private static final Weights EQUAL = new Weights("equal",
            new double[]{0.125, 0.125, 0.125, 0.125, 0.125, 0.125, 0.125, 0.125});
    private static final Weights TUNED_BEST = new Weights("tuned-best",
            new double[]{0.224, 0.152, 0.227, 0.041, 0.034, 0.088, 0.133, 0.101});
    private static final Weights TUNED_TOP_HALF = new Weights("tuned-tophalf",
            new double[]{0.22, 0.1672, 0.227, 0.025, 0.0544, 0.088, 0.1282, 0.0902});

    /** Search depth and move filter K. */
    record Setting(int depth, int moveFilter) {

        String label() {
            return "d" + depth + (moveFilter == AdvancedAgent.NO_FILTER ? "-full" : "-k" + moveFilter);
        }
    }

    /** Settings that are timed, cheapest first per depth. */
    private static final List<Setting> SETTINGS = List.of(
            new Setting(2, 2), new Setting(2, 3), new Setting(2, 5), new Setting(2, 8),
            new Setting(2, 10), new Setting(2, 12), new Setting(2, 16),
            new Setting(3, 2), new Setting(3, 3));

    /** One agent in the tournament: weights plus setting, e.g. "tuned-best/d2-k5". */
    record Contender(Weights weights, Setting setting) {

        String name() {
            return weights.name() + "/" + setting.label();
        }

        AdvancedAgent create() {
            return new AdvancedAgent(weights.evaluator(), setting.depth(), setting.moveFilter());
        }

        /**
         * Name in the visualizer's style, e.g. "depth2-move-filter-k5" (equal weights)
         * or "tuned-best-depth2-move-filter-k5", so its depth-2 and depth-3 panels find it.
         */
        String benchmarkName() {
            return NightTournamentTest.benchmarkName(weights, setting);
        }
    }

    private static String benchmarkName(Weights weights, Setting setting) {
        String prefix = weights == EQUAL ? "" : weights.name() + "-";
        String filter = setting.moveFilter() == AdvancedAgent.NO_FILTER ? "-full" : "-move-filter-k" + setting.moveFilter();
        return prefix + "depth" + setting.depth() + filter;
    }

    // ---------- results ----------

    private record GameResult(int game, boolean aIsWhite, boolean aWon, int points, int turns,
                              double avgMsA, double maxMsA, double avgMsB, double maxMsB) {
    }

    /** Summary of a match from the view of agent A. */
    private record MatchResult(String a, String b, int games, int winsA, int netPointsA,
                               double maxMsA, double maxMsB, String stopReason) {

        double winRate() {
            return games == 0 ? 0.5 : (double) winsA / games;
        }

        boolean provenBetter(double z) {
            return games > 0 && wilson(winsA, games, z)[0] > 0.5;
        }

        boolean provenWorse(double z) {
            return games > 0 && wilson(winsA, games, z)[1] < 0.5;
        }
    }

    /** One timed decision, kept until the setting is finished. */
    private record Decision(int position, Dice.DiceOutcome roll, long nanos, int legalPlays, int chosenPlays,
                            AdvancedAgent.Metrics metrics) {
    }

    /** Single-threaded timing of one setting. */
    private record Timing(Setting setting, int decisions, double meanMs, double p95Ms,
                          double maxMs, String slowest, boolean allowed, String note) {
    }

    // ---------- state of this run ----------

    private final long deadline = System.currentTimeMillis() + (long) (HOURS * 3_600_000);
    private final String runId = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    private final List<Timing> timings = new ArrayList<>();
    private final List<MatchResult> matches = new ArrayList<>();
    private final List<String> log = new ArrayList<>();
    private Timing championTiming;
    private Contender champion;
    private boolean outOfTime;
    private boolean writeStrength;     // false if strength.csv has other columns
    private boolean writeBenchmark;    // false if output.csv has other columns

    @Test
    void overnightTournament() throws Exception {
        prepareFile(GAMES_FILE, GAMES_HEADER);
        prepareFile(TIMING_FILE, TIMING_HEADER);
        writeStrength = canAppend(STRENGTH_FILE, STRENGTH_HEADER);
        writeBenchmark = canAppend(BENCHMARK_FILE, BENCHMARK_HEADER);
        say("Start: " + HOURS + " h, limit " + LIMIT_MS / 1000 + " s per move, " + THREADS + " threads");

        List<Round> positions = samplePositions(CHAMPION_TIMING_POSITIONS);

        // Stage 1: which settings are fast enough?
        List<Setting> allowed = timeSettings(positions.subList(0, TIMING_POSITIONS));
        writeReport();

        Contender start = new Contender(EQUAL, new Setting(2, 2));
        champion = start;
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        try {
            // Stage 2: coefficients, at the cheap setting
            for (Weights weights : List.of(TUNED_BEST, TUNED_TOP_HALF)) {
                challenge(pool, new Contender(weights, champion.setting()), COEFFICIENT_GAMES, "coefficients");
            }

            // Stage 3: settings ladder with the champion's weights
            for (Setting setting : allowed) {
                if (!setting.equals(champion.setting())) {
                    int games = setting.depth() >= 3 ? DEPTH_THREE_GAMES : SETTING_GAMES;
                    challenge(pool, new Contender(champion.weights(), setting), games, "setting");
                }
            }

            // Stage 4: final evidence against the starting agent and depth 1
            if (!champion.equals(start)) {
                playMatch(pool, champion, start, FINAL_GAMES);
            }
            playMatch(pool, champion, new Contender(EQUAL, new Setting(1, AdvancedAgent.NO_FILTER)), FINAL_GAMES);
        } finally {
            pool.shutdownNow();
        }

        // Stage 5: champion timing on more positions
        if (!outOfTime) {
            championTiming = time(champion.weights(), champion.setting(), positions, "champion", CHAMPION_BOARD);
        }
        say("Done. Champion: " + champion.name());
        writeReport();
    }

    // ---------- stage helpers ----------

    /**
     * Times every setting with equal weights; a setting slower than the limit stops
     * being timed at once, and so do all larger K of the same depth.
     *
     * @return the allowed settings, cheapest (by mean time) first
     */
    private List<Setting> timeSettings(List<Round> positions) throws IOException {
        Setting depthOne = new Setting(1, AdvancedAgent.NO_FILTER);
        time(EQUAL, depthOne, positions, "warmup", null);              // let the JIT compile first
        timings.add(time(EQUAL, depthOne, positions, runId, SETTINGS_BOARD)); // reference for the visualizer
        List<Setting> allowed = new ArrayList<>();
        int tooSlowDepth = -1;
        for (Setting setting : SETTINGS) {
            if (setting.depth() == tooSlowDepth) {
                timings.add(new Timing(setting, 0, 0, 0, 0, "", false, "skipped: a smaller K was too slow"));
                continue;
            }
            Timing timing = time(EQUAL, setting, positions, runId, SETTINGS_BOARD);
            timings.add(timing);
            if (timing.allowed()) {
                allowed.add(setting);
            } else {
                tooSlowDepth = setting.depth();
            }
        }
        allowed.sort((x, y) -> Double.compare(meanMs(x), meanMs(y)));
        return allowed;
    }

    private double meanMs(Setting setting) {
        for (Timing timing : timings) {
            if (timing.setting().equals(setting)) {
                return timing.meanMs();
            }
        }
        return Double.MAX_VALUE;
    }

    /**
     * Lets the challenger play the champion; it takes over when it wins more than half.
     */
    private void challenge(ExecutorService pool, Contender challenger, int games, String stage) throws Exception {
        MatchResult result = playMatch(pool, challenger, champion, games);
        if (result != null && result.games() > 0 && result.winRate() > 0.5) {
            say("New champion (" + stage + "): " + challenger.name());
            champion = challenger;
        }
    }

    /**
     * Plays batches of games until the 99% interval excludes 50%, the game limit is
     * reached or the time is up. Games already in night_games.csv are reused.
     *
     * @return the match summary, or null if there was no time left to start it
     */
    private MatchResult playMatch(ExecutorService pool, Contender a, Contender b, int maxGames) throws Exception {
        if (outOfTime || System.currentTimeMillis() > deadline) {
            outOfTime = true;
            say("Skipped (time is up): " + a.name() + " vs " + b.name());
            return null;
        }
        String match = a.name() + " vs " + b.name();
        Map<Integer, GameResult> results = readRecordedGames(match);
        say("Match " + match + " (" + results.size() + " games already recorded)");
        copyMissingToStrength(a, b, results);

        String stopReason = "game limit";
        int target = 0;
        while (target < maxGames) {
            target = Math.min(target + BATCH, maxGames);
            List<Future<GameResult>> running = new ArrayList<>();
            for (int game = 0; game < target; game++) {
                if (!results.containsKey(game)) {
                    int g = game;
                    running.add(pool.submit(() -> playAndRecord(match, a, b, g)));
                }
            }
            for (Future<GameResult> future : running) {
                GameResult result = future.get();
                if (result != null) {
                    results.put(result.game(), result);
                }
            }
            MatchResult summary = summarize(a, b, results, target, "");
            say(String.format(Locale.ROOT, "  %d games: A wins %.1f%% (95%%: %s)",
                    summary.games(), 100 * summary.winRate(), interval(summary, Z_REPORT)));
            if (System.currentTimeMillis() > deadline) {
                outOfTime = true;
                stopReason = "time is up";
                break;
            }
            if (summary.provenBetter(Z_STOP) || summary.provenWorse(Z_STOP)) {
                stopReason = "clear at 99%";
                break;
            }
        }
        MatchResult summary = summarize(a, b, results, target, stopReason);
        matches.add(summary);
        writeReport();
        return summary;
    }

    /** Plays one game (unless the time is up) and appends it to night_games.csv. */
    private GameResult playAndRecord(String match, Contender a, Contender b, int game) throws IOException {
        if (System.currentTimeMillis() > deadline) {
            return null;
        }
        GameResult result = playGame(a, b, game);
        String row = String.format(Locale.ROOT, "%s,%d,%s,%s,%s,%s,%d,%d,%.3f,%.3f,%.3f,%.3f%n",
                match, game, a.name(), b.name(), result.aIsWhite() ? "WHITE" : "BLACK",
                result.aWon() ? "A" : "B", result.points(), result.turns(),
                result.avgMsA(), result.maxMsA(), result.avgMsB(), result.maxMsB());
        synchronized (NightTournamentTest.class) {
            Files.writeString(GAMES_FILE, row, StandardOpenOption.APPEND);
            if (writeStrength) {
                Files.writeString(STRENGTH_FILE, strengthRow(a, b, result), StandardOpenOption.APPEND);
            }
        }
        return result;
    }

    /** Match name in strength.csv; "(night)" keeps it apart from matches with the old dice. */
    private static String strengthMatch(Contender a, Contender b) {
        return a.benchmarkName() + " vs " + b.benchmarkName() + " (night)";
    }

    private static String strengthRow(Contender a, Contender b, GameResult r) {
        return String.format(Locale.ROOT, "%s,%d,%s,%d,%s,%d,%s,%s,%d,%d,%.3f,%.3f%n",
                strengthMatch(a, b), r.game(), a.benchmarkName(), a.setting().depth(),
                b.benchmarkName(), b.setting().depth(), r.aIsWhite() ? "WHITE" : "BLACK",
                r.aWon() ? "A" : "B", r.points(), r.turns(), r.avgMsA(), r.avgMsB());
    }

    /**
     * Copies games that are in night_games.csv but not yet in strength.csv (e.g. the
     * run was stopped between the two writes), so strength.csv never misses or repeats a game.
     */
    private void copyMissingToStrength(Contender a, Contender b, Map<Integer, GameResult> results) throws IOException {
        if (!writeStrength || results.isEmpty()) {
            return;
        }
        String match = strengthMatch(a, b);
        java.util.Set<Integer> present = new java.util.HashSet<>();
        for (String line : Files.readAllLines(STRENGTH_FILE)) {
            String[] f = line.split(",", -1);
            if (f.length == 12 && f[0].equals(match)) {
                try {
                    present.add(Integer.parseInt(f[1]));
                } catch (NumberFormatException cutOffRow) {
                    // ignored, see readRecordedGames
                }
            }
        }
        StringBuilder rows = new StringBuilder();
        results.values().stream()
                .filter(r -> !present.contains(r.game()))
                .sorted((x, y) -> Integer.compare(x.game(), y.game()))
                .forEach(r -> rows.append(strengthRow(a, b, r)));
        if (!rows.isEmpty()) {
            Files.writeString(STRENGTH_FILE, rows.toString(), StandardOpenOption.APPEND);
        }
    }

    /** Uses only games 0 .. upTo-1, so the result does not depend on finishing order. */
    private static MatchResult summarize(Contender a, Contender b, Map<Integer, GameResult> results,
                                         int upTo, String stopReason) {
        int games = 0, wins = 0, points = 0;
        double maxA = 0, maxB = 0;
        for (GameResult r : results.values()) {
            if (r.game() < upTo) {
                games++;
                wins += r.aWon() ? 1 : 0;
                points += r.aWon() ? r.points() : -r.points();
                maxA = Math.max(maxA, r.maxMsA());
                maxB = Math.max(maxB, r.maxMsB());
            }
        }
        return new MatchResult(a.name(), b.name(), games, wins, points, maxA, maxB, stopReason);
    }

    // ---------- playing ----------

    /**
     * One full game, played like the GUI (one chooseMove per turn). Agent A is white in
     * even games; games 2k and 2k+1 use the same dice.
     */
    private static GameResult playGame(Contender a, Contender b, int game) {
        boolean aIsWhite = game % 2 == 0;
        Strategy agentA = a.create();
        Strategy agentB = b.create();
        Player white = new Player("White", Player.Color.WHITE, aIsWhite ? agentA : agentB);
        Player black = new Player("Black", Player.Color.BLACK, aIsWhite ? agentB : agentA);
        Round round = new Round(white, black, new Dice(new Random(SEED_OFFSET + game / 2)));
        round.determineFirstPlayer();

        long totalA = 0, totalB = 0, maxA = 0, maxB = 0;
        int movesA = 0, movesB = 0;
        for (int turn = 1; turn <= MAX_TURNS; turn++) {
            round.startTurn();
            List<Play> legal = round.getRequiredPlays(round.getMoves());
            if (!legal.isEmpty()) {
                Strategy mover = round.getCurrentPlayer().getStrategy();
                long start = System.nanoTime();
                List<Play> chosen = mover.chooseMove(legal, round);
                long nanos = System.nanoTime() - start;
                if (mover == agentA) {
                    totalA += nanos;
                    maxA = Math.max(maxA, nanos);
                    movesA++;
                } else {
                    totalB += nanos;
                    maxB = Math.max(maxB, nanos);
                    movesB++;
                }
                for (Play play : chosen) {
                    round.executePlay(play);
                }
            }
            if (round.checkWinConditions()) {
                boolean aWon = (round.getWinner() == white) == aIsWhite;
                return new GameResult(game, aIsWhite, aWon, round.getWinTypeValue(), turn,
                        ms(totalA) / Math.max(1, movesA), ms(maxA), ms(totalB) / Math.max(1, movesB), ms(maxB));
            }
            round.endTurn();
        }
        throw new IllegalStateException("Game " + game + " did not finish within " + MAX_TURNS + " turns");
    }

    // ---------- timing ----------

    /**
     * Positions to time: the opening and then every 6th turn of two games between
     * depth-1 agents. Each position is stored before the dice are thrown.
     */
    private static List<Round> samplePositions(int count) {
        List<Round> positions = new ArrayList<>();
        for (int seed = 1; positions.size() < count; seed++) {
            Player white = new Player("White", Player.Color.WHITE, new AdvancedAgent(EQUAL.evaluator(), 1));
            Player black = new Player("Black", Player.Color.BLACK, new AdvancedAgent(EQUAL.evaluator(), 1));
            Round round = new Round(white, black, new Dice(new Random(SEED_OFFSET + 5000 + seed)));
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

    /**
     * Times one decision per position and roll (single-threaded). Stops at the first
     * decision slower than the limit, because the setting is then not allowed anyway.
     */
    private Timing time(Weights weights, Setting setting, List<Round> positions, String run,
                        String board) throws IOException {
        List<Double> times = new ArrayList<>();
        List<Decision> decisions = new ArrayList<>();
        String slowest = "";
        double max = 0;
        StringBuilder rows = new StringBuilder();
        for (int p = 0; p < positions.size(); p++) {
            for (Dice.DiceOutcome roll : Dice.getDiceOutcomes()) {
                Round round = new Round(positions.get(p), new Dice(roll.die1(), roll.die2()));
                List<Play> legal = round.getRequiredPlays(round.getMoves());
                if (legal.isEmpty()) {
                    continue;
                }
                AdvancedAgent agent = new AdvancedAgent(weights.evaluator(), setting.depth(), setting.moveFilter());
                long start = System.nanoTime();
                List<Play> chosen = agent.chooseMove(legal, round);
                long nanos = System.nanoTime() - start;
                double ms = ms(nanos);
                times.add(ms);
                decisions.add(new Decision(p, roll, nanos, legal.size(), chosen.size(), agent.getLastMetrics()));
                String rollName = roll.die1() + "-" + roll.die2();
                rows.append(String.format(Locale.ROOT, "%s,%s,%d,%s,%.3f%n", run, setting.label(), p, rollName, ms));
                if (ms > max) {
                    max = ms;
                    slowest = "position " + p + ", roll " + rollName;
                }
                if (ms > LIMIT_MS) {
                    appendBenchmarkRows(weights, setting, board, decisions);
                    return finishTiming(setting, times, max, slowest, false, "stopped: over the limit", rows);
                }
            }
        }
        appendBenchmarkRows(weights, setting, board, decisions);
        return finishTiming(setting, times, max, slowest, true, "", rows);
    }

    /**
     * Adds the timing to output.csv in the benchmark's format: one row per roll, combining
     * that roll over all positions (min / median / average / max time, average counters).
     * maxNs is therefore the slowest decision of that roll.
     */
    private void appendBenchmarkRows(Weights weights, Setting setting, String board,
                                     List<Decision> decisions) throws IOException {
        if (!writeBenchmark || board == null || decisions.isEmpty()) {
            return;
        }
        StringBuilder rows = new StringBuilder();
        for (Dice.DiceOutcome roll : Dice.getDiceOutcomes()) {
            List<Decision> same = new ArrayList<>();
            for (Decision d : decisions) {
                if (d.roll().equals(roll)) {
                    same.add(d);
                }
            }
            if (same.isEmpty()) {
                continue;
            }
            long[] nanos = new long[same.size()];
            double[] sums = new double[9];
            for (int i = 0; i < same.size(); i++) {
                Decision d = same.get(i);
                AdvancedAgent.Metrics m = d.metrics();
                nanos[i] = d.nanos();
                double[] values = {d.legalPlays(), d.chosenPlays(), m.nodeVisits(), m.leafEvaluations(),
                        m.generatedTurns(), m.duplicateTurns(), m.roundCopies(), m.boardCopies(),
                        m.evaluatorCalls()};
                for (int j = 0; j < values.length; j++) {
                    sums[j] += values[j];
                }
            }
            Arrays.sort(nanos);
            long total = 0;
            for (long n : nanos) {
                total += n;
            }
            StringBuilder counters = new StringBuilder();
            for (double sum : sums) {
                counters.append(',').append(Math.round(sum / same.size()));
            }
            counters.append(",,");   // cacheHits, cacheMisses: no longer measured
            rows.append(String.format(Locale.ROOT, "ADVANCED,%d,%s,%s,mixed,%d-%d,%.6f%s,%d,%d,%.0f,%d%n",
                    setting.depth(), benchmarkName(weights, setting), board, roll.die1(), roll.die2(),
                    roll.probability(), counters, nanos[0], nanos[nanos.length / 2],
                    (double) total / nanos.length, nanos[nanos.length - 1]));
        }
        Files.writeString(BENCHMARK_FILE, rows.toString(), StandardOpenOption.APPEND);
    }

    private Timing finishTiming(Setting setting, List<Double> times, double max, String slowest,
                                boolean allowed, String note, StringBuilder rows) throws IOException {
        Files.writeString(TIMING_FILE, rows.toString(), StandardOpenOption.APPEND);
        List<Double> sorted = new ArrayList<>(times);
        Collections.sort(sorted);
        double mean = sorted.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double p95 = sorted.isEmpty() ? 0 : sorted.get((int) Math.ceil(0.95 * sorted.size()) - 1);
        Timing timing = new Timing(setting, times.size(), mean, p95, max, slowest, allowed, note);
        say(String.format(Locale.ROOT, "Timing %s: mean %.0f ms, p95 %.0f ms, max %.0f ms %s",
                setting.label(), mean, p95, max, allowed ? "(allowed)" : "(" + note + ")"));
        return timing;
    }

    // ---------- report ----------

    private void writeReport() throws IOException {
        StringBuilder md = new StringBuilder();
        md.append("# Night tournament ").append(runId).append("\n\n");
        md.append(String.format(Locale.ROOT, "Limit %.1f s per move, %d threads, %s.%n%n",
                LIMIT_MS / 1000, THREADS, outOfTime ? "stopped because the time was up" : "within the time budget"));

        if (champion != null) {
            md.append("## Champion: `").append(champion.name()).append("`\n\n");
            md.append("For App.createAIPlayer:\n\n```java\nnew AdvancedAgent(new BoardEvaluator(new double[]")
                    .append(Arrays.toString(normalized(champion.weights().values()))
                            .replace('[', '{').replace(']', '}'))
                    .append("), ").append(champion.setting().depth()).append(", ")
                    .append(champion.setting().moveFilter() == AdvancedAgent.NO_FILTER
                            ? "AdvancedAgent.NO_FILTER" : champion.setting().moveFilter())
                    .append(")\n```\n\n");
        }

        md.append("## Matches (win rate of A, 95% Wilson interval)\n\n");
        md.append("| A | B | games | A wins | 95% interval | verdict | points/game A | max ms A | max ms B | stopped |\n");
        md.append("|---|---|---:|---:|---|---|---:|---:|---:|---|\n");
        for (MatchResult m : matches) {
            String verdict = m.provenBetter(Z_REPORT) ? "**A better**"
                    : m.provenWorse(Z_REPORT) ? "**B better**" : "not significant";
            md.append(String.format(Locale.ROOT, "| %s | %s | %d | %.1f%% | %s | %s | %+.3f | %.0f | %.0f | %s |%n",
                    m.a(), m.b(), m.games(), 100 * m.winRate(), interval(m, Z_REPORT), verdict,
                    m.games() == 0 ? 0.0 : (double) m.netPointsA() / m.games(), m.maxMsA(), m.maxMsB(), m.stopReason()));
        }
        md.append("\nMax ms in games is measured while ").append(THREADS)
                .append(" games run at once, so it is higher than in the GUI.\n\n");

        md.append("## Time per decision (single-threaded, ").append(TIMING_POSITIONS)
                .append(" positions x 21 rolls, equal weights)\n\n");
        md.append("| setting | decisions | mean ms | p95 ms | max ms | slowest | allowed |\n");
        md.append("|---|---:|---:|---:|---:|---|---|\n");
        List<Timing> rows = new ArrayList<>(timings);
        if (championTiming != null) {
            rows.add(championTiming);
        }
        for (Timing t : rows) {
            String name = t == championTiming ? "champion " + t.setting().label() + " (" + CHAMPION_TIMING_POSITIONS + " positions)"
                    : t.setting().label();
            md.append(String.format(Locale.ROOT, "| %s | %d | %.0f | %.0f | %.0f | %s | %s |%n",
                    name, t.decisions(), t.meanMs(), t.p95Ms(), t.maxMs(), t.slowest(),
                    t.allowed() ? "yes" : "no, " + t.note()));
        }

        md.append("\n## Log\n\n```\n");
        synchronized (log) {
            log.forEach(line -> md.append(line).append('\n'));
        }
        md.append("```\n");
        Files.writeString(REPORT_FILE, md.toString());
    }

    // ---------- small helpers ----------

    /** Prints a line with the time and keeps it for the report's log. */
    private void say(String message) {
        String line = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "  " + message;
        System.out.println(line);
        synchronized (log) {
            log.add(line);
        }
    }

    /** Wilson score interval for a win rate. */
    private static double[] wilson(int wins, int games, double z) {
        double p = (double) wins / games;
        double z2 = z * z;
        double centre = (p + z2 / (2 * games)) / (1 + z2 / games);
        double margin = z * Math.sqrt(p * (1 - p) / games + z2 / (4.0 * games * games)) / (1 + z2 / games);
        return new double[]{centre - margin, centre + margin};
    }

    private static String interval(MatchResult m, double z) {
        if (m.games() == 0) {
            return "-";
        }
        double[] ci = wilson(m.winsA(), m.games(), z);
        return String.format(Locale.ROOT, "%.1f-%.1f%%", 100 * ci[0], 100 * ci[1]);
    }

    /** Scales the weights so they sum to exactly 1 (BoardEvaluator requires this). */
    private static double[] normalized(double[] weights) {
        double sum = 0;
        for (double w : weights) {
            sum += w;
        }
        double[] result = new double[weights.length];
        for (int i = 0; i < weights.length; i++) {
            result[i] = weights[i] / sum;
        }
        return result;
    }

    private static double ms(long nanos) {
        return nanos / 1_000_000.0;
    }

    private static double doubleProperty(String name, double defaultValue) {
        String value = System.getProperty(name);
        return value == null ? defaultValue : Double.parseDouble(value);
    }

    /** Creates the file with its header, or checks that an existing file has that header. */
    private static void prepareFile(Path file, String header) throws IOException {
        if (Files.notExists(file) || Files.size(file) == 0) {
            Files.writeString(file, header + System.lineSeparator(),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            return;
        }
        String content = Files.readString(file);
        if (!content.startsWith(header + System.lineSeparator()) && !content.startsWith(header + "\n")) {
            throw new IllegalStateException(file.getFileName() + " has other columns than expected; rename it first.");
        }
        if (!content.endsWith("\n")) {
            // the last row was cut off (e.g. IntelliJ was stopped while writing): end it, it will be skipped
            Files.writeString(file, System.lineSeparator(), StandardOpenOption.APPEND);
        }
    }

    /**
     * True if the existing file has exactly this header (then rows can be appended).
     * Otherwise the night run continues without that file and says so.
     */
    private boolean canAppend(Path file, String header) throws IOException {
        if (Files.notExists(file) || Files.size(file) == 0) {
            say("Not found, skipped: " + file.getFileName());
            return false;
        }
        String content = Files.readString(file);
        if (!content.startsWith(header + "\n") && !content.startsWith(header + "\r\n")) {
            say("Other columns than expected, skipped: " + file.getFileName());
            return false;
        }
        if (!content.endsWith("\n")) {
            Files.writeString(file, System.lineSeparator(), StandardOpenOption.APPEND);
        }
        return true;
    }

    /** Games of this match that are already in night_games.csv, by game number. */
    private static Map<Integer, GameResult> readRecordedGames(String match) throws IOException {
        Map<Integer, GameResult> results = new HashMap<>();
        for (String line : Files.readAllLines(GAMES_FILE)) {
            String[] f = line.split(",", -1);
            if (f.length == 12 && f[0].equals(match)) {
                try {
                    int game = Integer.parseInt(f[1]);
                    results.put(game, new GameResult(game, f[4].equals("WHITE"), f[5].equals("A"),
                            Integer.parseInt(f[6]), Integer.parseInt(f[7]), Double.parseDouble(f[8]),
                            Double.parseDouble(f[9]), Double.parseDouble(f[10]), Double.parseDouble(f[11])));
                } catch (NumberFormatException cutOffRow) {
                    // a row that was cut off is ignored, so that game is simply played again
                }
            }
        }
        return results;
    }
}
