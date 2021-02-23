package intelligence;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.conf.BackpropType;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;

import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.AdaGrad;
import org.nd4j.linalg.lossfunctions.LossFunctions;

import robots.Action;

public class DeepQLearning {
	private final MultiLayerNetwork network;

	private double epsilon = 0.9;

	public DeepQLearning(final int numberOfInputs, final int numberOfOutputs) {
		final int neurons = numberOfInputs + 1;

		// Just make sure the number of inputs of the next layer equals to the number of
		// outputs in the previous layer.
		final MultiLayerConfiguration configuration = new NeuralNetConfiguration.Builder().seed(12345)
				.weightInit(WeightInit.RELU).updater(new AdaGrad(0.5)).activation(Activation.RELU)
				.optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT).l2(0.0001).list()
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
	}

	public void updateEpsilon() {
		epsilon -= 0.001;
	}

	public Action getActionFromStates(final int[] states) {
		return epsilonGreedyAction(states, epsilon);
	}

	public Action epsilonGreedyAction(final int[] states, final double epsilon) {
		// https://www.geeksforgeeks.org/epsilon-greedy-algorithm-in-reinforcement-learning/
		final double random = getRandomDouble();
		if (random < epsilon) {
			return Action.getRandomAction();
		}

		return getActionFromTheNetwork(states);
	}

	public Action getActionFromTheNetwork(final int[] states) {
		final INDArray output = network.output(toINDArray(states), false);

		// Values provided by the network. Based on them we chose the current best
		// action.

		final float[] outputValues = output.data().asFloat();

		// Find index of the highest value
		final int maxValueIndex = getMaxValueIndex(outputValues);

		return Action.getActionByIndex(maxValueIndex);
	}

	private INDArray toINDArray(final int[] states) {
		return Nd4j.create(new int[][] { states });
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

	public void train(final int[] states, final Action action, final double score, final int[] nextState) {
		// Get max q score for next state
		final double maxQScore = getMaxQScore(nextState);

		// Calculate target score
		final double targetScore = score + (0.9 * maxQScore);

		// Update the table with new score
		qTable.put(getStateWithActionString(Arrays.toString(states), action), targetScore);

		// Update network
		final INDArray stateObservation = toINDArray(states);
		final INDArray output = network.output(stateObservation);
		final INDArray updatedOutput = output.putScalar(action.getActionIndex(), targetScore);

		network.fit(stateObservation, updatedOutput);
	}

	private double getMaxQScore(final int[] states) {
		final String gameStateString = Arrays.toString(states);

		final String stateWithActUP = getStateWithActionString(gameStateString, Action.UP);
		final String stateWithActRIGHT = getStateWithActionString(gameStateString, Action.RIGHT);
		final String stateWithActDOWN = getStateWithActionString(gameStateString, Action.DOWN);
		final String stateWithActLEFT = getStateWithActionString(gameStateString, Action.LEFT);

		qTable.putIfAbsent(stateWithActUP, 0.0);
		qTable.putIfAbsent(stateWithActRIGHT, 0.0);
		qTable.putIfAbsent(stateWithActDOWN, 0.0);
		qTable.putIfAbsent(stateWithActLEFT, 0.0);

		double score = qTable.get(stateWithActUP);

		final Double scoreRight = qTable.get(stateWithActRIGHT);
		if (scoreRight > score) {
			score = scoreRight;
		}

		final Double scoreDown = qTable.get(stateWithActDOWN);
		if (scoreDown > score) {
			score = scoreDown;
		}

		final Double scoreLeft = qTable.get(stateWithActLEFT);
		if (scoreLeft > score) {
			score = scoreLeft;
		}

		return score;
	}

	private String getStateWithActionString(final String stateString, final Action action) {
		return stateString + '-' + action;
	}

	// private Map<String, Double> initQTable() {
	// final HashMap<String, Double> qTable = new HashMap<>();
	// // final List<String> inputs =
	// // getInputs(GameStateHelper.getNumberOfPossibleStates());
	// // final List<String> inputs =
	// // getInputs(GameStateHelper.getNumberOfPossibleStates());

	// // for (final String stateInput : inputs) {
	// // qTable.put(getStateWithActionString(stateInput, 4), 0.0);
	// // qTable.put(getStateWithActionString(stateInput, 1), 0.0);
	// // qTable.put(getStateWithActionString(stateInput, 2), 0.0);
	// // qTable.put(getStateWithActionString(stateInput, 3), 0.0);
	// // }

	// return qTable;
	// }

	// private List<String> getInputs(final int inputCount) {
	// final List<String> inputs = new ArrayList<>();

	// for (int i = 0; i < Math.pow(2, inputCount); i++) {
	// String bin = Integer.toBinaryString(i);
	// while (bin.length() < inputCount) {
	// bin = "0" + bin;
	// }

	// inputs.add(String.copyValueOf(bin.toCharArray()));
	// }

	// return inputs;
	// }
}
