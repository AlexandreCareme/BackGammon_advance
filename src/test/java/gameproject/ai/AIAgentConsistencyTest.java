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

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies that the depth-zero advanced search makes the same decision as
 * EasyAgent on the standard starting board.
 */
class AIAgentConsistencyTest {

    @Test
    void easyAndAdvancedDepthZeroChooseTheSameDecisionOnTheStandardBoard() {
        for (Dice.DiceOutcome dice : Dice.getDiceOutcomes()) {
            List<Play> easyMoves = chooseMoves(
                    new EasyAgent(new BoardEvaluator(deterministicCoefficients())),
                    dice
            );
            List<Play> advancedMoves = chooseMoves(
                    new AdvancedAgent(
                            new BoardEvaluator(deterministicCoefficients()),
                            0
                    ),
                    dice
            );

            assertSameMoves(easyMoves, advancedMoves, dice);
        }
    }

    private List<Play> chooseMoves(
            Strategy strategy,
            Dice.DiceOutcome dice) {

        Player white = new Player("White", Player.Color.WHITE, strategy);
        Player black = new Player("Black", Player.Color.BLACK, strategy);

        Round round = new Round(
                white,
                black,
                new Dice(1, 2, new FixedRandom(0, 1))
        );
        round.determineFirstPlayer();

        Round benchmarkRound = new Round(
                round,
                new Dice(dice.die1(), dice.die2())
        );
        List<Play> possiblePlays =
                benchmarkRound.getRequiredPlays(benchmarkRound.getMoves());

        return strategy.chooseMove(possiblePlays, benchmarkRound);
    }

    private void assertSameMoves(
            List<Play> expected,
            List<Play> actual,
            Dice.DiceOutcome dice) {

        String roll = dice.die1() + "-" + dice.die2();
        assertEquals(expected.size(), actual.size(),
                "Different move count for dice " + roll);

        for (int i = 0; i < expected.size(); i++) {
            Play expectedPlay = expected.get(i);
            Play actualPlay = actual.get(i);

            assertEquals(expectedPlay.getType(), actualPlay.getType(),
                    "Different play type at index " + i + " for dice " + roll);
            assertEquals(expectedPlay.getInitialPoint(), actualPlay.getInitialPoint(),
                    "Different starting point at index " + i + " for dice " + roll);
            assertEquals(expectedPlay.getForwardPoint(), actualPlay.getForwardPoint(),
                    "Different destination at index " + i + " for dice " + roll);
            assertEquals(expectedPlay.getDieUsed(), actualPlay.getDieUsed(),
                    "Different die used at index " + i + " for dice " + roll);
        }
    }

    private static double[] deterministicCoefficients() {
        double[] coefficients = new double[8];
        double equalWeight = 1.0 / 8.0;

        for (int i = 0; i < 8; i++) {
            coefficients[i] = equalWeight;
        }

        return coefficients;
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
                throw new IllegalStateException(
                        "Opening roll requested too many values"
                );
            }
            return values[index++];
        }
    }
}
