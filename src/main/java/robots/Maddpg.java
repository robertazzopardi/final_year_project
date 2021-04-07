package robots;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import intelligence.Maddpg.ReplayBuffer;
import intelligence.Maddpg.Sample;

public class Maddpg implements Runnable {

    private static final int NUM_AGENTS = 4;

    private final ReplayBuffer replayBuffer;

    private final Hunter[] agents;

    private final RobotController robotController;

    private final int maxEpisode;
    private final int maxStep;
    private final int batchSize;

    public Maddpg(final int cap, final Hunter[] agents, final RobotController controller,
            final int maxEpisode, final int maxStep, final int batchSize) {
        this.replayBuffer = new ReplayBuffer(cap);
        this.agents = agents;
        this.robotController = controller;
        this.maxEpisode = maxEpisode;
        this.maxStep = maxStep;
        this.batchSize = batchSize;
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
            final var obsBatcI = exp.obsBatch.get(i);
            final var indivActionBatchI = exp.indivActionBatch.get(i);
            final var indivRewardBatchI = exp.indivRewardBatch.get(i);
            final var nextObsBatchI = exp.nextObsBatch.get(i).toArray(Boolean[]::new);

            final var nextGlobalActions = new ArrayList<>();

            for (final Hunter hunter : agents) {
                final Action indivNextAction = hunter.getAction(nextObsBatchI);

            }
        }
    }

    @Override
    public void run() {
        final List<Double> episodeRewards = new ArrayList<>();

        for (int i = 0; i < maxEpisode; i++) {
            Boolean[][] states = Arrays.stream(agents).map(h -> h.createGameObservation())
                    .toArray(Boolean[][]::new);

            double epReward = 0;

            for (int j = 0; j < maxStep; j++) {
                // while (running) {

                final Action[] actions = getActions(states);

                final StepObs obs = robotController.step(actions);


                // getNextObs

                System.out.println("one pass " + j);

                epReward +=
                        Arrays.stream(obs.rewards).mapToDouble(r -> r).average().orElse(Double.NaN);

                if (j == maxStep - 1) {
                    // dones = true
                    replayBuffer.push(states, actions, obs.rewards, obs.nextStates);
                    episodeRewards.add(epReward);
                    System.out.println("episode: " + i + " reward: " + epReward);
                } else {
                    // dones = 0
                    replayBuffer.push(states, actions, obs.rewards, obs.nextStates);
                    states = obs.nextStates;

                    if (replayBuffer.getLength() > batchSize) {
                        update(batchSize);
                    }
                }

            }

            System.out.println("episode done");
        }
    }



}
