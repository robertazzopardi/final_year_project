package intelligence;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.deeplearning4j.core.storage.StatsStorage;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.BackpropType;
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
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import robots.Action;
import robots.RobotController;
import simulation.SimulationEnv;

public class DeepQLearning {

	private static final Logger LOGGER = LoggerFactory.getLogger(DeepQLearning.class);
	private static final String FILE_NAME_PREFIX = SimulationEnv.OUTPUT_FOLDER + "network_";
	// private static final int HIDDEN_NEURONS = RobotController.STATE_COUNT + 1;
	private static final int HIDDEN_NEURONS = 150;

	private final MultiLayerNetwork network;
	private double epsilon = 0.9;
	private final Map<String, Double> qTable;

	// 0.001
	private static final double LEARNING_RATE = 0.001;

	// Just make sure the number of inputs of the next layer equals to the number of
	// outputs in the previous layer.
	private static final MultiLayerConfiguration configuration = new NeuralNetConfiguration.Builder().seed(12345)
			.trainingWorkspaceMode(WorkspaceMode.ENABLED).weightInit(WeightInit.RELU).updater(new Adam(LEARNING_RATE))
			.activation(Activation.RELU).optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT).l2(0.0001)
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
			.layer(2,
					new DenseLayer.Builder().nIn(HIDDEN_NEURONS).nOut(HIDDEN_NEURONS).weightInit(WeightInit.RELU)
							.activation(Activation.RELU).build())
			// Output layer
			.layer(3,
					new OutputLayer.Builder().nIn(HIDDEN_NEURONS).nOut(Action.LENGTH).weightInit(WeightInit.RELU)
							.activation(Activation.SOFTMAX).lossFunction(LossFunctions.LossFunction.MSE).build())
			.backpropType(BackpropType.Standard).build();

	public DeepQLearning() {
		qTable = new HashMap<>();
		network = new MultiLayerNetwork(configuration);
		network.init();

		// Initialize the user interface backend
		UIServer uiServer = UIServer.getInstance();
		StatsStorage statsStorage = new InMemoryStatsStorage();
		uiServer.attach(statsStorage);
		network.setListeners(new StatsListener(statsStorage));
	}

	public DeepQLearning(final MultiLayerNetwork network) {
		qTable = new HashMap<>();
		this.network = network;
		network.init();
		// network.setListeners(new ScoreIterationListener(1));

		// Initialize the user interface backend
		UIServer uiServer = UIServer.getInstance();
		StatsStorage statsStorage = new InMemoryStatsStorage();
		uiServer.attach(statsStorage);
		network.setListeners(new StatsListener(statsStorage));
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

	public Action epsilonGreedyAction(final float[] states) {
		// https://www.geeksforgeeks.org/epsilon-greedy-algorithm-in-reinforcement-learning/
		final double random = getRandomDouble();
		if (random < epsilon) {
			return Action.getRandomAction();
		}

		return getActionFromTheNetwork(states);
	}

	public Action getActionFromTheNetwork(final float[] states) {
		final INDArray output = network.output(toINDArray(states), false);

		// Values provided by the network. Based on them we chose the current best
		// action.

		final float[] outputValues = output.data().asFloat();

		// Find index of the highest value
		final int maxValueIndex = getMaxValueIndex(outputValues);

		return Action.getActionByIndex(maxValueIndex);
	}

	private INDArray toINDArray(final float[] states) {
		return Nd4j.create(new float[][] { states });
	}

	private double getRandomDouble() {
		return (Math.random() * ((double) 1 + 1 - (double) 0)) + (double) 0;
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

		final String stateWithActTRAVEL = gameStateString + '-' + Action.TRAVEL;
		final String stateWithActRIGHT_TURN = gameStateString + '-' + Action.RIGHT_TURN;
		final String stateWithActNOTHING = gameStateString + '-' + Action.NOTHING;
		final String stateWithActLEFT_TURN = gameStateString + '-' + Action.LEFT_TURN;

		qTable.putIfAbsent(stateWithActTRAVEL, 0.0);
		qTable.putIfAbsent(stateWithActRIGHT_TURN, 0.0);
		qTable.putIfAbsent(stateWithActNOTHING, 0.0);
		qTable.putIfAbsent(stateWithActLEFT_TURN, 0.0);

		double score = qTable.get(stateWithActTRAVEL);

		final Double scoreRight = qTable.get(stateWithActRIGHT_TURN);
		if (scoreRight > score) {
			score = scoreRight;
		}

		final Double scoreDown = qTable.get(stateWithActNOTHING);
		if (scoreDown > score) {
			score = scoreDown;
		}

		final Double scoreLeft = qTable.get(stateWithActLEFT_TURN);
		if (scoreLeft > score) {
			score = scoreLeft;
		}

		// LOGGER.info(Integer.toString(qTable.size()));

		return score;
	}

	public static void saveNetwork(final MultiLayerNetwork network, final int number, final String episode) {
		LOGGER.debug("Saving trained network");
		try {
			network.save(new File(FILE_NAME_PREFIX + number + "_" + episode + ".zip"));
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

	public static DeepQLearning loadNetwork(final File file) {
		try {
			return new DeepQLearning(MultiLayerNetwork.load(file, true));
		} catch (final IOException e) {
			LOGGER.error("Failed to load network: '{}'", e.getMessage(), e);
		}

		LOGGER.info("Making new Network");
		return new DeepQLearning();
	}
}
