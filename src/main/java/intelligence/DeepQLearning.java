package intelligence;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Stream;

import org.deeplearning4j.core.storage.StatsStorage;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.BackpropType;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.ui.api.UIServer;
import org.deeplearning4j.ui.model.stats.StatsListener;
import org.deeplearning4j.ui.model.storage.InMemoryStatsStorage;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.AdaGrad;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import robots.Action;
import robots.RobotController;
import simulation.SimulationEnv;

public class DeepQLearning {

	private static final Logger LOGGER = LoggerFactory.getLogger(DeepQLearning.class);
	private static final String FILE_NAME_PREFIX = SimulationEnv.OUTPUT_FOLDER + "network_";
	// private static final int HIDDEN_NEURONS = RobotController.STATE_COUNT * 2;
	private static final int HIDDEN_NEURONS = 150;

	private static final Random RANDOM = new Random();

	private final MultiLayerNetwork network;

	// private double epsilon = 0.5;
	private double epsilon = 0.9;

	// private static final Map<String, Double> qTable = new HashMap<>();
	private static final Map<String, Double> qTable = initQTable();

	// 0.001
	// private static final double LEARNING_RATE = 0.0006;
	private static final double LEARNING_RATE = 0.001;

	// public static final int NUMBER_OF_STATES = (3 * 4) + 4;

	// Just make sure the number of inputs of the next layer equals to the number of
	// outputs in the previous layer.
	private final MultiLayerConfiguration configuration = new NeuralNetConfiguration.Builder()
			//
			.seed(12345)
			//
			.optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
			//
			.weightInit(WeightInit.XAVIER)
			//
			// .updater(new Adam(.0001))
			// .updater(new Adam(new StepSchedule(ScheduleType.EPOCH, 0.0001, 0.1, 1)))
			.updater(new AdaGrad(.001))
			// .updater(new Sgd(0.001))
			//
			.activation(Activation.LEAKYRELU)
			//
			.l2(0.001)
			//
			.list()
			//
			.layer(0, new DenseLayer.Builder()
					//
					.nIn(RobotController.STATE_COUNT).nOut(HIDDEN_NEURONS)
					//
					.weightInit(WeightInit.XAVIER)
					//
					.activation(Activation.LEAKYRELU)
					//
					.build())
			//
			.layer(1, new DenseLayer.Builder()
					//
					.nIn(HIDDEN_NEURONS).nOut(HIDDEN_NEURONS)
					//
					.weightInit(WeightInit.XAVIER)
					//
					.activation(Activation.LEAKYRELU)
					//
					.build())
			//
			.layer(2, new OutputLayer.Builder()
					//
					.nIn(HIDDEN_NEURONS).nOut(Action.LENGTH)
					//
					.lossFunction(LossFunctions.LossFunction.MSE)
					// .lossFunction(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD)
					//
					.weightInit(WeightInit.XAVIER)
					//
					// .activation(Activation.IDENTITY)
					// .activation(Activation.LEAKYRELU)
					.activation(Activation.SOFTMAX)
					//
					.weightInit(WeightInit.XAVIER)
					//
					.build())
			//
			.backpropType(BackpropType.Standard).build();

	public DeepQLearning(boolean eval) {
		network = new MultiLayerNetwork(configuration);
		network.init();

		initQTable();

		if (!eval) {
			// Initialize the user interface backend
			final UIServer uiServer = UIServer.getInstance();
			final StatsStorage statsStorage = new InMemoryStatsStorage();
			uiServer.attach(statsStorage);
			network.setListeners(new StatsListener(statsStorage));

			// network.setListeners(new ScoreIterationListener(1));

			// this will limit frequency of gc calls to 5000 milliseconds
			Nd4j.getMemoryManager().setAutoGcWindow(5000);
		}

	}

	public DeepQLearning(final MultiLayerNetwork network, boolean eval) {
		this.network = network;
		network.init();

		if (!eval) {
			// Initialize the user interface backend
			final UIServer uiServer = UIServer.getInstance();
			final StatsStorage statsStorage = new InMemoryStatsStorage();
			uiServer.attach(statsStorage);
			network.setListeners(new StatsListener(statsStorage));

			// network.setListeners(new ScoreIterationListener(1));

			// this will limit frequency of gc calls to 5000 milliseconds
			Nd4j.getMemoryManager().setAutoGcWindow(5000);
		}
	}

	public MultiLayerNetwork getNetwork() {
		return network;
	}

