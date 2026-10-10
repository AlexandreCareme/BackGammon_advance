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

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.function.Supplier;

/**
 * Measures playing strength: plays complete games between two AI agents and
 * appends one row per game to strength.csv, which the visualizer turns into
 * win rates with 95% confidence intervals.
 *
 * Games come in pairs: games 2k and 2k+1 use the same dice seed with the
 * colours swapped, so luck from the dice is shared between both agents.
 * Turns are played like the GUI does (one chooseMove call per turn).
 * There is no doubling cube: a game is worth 1 (single), 2 (gammon) or 3 (backgammon) points.
 *
 * Games already recorded for a match are skipped, so an interrupted run
 * continues where it stopped. Set the number of games with -Dstrength.games=N.
 */
class AIAgentStrengthTest {

    private static final Path OUTPUT_FILE =
            Path.of("src/test/java/gameproject/ai/strength.csv");
    private static final String CSV_HEADER =
            "match,game,agentA,depthA,agentB,depthB,colorA,winner,points,turns,"
                    + "msPerMoveA,msPerMoveB";
    private static final int DEFAULT_GAMES = 100;
    private static final int MAX_TURNS = 2000;

    /**
     * One side of a match.
     *
     * @param name label shown in the visualizer, e.g. "depth1-turn-dedup"
     * @param depth search depth, used to group matches in the visualizer
     * @param factory creates a fresh agent for every game
     */
    record AgentSpec(String name, int depth, Supplier<Strategy> factory) {
    }

    private record GameResult(String colorA, String winner, int points, int turns,
                              double msPerMoveA, double msPerMoveB) {
    }

    /**
     * Current depth 1 against current depth 0. Takes a few minutes for 100 games.
     * Template for new matches, e.g. once the move filter exists:
     * new AgentSpec("depth2-move-filter-k5", 2, () -> new AdvancedAgent(evaluator(), 2, 5))
     */
    @Test
    void depthOneVersusDepthZero() throws IOException {
        playMatchAndAppend(
                new AgentSpec("depth1-turn-dedup", 1, () -> new AdvancedAgent(evaluator(), 1)),
                new AgentSpec("depth0-turn-dedup", 0, () -> new AdvancedAgent(evaluator(), 0)),
                Integer.getInteger("strength.games", DEFAULT_GAMES)
        );
    }

    /**
     * Move filter K = 5 at depth 2 against full depth 1: does searching deeper
     * on only the best 5 turns make the agent stronger? 100 games by default
     * (-Dstrength.games=N); about 8 s per game, so roughly 15 minutes.
     */
    @Test
    void depthTwoFilterK5VersusDepthOne() throws IOException {
        playMatchAndAppend(
                new AgentSpec("depth2-move-filter-k5", 2, () -> new AdvancedAgent(evaluator(), 2, 5)),
                new AgentSpec("depth1-full", 1, () -> new AdvancedAgent(evaluator(), 1)),
                Integer.getInteger("strength.games", DEFAULT_GAMES)
        );
    }

    /**
     * Move filter K = 5 at depth 2 against full (unfiltered) depth 2: how much
     * strength does the filter cost? Around 50% means nothing is lost. Slow because
     * full depth 2 takes seconds per move: 40 games by default
     * (-Dstrength.fullDepthTwo.games=N), expect roughly 1-2 hours.
     */
    @Test
    void depthTwoFilterK5VersusDepthTwoFull() throws IOException {
        playMatchAndAppend(
                new AgentSpec("depth2-move-filter-k5", 2, () -> new AdvancedAgent(evaluator(), 2, 5)),
                new AgentSpec("depth2-full", 2, () -> new AdvancedAgent(evaluator(), 2)),
                Integer.getInteger("strength.fullDepthTwo.games", 40)
        );
    }

    /**
     * The other points of the speed vs strength trade-off: move filter K = 2, 3 and 8
     * at depth 2, each against full depth 1. Together with the K = 5 match this shows
     * from which K depth 2 is stronger than depth 1. 100 games each by default
     * (-Dstrength.games=N); roughly 10 minutes for K = 2, 20 for K = 3, 1.5 hours for K = 8.
     * Already played games are skipped, so it can be stopped and restarted.
     */
    @Test
    void depthTwoFilterSweepVersusDepthOne() throws IOException {
        for (int k : new int[]{2, 3, 8}) {
            playMatchAndAppend(
                    new AgentSpec("depth2-move-filter-k" + k, 2, () -> new AdvancedAgent(evaluator(), 2, k)),
                    new AgentSpec("depth1-full", 1, () -> new AdvancedAgent(evaluator(), 1)),
                    Integer.getInteger("strength.games", DEFAULT_GAMES)
            );
        }
    }

