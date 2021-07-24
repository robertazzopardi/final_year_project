package intelligence.Maddpg;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.NDArrayIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import environment.Env;
import environment.util.CapturesChart;
import intelligence.Mode;
import robots.Action;
import robots.Agent;
import robots.StepObs;

public class Maddpg {
    private static final boolean STEP = false;

    private static final Logger LOG = LoggerFactory.getLogger(Maddpg.class.getSimpleName());

    private static final ExecutorService executor = Executors.newFixedThreadPool(Env.AGENT_COUNT);

    private final ReplayBuffer replayBuffer;

    private final Env env;

    private final int maxEpisode;
    private final int maxStep;
    private final int batchSize;

    public Maddpg(final Env env, final int maxEpisode, final int maxStep, final int batchSize) {
        this.replayBuffer = new ReplayBuffer();
        // replayBuffer = ReplayBuffer.deserialiseBuffer();
        this.env = env;
        this.maxEpisode = maxEpisode;
        this.maxStep = maxStep;
        this.batchSize = batchSize;
    }

    public Action[] getActions(final INDArray[] states, final int episode) {
        final Action[] actions = new Action[Env.HUNTER_COUNT];

        for (int i = 0; i < Env.HUNTER_COUNT; i++) {
            final int[] arr = states[i].toIntVector();
            try (INDArray state = Nd4j.createFromArray(new int[][] {arr})) {
                actions[i] = env.getAgents().get(i).getAction(state, episode);
            }
        }

        return actions;
    }

    public void update(final int batchSize) {
        final Sample exp = replayBuffer.sample(batchSize);

        final List<AgentUpdate> updaters = new ArrayList<>();
        for (int i = 0; i < Env.HUNTER_COUNT; i++) {
            final INDArray obsBatchI = exp.obsBatch[i];
            final List<Action> indivActionBatchI = exp.indivActionBatch.get(i);
            final List<Float> indivRewardBatchI = exp.indivRewardBatch.get(i);
            final INDArray nextObsBatchI = exp.nextObsBatch[i];

            final List<INDArray> nextGlobalActions = new ArrayList<>();

            for (int j = 0; j < Env.HUNTER_COUNT; j++) {
                final Agent hunter = env.getAgents().get(j);
                final INDArray arr = hunter.getActorTarget().predict(Nd4j.vstack(nextObsBatchI));

                final Float[] indexes = new Float[Env.BATCH_SIZE];
                for (int row = 0; row < arr.rows(); row++) {
                    final INDArray y = arr.get(NDArrayIndex.point(row), NDArrayIndex.all());
                    indexes[row] = Float.valueOf(hunter.getActor().nextAction(y, 0));
                }

                try (final INDArray n = Nd4j.createFromArray(indexes)) {
                    nextGlobalActions.add(Nd4j.stack(0, n));
                }
            }

            final INDArray tmp =
                    Nd4j.concat(0, nextGlobalActions.stream().map(x -> x).toArray(INDArray[]::new))
                            .reshape(batchSize, Env.HUNTER_COUNT);

            updaters.add(new AgentUpdate(env.getAgents().get(i),
                    new Data(indivRewardBatchI, obsBatchI, exp.globalStateBatch,
                            exp.globalActionsBatch, exp.globalNextStateBatch),
                    tmp, indivActionBatchI));
        }

        try {
            executor.invokeAll(updaters);
        } catch (final Exception e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Runs the training for specified episodes x max steps
     */
    public void run() {
        final List<Double> episodeRewards = new ArrayList<>();
        final List<Integer> steps = new ArrayList<>();

        for (int episode = 1; episode < maxEpisode; episode++) {
            INDArray[] states = env.reset();

            double epReward = 0;
            int step = 0;
            long startTime = System.nanoTime();
            for (; step < maxStep; step++) {
                final Action[] actions = getActions(states, episode);

                // Simulate one step in the environment
                // Blocks until all hunters have moved in the environment
                final StepObs observation = env.step(actions);

                epReward += Arrays.stream(observation.getRewards()).mapToDouble(r -> r).average()
                        .orElse(Double.NaN);

                if (observation.isDone() || step == maxStep - 1) {
                    break;
                }
                if (env.getMode() == Mode.EVAL) {
                    states = observation.getNextStates();
                    // slow down evaluation a bit
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        Thread.currentThread().interrupt();
                    }
                } else {
                    replayBuffer.push(states, actions, observation.getRewards(),
                            observation.getNextStates());

                    states = observation.getNextStates();

                    if (replayBuffer.getLength() > batchSize && step % batchSize == 0) {
                        update(batchSize);
                    }
                }

                if (STEP) {
                    try {
                        System.in.read();
                    } catch (final IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            final String episodeTime =
                    String.valueOf(TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - startTime));

            steps.add(step);
            episodeRewards.add(epReward);

            logEpisodeInformation(episodeRewards, episode, epReward, step, steps, episodeTime);

            if (episode > 0 && episode % 100 == 0) {
                // saveNetworks(episode);

                final int ep = episode;
                new Thread(() -> CapturesChart.makeChart(ep, episodeRewards, steps)).start();
            }

        }

        saveNetworks();
        // ReplayBuffer.serialiseBuffer(replayBuffer);

        try {
            System.in.read();
        } catch (final IOException e) {
            e.printStackTrace();
        }
        System.exit(0);
    }

    private void saveNetworks() {
        try {
            // Save the networks
            for (int i = 0; i < Env.HUNTER_COUNT; i++) {
                env.getAgents().get(i).getActor().saveNetwork(Env.OUTPUT_FOLDER + "actor_"
                        + env.trainedEpisodes + "_" + (i + 1) + ".zip");
                env.getAgents().get(i).getCritic().saveNetwork(Env.OUTPUT_FOLDER + "critic_"
                        + env.trainedEpisodes + "_" + (i + 1) + ".zip");
            }
            env.removeOldFilesFiles();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void logEpisodeInformation(final List<Double> episodeRewards, final int i,
            final double epReward, final int j, final List<Integer> steps, final String time) {
        final String log = String.format(
                "Episode: %s | Step: %s | Average: %s | Reward: %s | Average: %s | Time %s",
                fixedLengthString(String.valueOf(i), String.valueOf(maxEpisode).length()),
                fixedLengthString(String.valueOf(j), String.valueOf(maxStep).length()),
                fixedLengthString(
                        String.format("%.2f", steps.stream().mapToLong(r -> r).average().orElse(0)),
                        String.valueOf(maxStep).length() + 3),
                fixedLengthString(String.format("%.2f", epReward), 7),
                fixedLengthString(
                        String.format("%.2f",
                                episodeRewards.stream().mapToDouble(r -> r).average().orElse(0)),
                        7),
                time);
        LOG.info(log);
    }

    public static String fixedLengthString(final String string, final int length) {
        final String format = "%1$" + length + "s";
        return String.format(format, string);
    }

    /**
     * Define a class to run the agent updates asynchronisally but block at the same time until
     * complete
     */
    class AgentUpdate implements Callable<Void> {
        final Agent agent;
        final Data data;
        final INDArray tmp;
        final List<Action> indivAction;

        public AgentUpdate(final Agent agent, final Data data, final INDArray tmp,
                final List<Action> indivAction) {
            this.agent = agent;
            this.data = data;
            this.tmp = tmp;
            this.indivAction = indivAction;
        }

        @Override
        public Void call() throws Exception {
            agent.update(data, tmp, indivAction);

            agent.updateTarget();
            return null;
        }
    }
}