	public void updateEpsilon() {
		epsilon -= 0.001;
	}

	public Action getActionFromStates(final int[] states) {
		// epsilon greedy action
		if (RANDOM.nextDouble() < epsilon) {
			return Action.getRandomAction();
		}

		return getActionFromTheNetwork(states);
	}

	private Action getActionFromTheNetwork(final int[] states) {

		final INDArray output = network.output(toINDArray(states), false);

		final float[] outputValues = output.data().asFloat();

		// Find index of the highest value
		final int maxValueIndex = getMaxValueIndex(outputValues);

		return Action.getActionByIndex(maxValueIndex);
	}

	private INDArray toINDArray(final int[] states) {
		return Nd4j.create(new int[][] { states });
	}

	private int getMaxValueIndex(final float[] values) {
		int maxAt = 0;

		for (int i = 0; i < values.length; i++) {
			maxAt = values[i] > values[maxAt] ? i : maxAt;
		}

		return maxAt;
	}

	public void update(final int[] states, final Action action, final double score, final int[] nextState) {
		// Get max q score for next state
		final double maxQScore = getMaxQScore(nextState);

		// Calculate target score
		final double targetScore = score + (0.9 * maxQScore);

		// Update the table with new score
		qTable.put(makeKey(Arrays.toString(states), action), targetScore);

		// Update network
		final INDArray stateObservation = toINDArray(states);
		final INDArray output = network.output(stateObservation);
		final INDArray updatedOutput = output.putScalar(action.getActionIndex(), targetScore);

		network.fit(stateObservation, updatedOutput);
	}

	private double getMaxQScore(final int[] states) {
		final String gameStateString = Arrays.toString(states);

		final String stateWithActFORWARD = makeKey(gameStateString, Action.FORWARD);
		final String stateWithActRIGHT = makeKey(gameStateString, Action.RIGHT);
		final String stateWithActNOTHING = makeKey(gameStateString, Action.NOTHING);
		final String stateWithActLEFT = makeKey(gameStateString, Action.LEFT);

		double score = qTable.get(stateWithActFORWARD);

		final Double scoreRight = qTable.get(stateWithActRIGHT);
		if (scoreRight > score) {
			score = scoreRight;
		}

		final Double scoreDown = qTable.get(stateWithActNOTHING);
		if (scoreDown > score) {
			score = scoreDown;
		}

		final Double scoreLeft = qTable.get(stateWithActLEFT);
		if (scoreLeft > score) {
			score = scoreLeft;
		}

		return score;
	}

	private static String makeKey(String state, Action action) {
		return state + "-" + action;
	}

	private static Map<String, Double> initQTable() {
		final HashMap<String, Double> qTable = new HashMap<>();
		final List<String> inputs = getInputs(RobotController.STATE_COUNT);

		for (final String state : inputs) {
			for (Action action : Action.values()) {
				qTable.put(makeKey(state, action), 0.0);
			}
		}

		return qTable;
	}

	private static List<String> getInputs(final int inputCount) {
		final List<String> inputs = new ArrayList<>();

		for (int i = 0; i < Math.pow(2, inputCount); i++) {
			StringBuilder bin = new StringBuilder(Integer.toBinaryString(i));
			while (bin.length() < inputCount) {
				bin.insert(0, "0");
			}

			inputs.add(Arrays.toString(toBinaryArray(bin.toString())));
		}

		return inputs;
	}

	private static Integer[] toBinaryArray(final String binary) {
		return Stream.of(binary.split("")).map(Integer::parseInt).toArray(Integer[]::new);
	}

	public static void saveNetwork(final MultiLayerNetwork network, final int number, final String episode) {
		LOGGER.debug("Saving trained network");
		try {
			network.save(new File(FILE_NAME_PREFIX + number + "_" + episode + ".zip"), true);
		} catch (final IOException e) {
			LOGGER.error("Failed to save network: '{}'", e.getMessage(), e);
		}
	}

	public static DeepQLearning loadNetwork(final File file, final boolean needsTraining, boolean eval) {
		try {
			// return new DeepQLearning(MultiLayerNetwork.load(file, true));
			return new DeepQLearning(MultiLayerNetwork.load(file, needsTraining), eval);
		} catch (final IOException e) {
			LOGGER.error("Failed to load network: '{}'", e.getMessage(), e);
		}

		LOGGER.info("Making new Network");
		return new DeepQLearning(eval);
	}
}
