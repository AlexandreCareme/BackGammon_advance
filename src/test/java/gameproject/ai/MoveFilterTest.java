package gameproject.ai;

// Written with Claude (Anthropic), reviewed by Kilian.
// If committed, disclose this assistance with:
// AI-Assisted-By: Claude

import gameproject.game.elements.Dice;
import gameproject.game.elements.Play;
import gameproject.game.elements.Round;
import gameproject.player.Player;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sanity checks for the move filter (new AdvancedAgent(evaluator, depth, K)).
 */
class MoveFilterTest {

    private static final double EPS = 1e-9;

    @Test
    void rejectsFilterSmallerThanOne() {
        assertThrows(IllegalArgumentException.class,
                () -> new AdvancedAgent(new BoardEvaluator(equalWeights()), 2, 0));
    }

    /**
     * At depth 0 all children are leaves, so the filter is never applied:
     * even K = 1 must choose exactly the same turn as the unfiltered agent.
     */
    @Test
    void depthZeroIsNotAffectedByTheFilter() {
        for (Dice.DiceOutcome dice : Dice.getDiceOutcomes()) {
            Round round = openingRound(dice.die1(), dice.die2());
            List<Play> unfiltered = choose(new AdvancedAgent(new BoardEvaluator(equalWeights()), 0), round);
            List<Play> filtered = choose(new AdvancedAgent(new BoardEvaluator(equalWeights()), 0, 1), round);
            assertEquals(boardAfter(unfiltered, round), boardAfter(filtered, round),
                    "Depth 0 changed for roll " + dice.die1() + "-" + dice.die2());
        }
    }

    /**
     * A K larger than any number of turns keeps every turn, only in a different
     * order. The chosen turn must therefore be worth exactly the same as the
     * unfiltered choice. (It can be a different turn when two turns are worth
     * exactly the same, because the first one found wins a tie.)
     */
    @Test
    void hugeFilterChoosesTurnsWorthTheSameAsNoFilterAtDepthOne() throws Exception {
        Random moveChoice = new Random(5);
        int decisions = 0;

        for (int game = 0; game < 4; game++) {
            Round round = newRound(200 + game);
            for (int turn = 0; turn < 40; turn++) {
                round.startTurn();
                List<Play> legal = round.getRequiredPlays(round.getMoves());
                if (!legal.isEmpty()) {
                    decisions++;
                    assertSameValue(round, 1, "game " + game + ", turn " + turn);
                    playRandomTurn(round, moveChoice);
                }
                if (round.checkWinConditions()) {
                    break;
                }
                round.endTurn();
            }
        }
        assertTrue(decisions > 100, "Too few decisions checked: " + decisions);
    }

    /** The same check at depth 2, on a few opening rolls (takes some seconds each). */
    @Test
    void hugeFilterChoosesTurnsWorthTheSameAsNoFilterAtDepthTwo() throws Exception {
        int[][] rolls = {{1, 5}, {3, 4}, {6, 2}};
        for (int[] roll : rolls) {
            Round round = openingRound(roll[0], roll[1]);
            assertSameValue(round, 2, "opening roll " + roll[0] + "-" + roll[1]);
        }
    }

    /** With a small K the agent must search fewer nodes, and still return a legal turn. */
    @Test
    void smallFilterSearchesLessAndStillPlaysALegalTurn() {
        Round round = openingRound(1, 1);

        AdvancedAgent unfiltered = new AdvancedAgent(new BoardEvaluator(equalWeights()), 1);
        AdvancedAgent filtered = new AdvancedAgent(new BoardEvaluator(equalWeights()), 1, 3);
        choose(unfiltered, round);
        List<Play> chosen = choose(filtered, round);

        assertTrue(filtered.getLastMetrics().nodeVisits() < unfiltered.getLastMetrics().nodeVisits(),
                "Filter did not reduce the search");

        Set<String> legalBoards = new HashSet<>();
        for (List<Play> turn : unfiltered.getPossibleTurns(round)) {
            legalBoards.add(boardAfter(turn, round));
        }
        assertTrue(legalBoards.contains(boardAfter(chosen, round)), "Filtered agent chose an illegal turn");
    }

    // ---------- helpers ----------

    /**
     * Lets an unfiltered agent and a K = 1000 agent choose a turn for this round.
     * If they pick different turns, both turns must have the same search value.
     */
    private void assertSameValue(Round round, int depth, String where) throws Exception {
        BoardEvaluator evaluator = new BoardEvaluator(equalWeights());
        List<Play> unfiltered = choose(new AdvancedAgent(evaluator, depth), round);
        List<Play> hugeFilter = choose(new AdvancedAgent(evaluator, depth, 1000), round);

        if (!boardAfter(unfiltered, round).equals(boardAfter(hugeFilter, round))) {
            AdvancedAgent reference = new AdvancedAgent(evaluator, depth);
            assertEquals(searchValue(reference, unfiltered, round, depth),
                    searchValue(reference, hugeFilter, round, depth), EPS,
                    "Different value at " + where);
        }
    }

    /** The value chooseMove gives a turn: play it, switch player, search to the given depth. */
    private double searchValue(AdvancedAgent agent, List<Play> turn, Round round, int depth) throws Exception {
        Player.Color aiColor = round.getCurrentPlayer().getColor();
        Round after = new Round(round);
        for (Play play : turn) {
            after.executePlay(play);
        }
        after.switchPlayer();   // applyTurnForSearch also switches the player after a turn

        Method search = AdvancedAgent.class.getDeclaredMethod("search", Round.class, Player.Color.class, int.class);
        search.setAccessible(true);
        return (double) search.invoke(agent, after, aiColor, depth);
    }

    private static List<Play> choose(AdvancedAgent agent, Round round) {
        return agent.chooseMove(round.getRequiredPlays(round.getMoves()), round);
    }

    private static String boardAfter(List<Play> turn, Round round) {
        Round copy = new Round(round);
        for (Play play : turn) {
            copy.executePlay(play);
        }
        return Arrays.toString(copy.getCurrentBoard().getCheckerCounts());
    }

    private static void playRandomTurn(Round round, Random random) {
        while (round.getMoves().length > 0) {
            List<Play> plays = round.getRequiredPlays(round.getMoves());
            if (plays.isEmpty()) {
                break;
            }
            round.executePlay(plays.get(random.nextInt(plays.size())));
        }
    }

    private static Round newRound(int seed) {
        Player white = new Player("White", Player.Color.WHITE, null);
        Player black = new Player("Black", Player.Color.BLACK, null);
        Round round = new Round(white, black, new Dice(new Random(seed)));
        round.determineFirstPlayer();
        return round;
    }

    /** The standard starting position with the given roll for the player to move. */
    private static Round openingRound(int die1, int die2) {
        Round round = newRound(0);
        return new Round(round, new Dice(die1, die2));
    }

    private static double[] equalWeights() {
        double[] coefficients = new double[8];
        Arrays.fill(coefficients, 0.125);
        return coefficients;
    }
}
