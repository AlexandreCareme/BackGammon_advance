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

/**
 * Checks that AdvancedAgent.getPossibleTurns produces exactly the turns the game
 * rules allow. The reference builds every turn step by step with
 * Round.getRequiredPlays (the game's own rules), so both must end on the same
 * set of boards, for many positions and all 21 dice rolls.
 */
class TurnGenerationTest {

    private static final int GAMES = 20;
    private static final int MAX_TURNS_PER_GAME = 200;

    @Test
    void generatedTurnsMatchTheGameRules() {
        AdvancedAgent agent = new AdvancedAgent(new BoardEvaluator(equalWeights()), 0);
        Random moveChoice = new Random(7);

        for (int game = 0; game < GAMES; game++) {
            Round round = newRound(game);

            for (int turn = 0; turn < MAX_TURNS_PER_GAME; turn++) {
                for (Dice.DiceOutcome dice : Dice.getDiceOutcomes()) {
                    Round position = new Round(round, new Dice(dice.die1(), dice.die2()));

                    List<List<Play>> turns = agent.getPossibleTurns(position);
                    Set<String> actual = finalBoards(turns, position);
                    Set<String> expected = new HashSet<>();
                    collectExpectedBoards(position, 0, expected);

                    String where = "game " + game + ", turn " + turn
                            + ", roll " + dice.die1() + "-" + dice.die2();
                    assertEquals(expected, actual, "Different final boards at " + where);
                    assertEquals(actual.size(), turns.size(), "Duplicate final board at " + where);
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

    /**
     * Reference implementation: plays every required play on a copy of the round,
     * recursively, and records the board when no further play is required.
     */
    private void collectExpectedBoards(Round round, int playsMade, Set<String> boards) {
        List<Play> plays = round.getRequiredPlays(round.getMoves());
        if (plays.isEmpty()) {
            if (playsMade > 0) {
                boards.add(Arrays.toString(round.getCurrentBoard().getCheckerCounts()));
            }
            return;
        }
        for (Play play : plays) {
            Round next = new Round(round);
            next.executePlay(play);
            collectExpectedBoards(next, playsMade + 1, boards);
        }
    }

    /**
     * Plays every turn on its own copy of the round (executePlay also checks
     * that each play is legal) and collects the resulting boards.
     */
    private Set<String> finalBoards(List<List<Play>> turns, Round round) {
        Set<String> boards = new HashSet<>();
        for (List<Play> turn : turns) {
            Round copy = new Round(round);
            for (Play play : turn) {
                copy.executePlay(play);
            }
            boards.add(Arrays.toString(copy.getCurrentBoard().getCheckerCounts()));
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
        for (int i = 0; i < 8; i++) {
            coefficients[i] = 0.125;
        }
        return coefficients;
    }
}
