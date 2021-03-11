package intelligence;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.deeplearning4j.core.storage.StatsStorage;
import org.deeplearning4j.datasets.iterator.FloatsDataSetIterator;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.BackpropType;
import org.deeplearning4j.nn.conf.GradientNormalization;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.WorkspaceMode;
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
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.learning.config.Nesterovs;
import org.nd4j.linalg.learning.config.RmsProp;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import robots.Action;
import robots.RobotController;
import simulation.SimulationEnv;

public class DeepQLearning {

	private static final Logger LOGGER = LoggerFactory.getLogger(DeepQLearning.class);
	private static final String FILE_NAME_PREFIX = SimulationEnv.OUTPUT_FOLDER + "network_";
	private static final int HIDDEN_NEURONS = RobotController.STATE_COUNT * 2;
	// private static final int HIDDEN_NEURONS = 150;

	private static final Random RANDOM = new Random();

	private final MultiLayerNetwork network;

	private double epsilon = 0.5;
	// private double epsilon = 0.9;

	private final Map<String, Double> qTable = new HashMap<>();

	// 0.001
	// private static final double LEARNING_RATE = 0.0006;
	private static final double LEARNING_RATE = 0.001;

	// Just make sure the number of inputs of the next layer equals to the number of
	// outputs in the previous layer.
	private final MultiLayerConfiguration configuration = new NeuralNetConfiguration.Builder().seed(12345)
			.trainingWorkspaceMode(WorkspaceMode.ENABLED)
			// Optimization Algorithm
			.optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
			// Weight Initialisation function
			.weightInit(WeightInit.RELU)
			// Gradient normalisation
			.gradientNormalization(GradientNormalization.ClipL2PerLayer)
			// Updater
			// .updater(new Adam(LEARNING_RATE))
			.updater(new AdaGrad(LEARNING_RATE))// optional epsilon
			// Activation Function
			.activation(Activation.RELU)
			// Regularization
			.l2(0.001)
			// Layers
			.list()
			// First hidden layer
			.layer(0,
					new DenseLayer.Builder().nIn(RobotController.STATE_COUNT).nOut(HIDDEN_NEURONS)
							.weightInit(WeightInit.RELU).activation(Activation.RELU).build())
			// Second hidden layer
			.layer(1,
					new DenseLayer.Builder().nIn(HIDDEN_NEURONS).nOut(HIDDEN_NEURONS).weightInit(WeightInit.RELU)
							.activation(Activation.RELU).build())
			// Third hidden layer
			// .layer(2,
			// new
			// DenseLayer.Builder().nIn(HIDDEN_NEURONS).nOut(HIDDEN_NEURONS).weightInit(WeightInit.RELU)
			// .activation(Activation.RELU).build())
			// Output layer
			.layer(2,
					new OutputLayer.Builder(LossFunctions.LossFunction.MSE).nIn(HIDDEN_NEURONS).nOut(Action.LENGTH)
							.weightInit(WeightInit.RELU).activation(Activation.IDENTITY)
							// .activation(Activation.SOFTMAX)
							.weightInit(WeightInit.RELU).build())
			.backpropType(BackpropType.Standard).build();

	public DeepQLearning() {
		network = new MultiLayerNetwork(configuration);
		network.init();

		// Initialize the user interface backend
		final UIServer uiServer = UIServer.getInstance();
		final StatsStorage statsStorage = new InMemoryStatsStorage();
		uiServer.attach(statsStorage);
		network.setListeners(new StatsListener(statsStorage));

		// network.setListeners(new ScoreIterationListener(1));

		// this will limit frequency of gc calls to 5000 milliseconds
		Nd4j.getMemoryManager().setAutoGcWindow(5000);
	}

	public DeepQLearning(final MultiLayerNetwork network) {
		this.network = network;
		network.init();

		// Initialize the user interface backend
		final UIServer uiServer = UIServer.getInstance();
		final StatsStorage statsStorage = new InMemoryStatsStorage();
		uiServer.attach(statsStorage);
		network.setListeners(new StatsListener(statsStorage));

		// network.setListeners(new ScoreIterationListener(1));

		// this will limit frequency of gc calls to 5000 milliseconds
		Nd4j.getMemoryManager().setAutoGcWindow(5000);
	}

	// Copy Constructor
	// public DeepQLearning(final DeepQLearning learning) {
	// LOGGER.info("CopyConstructor");
	// this.network = learning.network;
	// this.epsilon = learning.epsilon;
	// this.qTable = learning.qTable;
	// }

