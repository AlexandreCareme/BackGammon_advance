package gameproject.ai;

import gameproject.game.elements.Dice;
import gameproject.game.elements.Round;
import gameproject.player.Player;
import gameproject.player.Strategy;

import java.util.Arrays;
import java.util.Random;


/**
 * Stand alone class which can be run in order to determine which coefficents work best for the objective value when playing
 * a certain type of A.I. against each other
 *
 * @author Diederick
 * @author Sven
 */
public class Coefficients {
    /**
     * The amount of coefficients used for evaluating a position
     */
    static int numberOfCoefficients = 8;
    /**
     * Create a random number with a fixed seed
     */
    private static Random RNG = new Random(15);
    /**
     * Create a random number for the dice used in simulating games with a fixed seed
     */
    private static Random randomDice = new Random(15);
    /**
     * The number of times a tournament  is ranked and renewed for one seed
     */
    static int improvementRounds = 6;
    /**
     * The number of games played between two strategies for determining their winrate
     */
    static int games = 100;
    /**
     * The number of teams in the tournament
     */
    static int teams = 8 ;
    /**
     * The amount by which two coefficients are changed in the first improvement round
     */
    static double maxTune = 0.1;
    /**
     * The factor by which the change becomes smaller after every improvement round
     */
    static double decayTune = 0.7;
    /**
     * The smallest amount by which two coefficients are changed
     */
    static double minTune = 0.02;

    /**
     * Runs a tournament based search of strategy vectors and checks the best strategy against the start weights
     */
    public static void main(String[] args) {
        int seeds = 4;
        double[][] finalComp = new double[seeds][];
        for(int k =0; k < seeds; k++ ){
            RNG = new Random(k);
            randomDice = new Random(k);
            double[][] rank  = Coefficients.createRanking();
            for(int i = 0; i < improvementRounds; i++){
                rank = Coefficients.ranking(rank);
                rank = Coefficients.updateRanking(rank, Coefficients.tuneMagnitude(i));
            }
            rank = Coefficients.ranking(rank);
            Coefficients.rankStats(rank);
            finalComp[k] = rank[0];
        }
        games = 2000;
        double[][] finals = Coefficients.ranking(finalComp);
        System.out.println("Final ranking:");
        for (int i = 0; i < finals.length; i++) {
            System.out.println((i + 1) + ". " + Arrays.toString(finals[i]));
        }

        System.out.println("Final winner vs start weights: " + winRate(finals[0], startWeight()));
        games = 100;

    }

    /**
     * Builds an equally weighted strategy vector.
     * @return
     */
    public static double[] startWeight(){
        double[] weights = new double[numberOfCoefficients];
        Arrays.fill(weights, 0, numberOfCoefficients, 1.0/numberOfCoefficients);
        return Coefficients.normalize(weights);
    }

    /**
     * Builds a randomly weighted strategy vector.
     * @return array with random weight coefficients.
     */
    public static double[] randomWeights(){
        double[] weights = new double[numberOfCoefficients];
        for(int i = 0; i < numberOfCoefficients; i++){
            weights[i] = RNG.nextDouble();
        }
        return Coefficients.normalize(weights);
    }

    /**
     * Builds a matrix consisting of different strategies which are going to play each other.
     * @return
     */
    static double[][] createRanking() {
        double[][] rank = new double[teams][];
        rank[0] = startWeight();
        for (int i = 1; i < teams; i++) {
            rank[i] = randomWeights();
        }
        return rank;
    }

    /**
     * Plays a tournament between the strategies and after the tournament it ranks the strategies from best to worse.
     *
     * @param rank matrix consisting of the competing strategies.
     * @return
     */
    public static double[][] ranking(double[][] rank){
        int h = rank.length;
        double[] score = new double[h];
        for (int i = 0; i < h; i++) {
            for (int j = i + 1; j < h; j++) {
                double r = winRate(rank[i], rank[j]);
                score[i] += r;
                score[j] += 1 - r;
            }
        }

        Integer[] order = new Integer[h];
        for (int i = 0; i < h; i++) {
            order[i] = i;
        }
        Arrays.sort(order, (a, b) -> Double.compare(score[b], score[a]));

        double[][] sortedRanking = new double[h][];
        double[] sortedScore = new double[h];
        for (int i = 0; i < h; i++) {
            sortedRanking[i] = rank[order[i]];
            sortedScore[i] = score[order[i]];
        }

        return sortedRanking;

    }

    /**
     * Prints the best vector, the average of the best half vector of a ranked population.
     * It then winrates of the best vector and of the average against the equal weights, and of the best vector against the average.
     *
     * @param rank A ranked tournament
     */
    public static void rankStats(double[][] rank){
        double[] bestCoefficients = rank[0];
        double[] worstCoefficients = rank[teams - 1];
        int half = teams / 2;

        double[] topHalfAverageCoefficients = new double[numberOfCoefficients];
        for (int i = 0; i < half; i++) {
            for (int j = 0; j < numberOfCoefficients; j++) {
                topHalfAverageCoefficients[j] += rank[i][j];
            }
        }
        for (int k = 0; k < numberOfCoefficients; k++) {
            topHalfAverageCoefficients[k] /= half;
        }
        topHalfAverageCoefficients = normalize(topHalfAverageCoefficients);
        System.out.println("Best: " + Arrays.toString(bestCoefficients));
        System.out.println("Top half Average: " + Arrays.toString(topHalfAverageCoefficients));
        System.out.println("worst: " + Arrays.toString(worstCoefficients));
        games = 1000;
        System.out.println("Best vs start weights: " + winRate(bestCoefficients, startWeight()));
        System.out.println("Best vs topHalf average: " + winRate(bestCoefficients, topHalfAverageCoefficients));
        //System.out.println("Best vs worst weights: " + winRate(bestCoefficients, worstCoefficients));
        //System.out.println("tophalf average average vs worst: " + winRate(topHalfAverageCoefficients, worstCoefficients ));
        System.out.println("Top half average vs start: " + winRate(topHalfAverageCoefficients, startWeight()));
        games =100;
    }

