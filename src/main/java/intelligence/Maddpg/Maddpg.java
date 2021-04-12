package intelligence.Maddpg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import robots.Action;
import robots.Hunter;
import robots.RobotController;
import robots.StepObs;

public class Maddpg {
    private static final int NUM_AGENTS = 4;

    private static final ExecutorService executor = Executors.newFixedThreadPool(NUM_AGENTS);

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

    public Action[] getActions(final Float[][] states) {
        final Action[] actions = new Action[NUM_AGENTS];

        for (int i = 0; i < NUM_AGENTS; i++) {
            actions[i] = agents[i].getAction(states[i]);
        }

        return actions;
    }

    /**
     * Define a class to run the agent updates asynchronisally but block at the same time until
     * complete
     */
    class AgentUpdate implements Callable<Void> {
        final Hunter hunter;
        final List<Float> indivRewardBatchI;
        final List<Float[]> obsBatchI;
        final List<Float[]> globalStateBatch;
        final List<Action[]> globalActionsBatch;
        final List<Float[]> globalNextStateBatch;
        final INDArray tmp;

        public AgentUpdate(final Hunter hunter, final List<Float> indivRewardBatchI,
                final List<Float[]> obsBatchI, final List<Float[]> globalStateBatch,
                final List<Action[]> globalActionsBatch, final List<Float[]> globalNextStateBatch,
                final INDArray tmp) {
            this.hunter = hunter;
            this.indivRewardBatchI = indivRewardBatchI;
            this.obsBatchI = obsBatchI;
            this.globalStateBatch = globalStateBatch;
            this.globalActionsBatch = globalActionsBatch;
            this.globalNextStateBatch = globalNextStateBatch;
            this.tmp = tmp;
        }

        @Override
        public Void call() throws Exception {
            hunter.update(indivRewardBatchI, obsBatchI, globalStateBatch, globalActionsBatch,
                    globalNextStateBatch, tmp);

            hunter.targetUpdate();
            return null;
        }
    }

    public void update(final int batchSize) {
        final Sample exp = replayBuffer.sample(batchSize);

        final List<AgentUpdate> updaters = new ArrayList<>();
        for (int i = 0; i < NUM_AGENTS; i++) {
            final List<Float[]> obsBatchI = exp.obsBatch.get(i);
            final List<Action> indivActionBatchI = exp.indivActionBatch.get(i);
            final List<Float> indivRewardBatchI = exp.indivRewardBatch.get(i);
            final List<Float[]> nextObsBatchI = exp.nextObsBatch.get(i);

            final List<INDArray> nextGlobalActions = new ArrayList<>();
            for (int j = 0; j < agents.length; j++) {
                final Hunter hunter = agents[j];
                final INDArray arr = Nd4j.createFromArray(nextObsBatchI.toArray(new Float[][] {}));
                final float[][] nobi = hunter.actor.forward(arr).toFloatMatrix();
                INDArray n = Nd4j.createFromArray(Arrays.stream(nobi)
                        .map(x -> Float.valueOf(hunter.getMaxValueIndex(x))).toArray(Float[]::new));
                n = Nd4j.stack(0, n);

                nextGlobalActions.add(n);
            }
            final INDArray tmp =
                    Nd4j.concat(0, nextGlobalActions.stream().map(x -> x).toArray(INDArray[]::new))
                            .reshape(32, 4);

            // agents[i].update(indivRewardBatchI, obsBatchI, exp.globalStateBatch,
            // exp.globalActionsBatch, exp.globalNextStateBatch, tmp);
            // agents[i].targetUpdate();
            updaters.add(new AgentUpdate(agents[i], indivRewardBatchI, obsBatchI,
                    exp.globalStateBatch, exp.globalActionsBatch, exp.globalNextStateBatch, tmp));
        }


        try {
            executor.invokeAll(updaters);
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Runs the training for specified episodes x max steps
     */
    public void run() {
        final List<Double> episodeRewards = new ArrayList<>();

        final int captures = 0;
        for (int i = 1; i <= maxEpisode; i++) {
            Float[][] states = robotController.reset();

            double epReward = 0;

            int j = 0;
            for (; j < maxStep; j++) {
                final Action[] actions = getActions(states);

                // Simulate one step in the environment
                // Blocks until all hunters have moved in the environment
                final StepObs observation = robotController.step(actions);

                epReward += Arrays.stream(observation.rewards).mapToDouble(r -> r).average()
                        .orElse(Double.NaN);

                if (observation.done || j == maxStep - 1) {
                    replayBuffer.push(states, actions, observation.rewards, observation.nextStates,
                            ones);
                    episodeRewards.add(epReward);
                    // System.out.println("episode: " + i + " reward: " + epReward);
                    break;
                } else {
                    replayBuffer.push(states, actions, observation.rewards, observation.nextStates,
                            zeros);
                    states = observation.nextStates;

                    if (replayBuffer.getLength() > batchSize) {
                        update(batchSize);
                    }
                }

                // System.out.println("step done");
            }

            // robotController.env.updateTitle(String.valueOf(i));
            System.out.println("episode: " + i + " steps: " + j + " episode reward: " + epReward);
        }

        System.exit(0);
    }

}
