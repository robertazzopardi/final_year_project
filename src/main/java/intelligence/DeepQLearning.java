package intelligence;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

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
import org.nd4j.linalg.learning.config.AdaGrad;
import org.nd4j.linalg.lossfunctions.LossFunctions;

import robots.Action;

public class DeepQLearning {

	private static final Logger LOGGER = Logger.getLogger(DeepQLearning.class.getSimpleName());

	private final MultiLayerNetwork network;

	private double epsilon = 0.9;

	public DeepQLearning(final int numberOfInputs, final int numberOfOutputs) {
		final int neurons = numberOfInputs + 1;

		// Just make sure the number of inputs of the next layer equals to the number of
		// outputs in the previous layer.
		final MultiLayerConfiguration configuration = new NeuralNetConfiguration.Builder().seed(12345)
				.trainingWorkspaceMode(WorkspaceMode.ENABLED).weightInit(WeightInit.XAVIER).updater(new AdaGrad(0.5))
				.activation(Activation.RELU).optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
				.l2(0.0006).list()
				// First hidden layer
				.layer(0,
						new DenseLayer.Builder().nIn(numberOfInputs).nOut(neurons).weightInit(WeightInit.RELU)
								.activation(Activation.RELU).build())
				// Second hidden layer
				.layer(1,
						new DenseLayer.Builder().nIn(neurons).nOut(neurons).weightInit(WeightInit.RELU)
								.activation(Activation.RELU).build())
				// Third hidden layer
				.layer(2,
						new DenseLayer.Builder().nIn(neurons).nOut(neurons).weightInit(WeightInit.RELU)
								.activation(Activation.RELU).build())
				// Output layer
				.layer(3,
						new OutputLayer.Builder().nIn(neurons).nOut(numberOfOutputs).weightInit(WeightInit.RELU)
								.activation(Activation.SOFTMAX)
								.lossFunction(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD).build())
				.backpropType(BackpropType.Standard).build();

		this.network = new MultiLayerNetwork(configuration);
		this.network.init();

		// // Initialize the user interface backend
		// UIServer uiServer = UIServer.getInstance();

		// // Configure where the network information (gradients, score vs. time etc) is
		// to
		// // be stored. Here: store in memory.
		// StatsStorage statsStorage = new InMemoryStatsStorage(); // Alternative: new
		// FileStatsStorage(File), for saving
		// // and loading later

		// // Attach the StatsStorage instance to the UI: this allows the contents of
		// the
		// // StatsStorage to be visualized
		// uiServer.attach(statsStorage);

		// // Then add the StatsListener to collect this information from the network,
		// as
		// // it trains
		// this.network.setListeners(new StatsListener(statsStorage));
	}

	public void updateEpsilon() {
		epsilon -= 0.001;
	}

	public Action getActionFromStates(final float[] states) {
		return epsilonGreedyAction(states, epsilon);
	}

	public Action epsilonGreedyAction(final float[] states, final double epsilon) {
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

	private final Map<String, Double> qTable = new HashMap<>();

	public void update(final float[] states, final Action action, final double score, final float[] nextState) {
		if (score >= 100) {
			final String scoreStr = Double.toString(score);
			LOGGER.info(scoreStr);
		}
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

		return score;
	}
}
