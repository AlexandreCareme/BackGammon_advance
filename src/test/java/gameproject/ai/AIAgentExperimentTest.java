package gameproject.ai;

// Written with Claude (Anthropic), reviewed by Kilian.
// If committed, disclose this assistance with:
// AI-Assisted-By: Claude

import gameproject.ai.AIAgentStrengthTest.AgentSpec;
import org.junit.jupiter.api.Test;

import java.io.IOException;

/**
 * One test per experiment: each one measures the time of an agent setting on all
 * 21 dice rolls (appended to output.csv) and then plays a strength match against
 * a reference agent (appended to strength.csv). Both parts use the same label,
 * so the visualizer links timing and strength automatically.
 *
 * Everything already recorded is skipped, so an experiment can be stopped and
 * restarted. Overrides: -Dexperiment.warmupRuns, -Dexperiment.measuredRuns and
 * -Dexperiment.games (applies to the experiment you run).
 */
class AIAgentExperimentTest {

    /**
     * An AdvancedAgent setting with the label used in both CSV files.
     *
     * @param label name in output.csv ("improvements") and strength.csv ("agentA"/"agentB")
     * @param depth search depth
     * @param moveFilter K for the move filter, or AdvancedAgent.NO_FILTER
     */
    record AgentSetting(String label, int depth, int moveFilter) {

        AdvancedAgent create() {
            return new AdvancedAgent(AIAgentStrengthTest.evaluator(), depth, moveFilter);
        }

        AgentSpec spec() {
            return new AgentSpec(label, depth, this::create);
        }
    }

    /** Reference for depth-2 settings: full depth 1. */
    private static final AgentSetting DEPTH_ONE =
            new AgentSetting("depth1-full", 1, AdvancedAgent.NO_FILTER);

    /** Reference for depth-3 settings: the best depth-2 setting (proven stronger than depth 1). */
    private static final AgentSetting DEPTH_TWO_K5 =
            new AgentSetting("depth2-move-filter-k5", 2, 5);

    /** E1: depth 2, K = 2 against depth 1. About 25 minutes. */
    @Test
    void depthTwoK2VersusDepthOne() throws IOException {
        runExperiment(new AgentSetting("depth2-move-filter-k2", 2, 2), DEPTH_ONE, 2, 5, 200);
    }

    /** E2: depth 2, K = 3 against depth 1. About 25 minutes. */
    @Test
    void depthTwoK3VersusDepthOne() throws IOException {
        runExperiment(new AgentSetting("depth2-move-filter-k3", 2, 3), DEPTH_ONE, 2, 5, 200);
    }

    /**
     * E3: depth 3, K = 2 against the best depth-2 setting (K = 5).
     * Timing with 0 warmup and 1 measured run. About 4 hours for 100 games.
     */
    @Test
    void depthThreeK2VersusDepthTwoK5() throws IOException {
        runExperiment(new AgentSetting("depth3-move-filter-k2", 3, 2), DEPTH_TWO_K5, 0, 1, 100);
    }

    /** E4: depth 3, K = 3, timing only (a strength match would take about 10 hours). About 5 minutes. */
    @Test
    void depthThreeK3TimingOnly() throws IOException {
        runExperiment(new AgentSetting("depth3-move-filter-k3", 3, 3), null, 0, 1, 0);
    }

    /**
     * Measures the candidate on all 21 dice rolls, then plays it against the opponent.
     *
     * @param candidate the setting being evaluated
     * @param opponent the reference agent, or null for timing only
     * @param warmupRuns default warmup runs per dice roll
     * @param measuredRuns default measured runs per dice roll
     * @param games default number of strength games (0 = timing only)
     */
    private static void runExperiment(AgentSetting candidate, AgentSetting opponent,
                                      int warmupRuns, int measuredRuns, int games) throws IOException {
        AIAgentBenchmarkTest.benchmarkAndAppend(
                AIAgentBenchmarkTest.Algorithm.ADVANCED,
                candidate.depth(),
                candidate.label(),
                "experiment.warmupRuns",
                "experiment.measuredRuns",
                warmupRuns,
                measuredRuns,
                dice -> true,
                candidate.moveFilter()
        );

        if (opponent != null && games > 0) {
            AIAgentStrengthTest.playMatchAndAppend(
                    candidate.spec(),
                    opponent.spec(),
                    Integer.getInteger("experiment.games", games)
            );
        }
    }
}
