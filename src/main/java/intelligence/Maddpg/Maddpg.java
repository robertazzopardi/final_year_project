package intelligence.Maddpg;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.NDArrayIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import robots.Action;
import robots.Agent;
import robots.RobotController;
import robots.StepObs;
import simulation.CapturesChart;
import simulation.Mode;

public class Maddpg {
    private static final boolean STEP = false;

    private static final Logger LOG = LoggerFactory.getLogger(Maddpg.class.getSimpleName());

    private static final ExecutorService executor =
            Executors.newFixedThreadPool(RobotController.AGENT_COUNT);

    private final ReplayBuffer replayBuffer;

    private final RobotController robotController;

    private final int maxEpisode;
    private final int maxStep;
    private final int batchSize;

    public Maddpg(final int cap, final RobotController controller, final int maxEpisode,
            final int maxStep, final int batchSize) {
        this.replayBuffer = new ReplayBuffer(cap);
        this.robotController = controller;
        this.maxEpisode = maxEpisode;
        this.maxStep = maxStep;
        this.batchSize = batchSize;
    }

    public Action[] getActions(final INDArray[] states, final int episode) {
        final Action[] actions = new Action[RobotController.AGENT_COUNT - 1];

        for (int i = 0; i < RobotController.AGENT_COUNT - 1; i++) {
            final int[] arr = states[i].toIntVector();
            try (INDArray state = Nd4j.createFromArray(new int[][] {arr})) {
                actions[i] = robotController.getAgents().get(i).getAction(state, episode);
            }
        }

        return actions;
    }

    public void update(final int batchSize) {
        final Sample exp = replayBuffer.sample(batchSize);

        final List<AgentUpdate> updaters = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            final List<INDArray> obsBatchI = exp.obsBatch.get(i);
            final List<Action> indivActionBatchI = exp.indivActionBatch.get(i);
            final List<Float> indivRewardBatchI = exp.indivRewardBatch.get(i);
            final List<INDArray> nextObsBatchI = exp.nextObsBatch.get(i);

            final List<INDArray> nextGlobalActions = new ArrayList<>();

            for (int j = 0; j < 4; j++) {
                final Agent hunter = robotController.getAgents().get(j);
                final INDArray arr = hunter.getActorTarget()
                        .predict(Nd4j.vstack(nextObsBatchI.toArray(INDArray[]::new)));

                final Float[] indexes = new Float[RobotController.BATCH_SIZE];
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
                            .reshape(batchSize, 4);

            updaters.add(new AgentUpdate(robotController.getAgents().get(i),
                    new Data(indivRewardBatchI, obsBatchI, exp.globalStateBatch,
                            exp.globalActionsBatch, exp.globalNextStateBatch),
                    tmp, indivActionBatchI));
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
        final List<Integer> steps = new ArrayList<>();

        for (int episode = 1; episode < maxEpisode; episode++) {
            INDArray[] states = robotController.reset();

            double epReward = 0;

            int step = 0;
            for (; step < maxStep; step++) {
                final Action[] actions = getActions(states, episode);

                // Simulate one step in the environment
                // Blocks until all hunters have moved in the environment
                final StepObs observation = robotController.step(actions);

                epReward += Arrays.stream(observation.getRewards()).mapToDouble(r -> r).average()
                        .orElse(Double.NaN);

                if (observation.isDone() || step == maxStep - 1) {
                    break;
                }
                if (robotController.getEnv().getMode() == Mode.EVAL) {
                    states = observation.getNextStates();
                } else {
                    replayBuffer.push(states, actions, observation.getRewards(),
                            observation.getNextStates());

                    states = observation.getNextStates();

                    // if (replayBuffer.getLength() > batchSize && j % batchSize * 2 == 0) {
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

            steps.add(step);
            episodeRewards.add(epReward);

            logEpisodeInformation(episodeRewards, episode, epReward, step, steps);

            if (episode > 0 && episode % 100 == 0) {
                saveNetworks(episode);

                final int ep = episode;
                new Thread(() -> CapturesChart.makeChart(ep, episodeRewards, steps)).start();
            }

        }

        // saveNetworks(maxEpisode);

        System.exit(0);

    }

    private void saveNetworks(final int episode) {
        // Save the networks
        for (int i = 0; i < 4; i++) {
            robotController.getAgents().get(i).getActor()
                    .saveNetwork(RobotController.OUTPUT_FOLDER + episode + "_" + (i + 1) + ".zip");
        }
    }

    private void logEpisodeInformation(final List<Double> episodeRewards, final int i,
            final double epReward, final int j, final List<Integer> steps) {
        final String log = String.format(
                "Episode: %s | Step: %s | Average: %s | Reward: %s | Average: %s",
                fixedLengthString(String.valueOf(i), String.valueOf(maxEpisode).length()),
                fixedLengthString(String.valueOf(j), String.valueOf(maxStep).length()),
                fixedLengthString(
                        String.format("%.2f", steps.stream().mapToLong(r -> r).average().orElse(0)),
                        String.valueOf(maxStep).length() + 3),
                fixedLengthString(String.format("%.2f", epReward), 7),
                fixedLengthString(
                        String.format("%.2f",
                                episodeRewards.stream().mapToDouble(r -> r).average().orElse(0)),
                        7));
        LOG.info(log);
    }

    public static String fixedLengthString(final String string, final int length) {
        final String format = "%1$" + length + "s";
        return String.format(format, string);
    }

    class Data {
        final List<Float> indivRewardBatchI;
        final List<INDArray> obsBatchI;
        final List<INDArray[]> globalStateBatch;
        final List<Action[]> globalActionsBatch;
        final List<INDArray[]> globalNextStateBatch;

        public Data(final List<Float> indivRewardBatchI, final List<INDArray> obsBatchI,
                final List<INDArray[]> globalStateBatch, final List<Action[]> globalActionsBatch,
                final List<INDArray[]> globalNextStateBatch) {
            this.indivRewardBatchI = indivRewardBatchI;
            this.obsBatchI = obsBatchI;
            this.globalStateBatch = globalStateBatch;
            this.globalActionsBatch = globalActionsBatch;
            this.globalNextStateBatch = globalNextStateBatch;
        }
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
            agent.update(data.indivRewardBatchI, data.obsBatchI, data.globalStateBatch,
                    data.globalActionsBatch, data.globalNextStateBatch, tmp, indivAction);

            agent.updateTarget();
            return null;
        }
    }
}
