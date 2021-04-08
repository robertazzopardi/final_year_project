package intelligence.Maddpg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.NDArrayIndex;
import robots.Action;
import robots.Agent;
import robots.Hunter;
import robots.RobotController;
import robots.StepObs;

public class Maddpg {

    private static final int NUM_AGENTS = 4;

    private final ReplayBuffer replayBuffer;

    private final Hunter[] agents;

    private final RobotController robotController;

    private final int maxEpisode;
    private final int maxStep;
    private final int batchSize;

    private final Integer[] ones = new Integer[NUM_AGENTS];
    private final Integer[] zeros = new Integer[NUM_AGENTS];

    public Maddpg(final int cap, final Hunter[] agents, final RobotController controller,
            final int maxEpisode, final int maxStep, final int batchSize) {
        this.replayBuffer = new ReplayBuffer(cap);
        this.agents = agents;
        this.robotController = controller;
        this.maxEpisode = maxEpisode;
        this.maxStep = maxStep;
        this.batchSize = batchSize;

        Arrays.fill(ones, 1);
        Arrays.fill(zeros, 0);
    }

    public Action[] getActions(final Boolean[][] states) {
        final Action[] actions = new Action[NUM_AGENTS];

        for (int i = 0; i < NUM_AGENTS; i++) {
            actions[i] = agents[i].getAction(states[i]);
        }

        return actions;
    }

    public void update(final int batchSize) {
        final Sample exp = replayBuffer.sample(batchSize);

        for (int i = 0; i < NUM_AGENTS; i++) {
            final List<Boolean[]> obsBatchI = exp.obsBatch.get(i);
            final List<Action> indivActionBatchI = exp.indivActionBatch.get(i);
            final List<Double> indivRewardBatchI = exp.indivRewardBatch.get(i);
            final Boolean[][] nextObsBatchI =
                    exp.nextObsBatch.get(i).stream().map(j -> j).toArray(Boolean[][]::new);

            // System.out.println(nextObsBatchI.size() + " fds");

            final List<Action[]> nextGlobalActions = new ArrayList<>();

            for (final Hunter agent : agents) {
                final INDArray indivNextAction =
                        agent.actor.forward(Nd4j.createFromArray(nextObsBatchI));


                int rowsCount = indivNextAction.rows();
                Action[] batchActions = new Action[rowsCount];
                for (int row = 0; row < rowsCount; row++) {
                    INDArray y = indivNextAction.get(NDArrayIndex.point(row), NDArrayIndex.all());
                    batchActions[row] =
                            Action.getActionByIndex(agent.getMaxValueIndex(y.toFloatVector()));

                    // nextGlobalActions.add(
                    // Action.getActionByIndex(agent.getMaxValueIndex(y.toFloatVector())));
                }

                nextGlobalActions.add(batchActions);
            }
            // System.out.println(nextGlobalActions.get(0).length);
            // System.out.println(exp.globalStateBatch.size() + " " + exp.globalActionsBatch.size()
            // + " " + exp.globalNextStateBatch.size() + " " + nextGlobalActions.size());


            agents[i].update(indivRewardBatchI, obsBatchI, exp.globalStateBatch,
                    exp.globalActionsBatch, exp.globalNextStateBatch, nextGlobalActions);

            agents[i].targetUpdate();

            System.exit(0);
        }
    }

    public void run() {
        final List<Double> episodeRewards = new ArrayList<>();

        for (int i = 0; i < maxEpisode; i++) {
            Boolean[][] states = robotController.reset();

            double epReward = 0;

            for (int j = 0; j < maxStep; j++) {
                final Action[] actions = getActions(states);

                // Simulate one step in the environment
                // Blocks until all hunters have moved in the environment
                final StepObs obs = robotController.step(actions);

                // System.out.println("one pass " + j);

                epReward +=
                        Arrays.stream(obs.rewards).mapToDouble(r -> r).average().orElse(Double.NaN);

                if (Arrays.stream(obs.dones).allMatch(d -> d) || j == maxStep - 1) {
                    replayBuffer.push(states, actions, obs.rewards, obs.nextStates, ones);
                    episodeRewards.add(epReward);
                    // System.out.println("episode: " + i + " reward: " + epReward);
                    break;
                } else {
                    replayBuffer.push(states, actions, obs.rewards, obs.nextStates, zeros);
                    states = obs.nextStates;

                    if (replayBuffer.getLength() > batchSize) {
                        update(batchSize);
                    }
                }

                // System.out.println("step done");

            }

            System.out.println("episode: " + i + " done");
        }
    }

}