    /**
     * Plays games between a and b until the match has the requested number of
     * games in strength.csv, appending one row after every finished game.
     *
     * @param a the agent whose win rate is reported
     * @param b the opponent
     * @param games total number of games the match should have
     * @throws IOException if strength.csv cannot be read or written
     */
    static void playMatchAndAppend(AgentSpec a, AgentSpec b, int games) throws IOException {
        if (games < 1) {
            throw new IllegalArgumentException("A match needs at least one game");
        }
        String match = a.name() + " vs " + b.name();
        appendHeaderIfNeeded();
        for (int game = countRecordedGames(match); game < games; game++) {
            appendRow(match, game, a, b, playGame(a, b, game));
        }
    }

    /**
     * Plays one full game. Agent A is white in even games and black in odd games.
     *
     * @param a agent A
     * @param b agent B
     * @param game the game number, which also selects the dice seed
     * @return the result of the game
     */
    private static GameResult playGame(AgentSpec a, AgentSpec b, int game) {
        long seed = game / 2;                 // same dice for games 2k and 2k+1
        boolean aIsWhite = game % 2 == 0;     // colours swapped within each pair
        Strategy strategyA = a.factory().get();
        Strategy strategyB = b.factory().get();

        Player white = new Player("White", Player.Color.WHITE, aIsWhite ? strategyA : strategyB);
        Player black = new Player("Black", Player.Color.BLACK, aIsWhite ? strategyB : strategyA);
        Round round = new Round(white, black, new Dice(new Random(seed)));
        round.determineFirstPlayer();

        long nanosA = 0, nanosB = 0;
        int decisionsA = 0, decisionsB = 0;

        for (int turns = 1; turns <= MAX_TURNS; turns++) {
            round.startTurn();
            List<Play> legalPlays = round.getRequiredPlays(round.getMoves());

            if (!legalPlays.isEmpty()) {
                Strategy mover = round.getCurrentPlayer().getStrategy();
                long start = System.nanoTime();
                List<Play> chosen = mover.chooseMove(legalPlays, round);
                long elapsed = System.nanoTime() - start;

                if (mover == strategyA) {
                    nanosA += elapsed;
                    decisionsA++;
                } else {
                    nanosB += elapsed;
                    decisionsB++;
                }
                for (Play play : chosen) {
                    round.executePlay(play);
                }
            }

            if (round.checkWinConditions()) {
                boolean whiteWon = round.getWinner() == white;
                boolean aWon = whiteWon == aIsWhite;
                return new GameResult(
                        aIsWhite ? "WHITE" : "BLACK",
                        aWon ? "A" : "B",
                        round.getWinTypeValue(),
                        turns,
                        averageMs(nanosA, decisionsA),
                        averageMs(nanosB, decisionsB)
                );
            }
            round.endTurn();
        }
        throw new IllegalStateException("Game " + game + " did not finish within " + MAX_TURNS + " turns");
    }

    private static double averageMs(long nanos, int decisions) {
        return decisions == 0 ? 0.0 : nanos / 1_000_000.0 / decisions;
    }

    /**
     * Same weights as the game uses (App.createBoardEvaluator): equal weights, no noise.
     */
    static BoardEvaluator evaluator() {
        double[] coefficients = new double[8];
        for (int i = 0; i < 8; i++) {
            coefficients[i] = 0.125;
        }
        return new BoardEvaluator(coefficients);
    }

    private static int countRecordedGames(String match) throws IOException {
        if (Files.notExists(OUTPUT_FILE)) {
            return 0;
        }
        int count = 0;
        for (String line : Files.readAllLines(OUTPUT_FILE)) {
            if (line.split(",", -1)[0].equals(match)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Writes the header to a new or empty CSV, and refuses to append to a CSV
     * whose header differs from CSV_HEADER (same safety check as the benchmark).
     */
    private static void appendHeaderIfNeeded() throws IOException {
        if (Files.notExists(OUTPUT_FILE) || Files.size(OUTPUT_FILE) == 0) {
            Files.writeString(OUTPUT_FILE, CSV_HEADER + System.lineSeparator(),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            return;
        }
        String existingHeader;
        try (BufferedReader reader = Files.newBufferedReader(OUTPUT_FILE)) {
            existingHeader = reader.readLine();
        }
        if (!CSV_HEADER.equals(existingHeader)) {
            throw new IllegalStateException(
                    "strength.csv has different columns than CSV_HEADER. "
                            + "Migrate the file before appending new games.");
        }
    }

    private static void appendRow(String match, int game, AgentSpec a, AgentSpec b,
                                  GameResult result) throws IOException {
        String row = String.format(
                Locale.ROOT,
                "%s,%d,%s,%d,%s,%d,%s,%s,%d,%d,%.3f,%.3f%n",
                match,
                game,
                a.name(),
                a.depth(),
                b.name(),
                b.depth(),
                result.colorA(),
                result.winner(),
                result.points(),
                result.turns(),
                result.msPerMoveA(),
                result.msPerMoveB()
        );
        Files.writeString(OUTPUT_FILE, row, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }
}