	public MultiLayerNetwork getNetwork() {
		return network;
	}

	public void updateEpsilon() {
		epsilon -= 0.001;
	}

	public Action getActionFromStates(final float[] states) {
		return epsilonGreedyAction(states);
	}

	private Action epsilonGreedyAction(final float[] states) {
		// https://www.geeksforgeeks.org/epsilon-greedy-algorithm-in-reinforcement-learning/
		final double random = RANDOM.nextDouble();

		if (random < epsilon) {

			return Action.getRandomAction();
		}

		return getActionFromTheNetwork(states);
	}

	private Action getActionFromTheNetwork(final float[] states) {

		final INDArray output = network.output(toINDArray(states), false);

		final float[] outputValues = output.data().asFloat();

		// Find index of the highest value
		final int maxValueIndex = getMaxValueIndex(outputValues);

		return Action.getActionByIndex(maxValueIndex);
	}

	private INDArray toINDArray(final float[] states) {
		return Nd4j.create(new float[][] { states });
	}

	private int getMaxValueIndex(final float[] values) {
		int maxAt = 0;

		for (int i = 0; i < values.length; i++) {
			maxAt = values[i] > values[maxAt] ? i : maxAt;
		}

		return maxAt;
	}

	public void update(final float[] states, final Action action, final double score, final float[] nextState) {
		// Get max q score for next state
		final double maxQScore = getMaxQScore(nextState);

		// Calculate target score
		final double targetScore = score + (0.9 * maxQScore);

		// Update the table with new score
		qTable.put(Arrays.toString(states) + '-' + action, targetScore);

		// Update network
		final INDArray stateObservation = toINDArray(states);
		final INDArray output = network.output(stateObservation);
		final INDArray updatedOutput = output.putScalar(action.getActionIndex(), targetScore);

		network.fit(stateObservation, updatedOutput);
	}

	private double getMaxQScore(final float[] states) {
		final String gameStateString = Arrays.toString(states);

		// System.out.println(gameStateString);
		final String stateWithActTRAVEL = gameStateString + '-' + Action.TRAVEL;
		final String stateWithActRIGHT_TURN = gameStateString + '-' + Action.RIGHT_TURN;
		final String stateWithActNOTHING = gameStateString + '-' + Action.NOTHING;
		final String stateWithActLEFT_TURN = gameStateString + '-' + Action.LEFT_TURN;

		qTable.putIfAbsent(stateWithActTRAVEL, 0.0);
		qTable.putIfAbsent(stateWithActRIGHT_TURN, 0.0);
		qTable.putIfAbsent(stateWithActNOTHING, 0.0);
		qTable.putIfAbsent(stateWithActLEFT_TURN, 0.0);

		double score = qTable.getOrDefault(stateWithActTRAVEL, 0.0);

		final Double scoreRight = qTable.getOrDefault(stateWithActRIGHT_TURN, 0.0);
		if (scoreRight > score) {
			score = scoreRight;
		}

		final Double scoreDown = qTable.getOrDefault(stateWithActNOTHING, 0.0);
		if (scoreDown > score) {
			score = scoreDown;
		}

		final Double scoreLeft = qTable.getOrDefault(stateWithActLEFT_TURN, 0.0);
		if (scoreLeft > score) {
			score = scoreLeft;
		}

		return score;
	}

	public static void saveNetwork(final MultiLayerNetwork network, final int number, final String episode) {
		LOGGER.debug("Saving trained network");
		try {
			network.save(new File(FILE_NAME_PREFIX + number + "_" + episode + ".zip"), true);
		} catch (final IOException e) {
			LOGGER.error("Failed to save network: '{}'", e.getMessage(), e);
		}
	}

	// public static DeepQLearning loadNetwork(final int number) {
	// try {
	// return new DeepQLearning(MultiLayerNetwork.load(new File(FILE_NAME_PREFIX +
	// number + ".zip"), true));
	// } catch (final IOException e) {
	// LOGGER.error("Failed to load network: '{}'", e.getMessage(), e);
	// }

	// return new DeepQLearning();
	// }

	public static DeepQLearning loadNetwork(final File file, final boolean needsTraining) {
		try {
			// return new DeepQLearning(MultiLayerNetwork.load(file, true));
			return new DeepQLearning(MultiLayerNetwork.load(file, needsTraining));
		} catch (final IOException e) {
			LOGGER.error("Failed to load network: '{}'", e.getMessage(), e);
		}

		LOGGER.info("Making new Network");
		return new DeepQLearning();
	}
}
