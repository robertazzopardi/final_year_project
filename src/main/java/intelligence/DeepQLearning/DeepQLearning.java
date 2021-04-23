package intelligence.DeepQLearning;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import org.deeplearning4j.core.storage.StatsStorage;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.BackpropType;
import org.deeplearning4j.nn.conf.GradientNormalization;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.gradient.Gradient;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.ui.api.UIServer;
import org.deeplearning4j.ui.model.stats.StatsListener;
import org.deeplearning4j.ui.model.storage.InMemoryStatsStorage;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import intelligence.Network;
import robots.Action;
import robots.Hunter;
import robots.RobotController;

public class DeepQLearning implements Network {

	private static final Logger LOGGER = LoggerFactory.getLogger(DeepQLearning.class);
	private static final String FILE_NAME_PREFIX = RobotController.OUTPUT_FOLDER + "network_";

	private static final int HIDDEN_NEURONS = 150;

	private static final Random RANDOM = new Random();

	private final MultiLayerNetwork network;

	// private double epsilon = 0.5;
	private double epsilon = 1;
	private static final double EPSILON_DECAY = 0.99975;
	private static final double MIN_EPSILON = 0.001;
	private static final double GAMMA = 0.99;

	private static final Map<String, Double> qTable = new HashMap<>();

	// 0.001
	// private static final double LEARNING_RATE = 0.0006;
	// private static final double LEARNING_RATE = 0.006;

	// Just make sure the number of inputs of the next layer equals to the number of
	// outputs in the previous layer.
	private final MultiLayerConfiguration configuration = new NeuralNetConfiguration.Builder()
			.seed(12345).optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
			.weightInit(WeightInit.RELU)
			//
			// .updater(new Adam(LEARNING_RATE))
			// .updater(new Adam(0.0005, 0.9, 0.999, 1e-08))
			.updater(new Adam(0.001, 0.9, 0.999, 0.1))
			// .updater(new Adam(0.0005, 0.9, 0.999, 0.1))
			// .updater(new Sgd(LEARNING_RATE))
			//
			.gradientNormalization(GradientNormalization.RenormalizeL2PerLayer)
			//
			.miniBatch(true)
			//
			.dropOut(0.8)
			//
			.l2(0.000001)
			//
			.list()
			//
			.layer(0, new DenseLayer.Builder().nIn(Hunter.OBSERVATION_COUNT).nOut(HIDDEN_NEURONS)
					.dropOut(0.5).weightInit(WeightInit.RELU).activation(Activation.RELU).build())
			.layer(1,
					new DenseLayer.Builder().nIn(HIDDEN_NEURONS).nOut(HIDDEN_NEURONS).dropOut(0.5)
							.weightInit(WeightInit.RELU).activation(Activation.RELU).build())
			.layer(2,
					new DenseLayer.Builder().nIn(HIDDEN_NEURONS).nOut(HIDDEN_NEURONS).dropOut(0.5)
							.weightInit(WeightInit.RELU).activation(Activation.RELU).build())
			.layer(3,
					new OutputLayer.Builder(LossFunctions.LossFunction.MSE).nIn(HIDDEN_NEURONS)
							.nOut(Action.LENGTH).weightInit(WeightInit.RELU)
							.activation(Activation.IDENTITY).weightInit(WeightInit.RELU).build())
			.backpropType(BackpropType.Standard).build();

	public DeepQLearning(final boolean eval) {
		network = new MultiLayerNetwork(configuration);
		network.init();

		if (!eval) {
			enableUIServer();
		}
	}

	public DeepQLearning(final MultiLayerNetwork network, final boolean eval) {
		this.network = network;
		network.init();

		epsilon = MIN_EPSILON;

		if (!eval) {
			enableUIServer();
		}
	}

	private void enableUIServer() {
		// Initialize the user interface backend
		final UIServer uiServer = UIServer.getInstance();
		final StatsStorage statsStorage = new InMemoryStatsStorage();
		uiServer.attach(statsStorage);
		network.setListeners(new StatsListener(statsStorage));

		// this will limit frequency of gc calls to 5000 milliseconds
		// Nd4j.getMemoryManager().setAutoGcWindow(5000);
		Nd4j.getMemoryManager().togglePeriodicGc(false);
	}

	public MultiLayerNetwork getNetwork() {
		return network;
	}

	/**
	 * epsilon reduction strategy from sendtex
	 * https://pythonprogramming.net/training-deep-q-learning-dqn-reinforcement-learning-python-tutorial/?completed=/deep-q-learning-dqn-reinforcement-learning-python-tutorial/
	 */
	public void updateEpsilon() {
		if (epsilon > MIN_EPSILON) {
			epsilon *= EPSILON_DECAY;
			epsilon = Math.max(MIN_EPSILON, epsilon);
		}
	}

