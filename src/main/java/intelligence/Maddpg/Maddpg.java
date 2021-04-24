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
import robots.Hunter;
import robots.RobotController;
import robots.StepObs;
import simulation.Mode;

public class Maddpg {
    private static final boolean STEP = false;

    private static final Logger LOG = LoggerFactory.getLogger(Maddpg.class.getSimpleName());

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

    public Action[] getActions(final Boolean[][] states, final int episode) {
        final Action[] actions = new Action[NUM_AGENTS];

        for (int i = 0; i < NUM_AGENTS; i++) {
            actions[i] = agents[i].getAction(states[i], episode);
        }

        return actions;
    }

    /**
     * Define a class to run the agent updates asynchronisally but block at the same time until
     * complete
     */
    class AgentUpdate implements Callable<Void> {
        final Hunter hunter;
        final Data data;
        final INDArray tmp;
        final List<Action> indivAction;
        final int num;

        public AgentUpdate(final Hunter hunter, final Data data, final INDArray tmp,
                final List<Action> indivAction, final int num) {
            this.hunter = hunter;
            this.data = data;
            this.tmp = tmp;
            this.indivAction = indivAction;
            this.num = num;
        }

        @Override
        public Void call() throws Exception {
            hunter.update(data.indivRewardBatchI, data.obsBatchI, data.globalStateBatch,
                    data.globalActionsBatch, data.globalNextStateBatch, tmp, indivAction, num);

            hunter.updateTarget();
            return null;
        }
    }

    public void update(final int batchSize) {
        final Sample exp = replayBuffer.sample(batchSize);

        final List<AgentUpdate> updaters = new ArrayList<>();
        for (int i = 0; i < NUM_AGENTS; i++) {
            final List<Boolean[]> obsBatchI = exp.obsBatch.get(i);
            final List<Action> indivActionBatchI = exp.indivActionBatch.get(i);
            final List<Float> indivRewardBatchI = exp.indivRewardBatch.get(i);
            final List<Boolean[]> nextObsBatchI = exp.nextObsBatch.get(i);

            final List<INDArray> nextGlobalActions = new ArrayList<>();
            for (int j = 0; j < agents.length; j++) {
                final Hunter hunter = agents[j];
                final INDArray arr = hunter.getActorTarget()
                        .predict(Nd4j.createFromArray(nextObsBatchI.toArray(new Boolean[][] {})));
                // final double[][] nobi = hunter.getActorTarget().predict(arr).toDoubleMatrix();

                // INDArray n = Nd4j.createFromArray(
                // Arrays.stream(nobi).map(x -> Float.valueOf(hunter.boltzmanDistribution(x)))
                // .toArray(Float[]::new));

                // n = Nd4j.stack(0, n);

                // nextGlobalActions.add(n);

                Float[] indexes = new Float[RobotController.BATCH_SIZE];
                for (int row = 0; row < arr.rows(); row++) {
                    INDArray y = arr.get(NDArrayIndex.point(row), NDArrayIndex.all());
                    // System.out.println();
                    indexes[row] = Float.valueOf(hunter.boltzmanNextAction(y, 0));
                }
                INDArray n = Nd4j.createFromArray(indexes);
                n = Nd4j.stack(0, n);
                nextGlobalActions.add(n);

            }
            final INDArray tmp =
                    Nd4j.concat(0, nextGlobalActions.stream().map(x -> x).toArray(INDArray[]::new))
                            .reshape(batchSize, 4);

            updaters.add(new AgentUpdate(agents[i],
                    new Data(indivRewardBatchI, obsBatchI, exp.globalStateBatch,
                            exp.globalActionsBatch, exp.globalNextStateBatch),
                    tmp, indivActionBatchI, i));
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

        for (int i = 0; i < maxEpisode; i++) {
            Boolean[][] states = robotController.reset();

            double epReward = 0;

            int j = 0;
            for (; j < maxStep; j++) {
                final Action[] actions = getActions(states, i);

                // Simulate one step in the environment
                // Blocks until all hunters have moved in the environment
                final StepObs observation = robotController.step(actions, j);

                epReward += Arrays.stream(observation.getRewards()).mapToDouble(r -> r).average()
                        .orElse(Double.NaN);

                if (robotController.getEnv().getMode() == Mode.EVAL) {
                    states = observation.getNextStates();
                } else {
                    if (observation.isDone() || j == maxStep - 1) {
                        replayBuffer.push(states, actions, observation.getRewards(),
                                observation.getNextStates(), ones);
                        break;
                    } else {
                        replayBuffer.push(states, actions, observation.getRewards(),
                                observation.getNextStates(), zeros);
                        states = observation.getNextStates();

                        if (replayBuffer.getLength() > batchSize && j % batchSize * 2 == 0) {
                            update(batchSize);
                        }
                    }
                }

                if (STEP) {
                    try {
                        System.in.read();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }

            steps.add(j);
            episodeRewards.add(epReward);

            // System.out.println(String.valueOf(epReward));
            logEpisodeInformation(episodeRewards, i, epReward, j, steps);
        }

        cleanUp();
    }

    private void cleanUp() {
        robotController.getEnv().stopRunning();

        // Save the networks
        for (int i = 0; i < 4; i++) {
            agents[i].getActor().saveNetwork(
                    RobotController.OUTPUT_FOLDER + maxEpisode + "_" + (i + 1) + ".zip");
        }

        System.exit(0);
    }

    private void logEpisodeInformation(final List<Double> episodeRewards, int i, double epReward,
            int j, final List<Integer> steps) {
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
        final List<Boolean[]> obsBatchI;
        final List<Boolean[]> globalStateBatch;
        final List<Action[]> globalActionsBatch;
        final List<Boolean[]> globalNextStateBatch;

        public Data(final List<Float> indivRewardBatchI, final List<Boolean[]> obsBatchI,
                final List<Boolean[]> globalStateBatch, final List<Action[]> globalActionsBatch,
                final List<Boolean[]> globalNextStateBatch) {
            this.indivRewardBatchI = indivRewardBatchI;
            this.obsBatchI = obsBatchI;
            this.globalStateBatch = globalStateBatch;
            this.globalActionsBatch = globalActionsBatch;
            this.globalNextStateBatch = globalNextStateBatch;
        }
    }

    // class OUNoise {
    // double mu = 0.0;
    // double theta = 0.15;
    // double sigma = 0.3;
    // double max_sigma = 0.3;
    // double min_sigma = 0.3;
    // int decay_period = 100000;
    // int action_dim = action_space.shape[0];
    // int low = action_space.low;
    // int high = action_space.high;
    // double[] state;

    // public OUNoise() {
    // state = new double[Action.LENGTH];
    // reset();
    // }

    // public void reset() {
    // // self.state = np.ones(self.action_dim) * mu
    // Arrays.fill(state, mu);
    // }

    // private void increment() {
    // // x = self.state
    // // dx = self.theta * (self.mu - x) + self.sigma * np.random.randn(self.action_dim)
    // state = Arrays.stream(state).map(i -> i + process(i)).toArray();
    // // self.state = x + dx
    // // return self.state
    // }

    // private double process(double val) {
    // return theta * (mu - val) + sigma * Math.random();
    // }

    // public Action get_action(float action, int step) {
    // ou_state = increment();
    // sigma = max_sigma - (max_sigma - min_sigma) * Math.min(1.0, step / decay_period);
    // return np.clip(action + ou_state, self.low, self.high);
    // if (result < low)
    // result = low;
    // if (result > high)
    // result = high;
    // }
    // }



}
