package gameproject.ai;

// Written with Claude (Anthropic), reviewed by Kilian.
// If committed, disclose this assistance with:
// AI-Assisted-By: Claude

import gameproject.game.elements.Dice;
import gameproject.game.elements.Play;
import gameproject.game.elements.Round;
import gameproject.player.Player;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks AdvancedAgent.getOrderedTurns for many positions and all 21 dice rolls:
 * it must return exactly the same turns as getPossibleTurns (same final boards,
 * same count), sorted best first for the player who moves.
 */
class TurnOrderingTest {

    private static final int GAMES = 20;
    private static final int MAX_TURNS_PER_GAME = 200;
    private static final double EPS = 1e-12;

    @Test
    void orderedTurnsAreTheSameTurnsSortedBestFirst() {
        BoardEvaluator evaluator = new BoardEvaluator(equalWeights());
        AdvancedAgent agent = new AdvancedAgent(evaluator, 0);
        Random moveChoice = new Random(11);

        for (int game = 0; game < GAMES; game++) {
            Round round = newRound(game);

            for (int turn = 0; turn < MAX_TURNS_PER_GAME; turn++) {
                for (Dice.DiceOutcome dice : Dice.getDiceOutcomes()) {
                    Round position = new Round(round, new Dice(dice.die1(), dice.die2()));
                    String where = "game " + game + ", turn " + turn
                            + ", roll " + dice.die1() + "-" + dice.die2();

                    List<List<Play>> plain = agent.getPossibleTurns(position);
                    List<List<Play>> ordered = agent.getOrderedTurns(position);

                    // 1. Same turns: same number and same set of final boards.
                    assertEquals(plain.size(), ordered.size(), "Different number of turns at " + where);
                    assertEquals(finalBoards(plain, position), finalBoards(ordered, position),
                            "Different final boards at " + where);

                    // 2. Best first: the mover's evaluation never goes up along the list.
                    Player.Color mover = position.getCurrentPlayer().getColor();
                    double previous = Double.POSITIVE_INFINITY;
                    for (List<Play> orderedTurn : ordered) {
                        double score = evaluator.evaluate(play(orderedTurn, position).getCurrentBoard(), mover);
                        assertTrue(score <= previous + EPS, "Not sorted best first at " + where);
                        previous = score;
                    }
                }

                // Advance the game with a random legal turn.
                round.startTurn();
                while (round.getMoves().length > 0) {
                    List<Play> plays = round.getRequiredPlays(round.getMoves());
                    if (plays.isEmpty()) {
                        break;
                    }
                    round.executePlay(plays.get(moveChoice.nextInt(plays.size())));
                }
                if (round.checkWinConditions()) {
                    break;
                }
                round.endTurn();
            }
        }
    }

    /** Plays one turn on a copy of the round (executePlay also checks the plays are legal). */
    private static Round play(List<Play> turn, Round round) {
        Round copy = new Round(round);
        for (Play play : turn) {
            copy.executePlay(play);
        }
        return copy;
    }

    private static Set<String> finalBoards(List<List<Play>> turns, Round round) {
        Set<String> boards = new HashSet<>();
        for (List<Play> turn : turns) {
            boards.add(Arrays.toString(play(turn, round).getCurrentBoard().getCheckerCounts()));
        }
        return boards;
    }

    private static Round newRound(int seed) {
        Player white = new Player("White", Player.Color.WHITE, null);
        Player black = new Player("Black", Player.Color.BLACK, null);
        Round round = new Round(white, black, new Dice(new Random(seed)));
        round.determineFirstPlayer();
        return round;
    }

    private static double[] equalWeights() {
        double[] coefficients = new double[8];
        Arrays.fill(coefficients, 0.125);
        return coefficients;
    }
}