	public Action getAction(final Boolean[] states) {
		// epsilon greedy action
		if (RANDOM.nextDouble() < epsilon) {
			return Action.getRandomAction();
		}

		final Action actionFromTheNetwork = getActionFromTheNetwork(states);
		updateEpsilon();
		return actionFromTheNetwork;
	}

	// private INDArray toINDArray(final Boolean[] states) {
	// return Nd4j.create(new Boolean[][] {states});
	// }
	private static INDArray toINDArray(final Boolean[] states) {
		return Nd4j.create(
				new double[][] {Arrays.stream(states).mapToDouble(i -> i ? 1 : 0).toArray()});
	}

	private int getMaxValueIndex(final double[] values) {
		int maxAt = 0;

		for (int i = 0; i < values.length; i++) {
			maxAt = values[i] > values[maxAt] ? i : maxAt;
		}

		return maxAt;
	}

	private Action getActionFromTheNetwork(final Boolean[] states) {
		final INDArray output = network.output(toINDArray(states), false);

		final double[] outputValues = output.toDoubleVector();

		// Find index of the highest value
		final int maxValueIndex = getMaxValueIndex(outputValues);

		return Action.getActionByIndex(maxValueIndex);
	}

	public void update(final Boolean[] states, final Action action, final double score,
			final Boolean[] newObservations) {

		// Get max q score for next state
		final double maxQScore = getMaxQScore(newObservations);

		// Calculate target score
		final double targetScore = score + (GAMMA * maxQScore);

		// Update the table with new score
		qTable.put(makeKey(Arrays.toString(states), action), targetScore);

		// Update network
		final INDArray stateObservation = toINDArray(states);
		final INDArray output = network.output(stateObservation);
		final INDArray updatedOutput = output.putScalar(action.getActionIndex(), targetScore);

		network.fit(stateObservation, updatedOutput);
	}

	private double getMaxQScore(final Boolean[] states) {
		final String gameObservationString = Arrays.toString(states);

		// final String FORWARD = makeKey(gameObservationString, Action.FORWARD);
		// final String LEFT = makeKey(gameObservationString, Action.LEFT);
		// final String RIGHT = makeKey(gameObservationString, Action.RIGHT);
		// final String NOTHING = makeKey(gameObservationString, Action.NOTHING);

		// qTable.putIfAbsent(FORWARD, 0.0);
		// qTable.putIfAbsent(LEFT, 0.0);
		// qTable.putIfAbsent(RIGHT, 0.0);
		// qTable.putIfAbsent(NOTHING, 0.0);

		// double score = qTable.getOrDefault(NOTHING, 0.0);
		double score = 0;
		// final Double scoreRight = qTable.getOrDefault(RIGHT, 0.0);
		// if (scoreRight > score) {
		// score = scoreRight;
		// }

		// final Double scoreLeft = qTable.getOrDefault(LEFT, 0.0);
		// if (scoreLeft > score) {
		// score = scoreLeft;
		// }

		// final Double scoreFORWARD = qTable.getOrDefault(FORWARD, 0.0);
		// if (scoreFORWARD > score) {
		// score = scoreFORWARD;
		// }

		return score;
	}

	private static String makeKey(final String state, final Action action) {
		return state + "-" + action;
	}

	public static void saveNetwork(final MultiLayerNetwork network, final int number,
			final String episode) {
		LOGGER.debug("Saving trained network");
		try {
			network.save(new File(FILE_NAME_PREFIX + number + "_" + episode + ".zip"), true);
		} catch (final IOException e) {
			LOGGER.error("Failed to save network: '{}'", e.getMessage(), e);
		}
	}

	public static DeepQLearning loadNetwork(final File file, final boolean needsTraining,
			final boolean eval) {
		try {
			return new DeepQLearning(MultiLayerNetwork.load(file, needsTraining), eval);
		} catch (final IOException e) {
			LOGGER.error("Failed to load network: '{}'", e.getMessage(), e);
		}

		LOGGER.info("Making new Network");
		return new DeepQLearning(eval);
	}

	@Override
	public INDArray predict(final INDArray inputs) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void update(final INDArray inputs, final INDArray outputs) {
		// TODO Auto-generated method stub

	}

	@Override
	public Gradient getGradient(INDArray inputs, INDArray labels) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void saveNetwork(String fileName) {
		// TODO Auto-generated method stub

	}

	@Override
	public MultiLayerNetwork loadNetwork(final File file, boolean moreTraining) {
		// TODO Auto-generated method stub
		return null;
	}
}