    /**
     * Builds a new tournament: keeps the top half unchanged, while tuning the bottom half.
     * @param sorted the current tournament standings
     * @param tune stepsize of changing the bottom half.
     * @return
     */
    public static double[][] updateRanking(double[][] sorted, double tune) {
        // Crate a ranking
        double[][] update = new double[teams][];
        int winners = teams / 2;

        for (int i = 0; i < winners; i++) {
            update[i] = sorted[i];
        }
        for (int i = winners; i < teams - 1; i++) {
            double[] parent = sorted[i - winners];
            update[i] = tuning(parent, tune);
        }
        for (int i = teams - 1; i < teams; i++) {
            update[i] = randomWeights();
        }

        return update;
    }


    /**
     * Plays games between two strategies and reports the ratio of wins.
     *
     * @param best The vector whose winrate is measured
     * @param opponent The vector played against
     * @return
     */
    public static double winRate(double[] best, double[] opponent){
    int wins = 0;

        for (int i = 0; i < games; i++){
            // Create Strategies for both players
            Strategy bestStrategy = new AdvancedAgent(new BoardEvaluator(best),0);
            Strategy opponentStrategy = new AdvancedAgent(new BoardEvaluator(opponent), 0);

        // Alternate colors so neither strategy always plays White
        Player bestPlayer;
        Player whitePlayer;
        Player blackPlayer;

        if (i % 2 == 0) {
            whitePlayer = new Player(
                    "Best",
                    Player.Color.WHITE,
                    bestStrategy
            );
            blackPlayer = new Player(
                    "Opponent",
                    Player.Color.BLACK,
                    opponentStrategy
            );
            bestPlayer = whitePlayer;
        } else {
            whitePlayer = new Player(
                    "Opponent",
                    Player.Color.WHITE,
                    opponentStrategy
            );
            blackPlayer = new Player(
                    "Best",
                    Player.Color.BLACK,
                    bestStrategy
            );
            bestPlayer = blackPlayer;
        }

        // Play the game and determine winner
        Round round = new Round(
                whitePlayer,
                blackPlayer,
                new Dice(randomDice)
        );

        round.startRound();

        if (round.getWinner() == bestPlayer) {
            wins++;
        }
    }

    return (double) wins / games;
}

    /**
     * The amount by which vectors are tuned, which decreases as the tournament evolves.
     *
     * @param decayStep #evolutions of the tournament standings we have had.
     * @return
     */
    static double tuneMagnitude(int decayStep) {
        return Math.max(minTune, maxTune * Math.pow(decayTune, decayStep));
    }

    /**
     * Builds a copy of a strategy, and changes the random coefficients.
     *
     * @param best The strategy being changed.
     * @param tune The magnitude of change off coefficients.
     * @return a tuned array of coefficients
     */
    public static double[] tuning(double[] best, double tune){
        double[] newBest = new double[numberOfCoefficients];
        boolean inBounds = true;

        while (inBounds) {
            for (int i = 0; i < numberOfCoefficients; i++) {
                newBest[i] = best[i];
            }

            // Determine which variables are tuned (one, up other down)
            int up = RNG.nextInt(numberOfCoefficients);
            int down = up;
            while (down == up) {
                down = RNG.nextInt(numberOfCoefficients);
            }

            // Tune
            newBest[up] += tune;
            newBest[down] -= tune;

            // Check violations (above 0)
            if (newBest [down] >= 0) {
                inBounds = false;
            }
            else {
                newBest[up] -= tune;
                newBest[down] += tune;
            }
        }
        return normalize(newBest);
        }


    /**
     * normalizes the weights of an array so that the sum off all inputs equals one.
     * Used for setting bounds of objective values in the BoardEvaluator class.
     * Rounds the coefficients on three digits
     *
     * @param weights list of original coefficients (weights).
     * @return normalized list of these coefficients
     */
    public static double[] normalize(double[] weights) {
        // Determine the length of the input array
        double initialSum = 0;
        for (int i = 0; i < numberOfCoefficients; i++) {
            initialSum += weights[i];
        }

        // Create a new array and initialize a new sum.
        double[] result = new double[numberOfCoefficients];
        double newSum = 0;

        // Normalize each coefficient apart from the last one and keep track of the current sum.
        for (int i = 0; i < numberOfCoefficients - 1; i++) {
            result[i] = Math.round((weights[i] / initialSum) * 1000.0) / 1000.0;
            newSum += result[i];
        }

        // Set final coefficient to make the sum of coefficients exactly one.
        result[numberOfCoefficients - 1] = Math.round((1.0 - newSum) * 1000.0) / 1000.0;

        return result;
    }
}



