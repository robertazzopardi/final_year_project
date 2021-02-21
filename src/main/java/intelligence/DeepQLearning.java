package intelligence;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

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

final class DeepQLearning extends Intelligence {
	private final MultiLayerNetwork network;

	private static final Random RANDOM = new Random();

	private double epsilon = 0.9;

	// Just make sure the number of inputs of the next layer equals to the number of
	// outputs in the previous layer.
	private static final MultiLayerConfiguration CONFIGURATION = new NeuralNetConfiguration.Builder().seed(12345)
			.weightInit(WeightInit.RELU).updater(new AdaGrad(0.5)).activation(Activation.RELU)
			.optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)// .learningRate(0.05)
			.l2(0.0001).list()
			// First hidden layer
			.layer(0,
					new DenseLayer.Builder().nIn(4).nOut(10).weightInit(WeightInit.RELU).activation(Activation.RELU)
							.build())
			// Second hidden layer
			.layer(1,
					new DenseLayer.Builder().nIn(10).nOut(10).weightInit(WeightInit.RELU).activation(Activation.RELU)
							.build())
			// Third hidden layer
			.layer(2,
					new DenseLayer.Builder().nIn(10).nOut(10).weightInit(WeightInit.RELU).activation(Activation.RELU)
							.build())
			// Output layer
			.layer(3,
					new OutputLayer.Builder().nIn(10).nOut(4).weightInit(WeightInit.RELU).activation(Activation.SOFTMAX)
							.lossFunction(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD).build())
			.backpropType(BackpropType.Standard).build();

	DeepQLearning() {
		this.network = new MultiLayerNetwork(CONFIGURATION);
		this.network.init();
	}

	@Override
	public void updateEpsilon() {
		epsilon -= 0.001;
	}

	@Override
	public int getActionFromState(final int state) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getActionFromStates(final int[] states) {
		return epsilonGreedyAction(states, epsilon);
	}

	@Override
	void train() {
		//
	}

	private static final int[] ACTION = new int[] { 1, 2, 3, 4 };

	public static int getRandomAction() {
		return ACTION[RANDOM.nextInt(ACTION.length)];
	}

	public int epsilonGreedyAction(final int[] states, final double epsilon) {
		// https://www.geeksforgeeks.org/epsilon-greedy-algorithm-in-reinforcement-learning/
		final double random = getRandomDouble();
		if (random < epsilon) {
			return getRandomAction();
		}

		return getActionFromTheNetwork(states);
	}

	public int getActionFromTheNetwork(final int[] states) {
		final INDArray output = network.output(toINDArray(states), false);

		/*
		 * Values provided by the network. Based on them we chose the current best
		 * action.
		 */
		final float[] outputValues = output.data().asFloat();

		// Find index of the highest value
		final int maxValueIndex = getMaxValueIndex(outputValues);

		// final int actionByIndex = ACTION[maxValueIndex];

		// return actionByIndex;
		return ACTION[maxValueIndex];
	}

	private INDArray toINDArray(final int[] states) {
		// return Nd4j.create(new boolean[][] { Booleans.toArray(Arrays.asList(states))
		// });
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

	private final Map<String, Double> Q_TABLE = new HashMap<>();// = initQTable();

	@Override
	public void update(final int[] states, final int action, final double score, final int[] nextState) {
		// Get max q score for next state
		final double maxQScore = getMaxQScore(nextState);

		// Calculate target score
		final double targetScore = score + (0.9 * maxQScore);

		// Update the table with new score
		// Q_TABLE.put(getStateWithActionString(state.getGameStateString(), action),
		// targetScore);
		Q_TABLE.put(getStateWithActionString(Arrays.toString(states), action), targetScore);

		// Update network
		final INDArray stateObservation = toINDArray(states);
		final INDArray output = network.output(stateObservation);
		final INDArray updatedOutput = output.putScalar((long) action - 1, targetScore);

		network.fit(stateObservation, updatedOutput);
	}

	private double getMaxQScore(final int[] states) {
		final String gameStateString = Arrays.toString(states);

		final String stateWithActUP = getStateWithActionString(gameStateString, 4);
		final String stateWithActRIGHT = getStateWithActionString(gameStateString, 1);
		final String stateWithActDOWN = getStateWithActionString(gameStateString, 2);
		final String stateWithActLEFT = getStateWithActionString(gameStateString, 3);

		// if (Q_TABLE.isEmpty()) {
		// // System.out.println("egg");
		// Q_TABLE.put(stateWithActUP, 0.0);
		// Q_TABLE.put(stateWithActRIGHT, 0.0);
		// Q_TABLE.put(stateWithActDOWN, 0.0);
		// Q_TABLE.put(stateWithActLEFT, 0.0);
		// }
		Q_TABLE.putIfAbsent(stateWithActUP, 0.0);
		Q_TABLE.putIfAbsent(stateWithActRIGHT, 0.0);
		Q_TABLE.putIfAbsent(stateWithActDOWN, 0.0);
		Q_TABLE.putIfAbsent(stateWithActLEFT, 0.0);

		double score = Q_TABLE.get(stateWithActUP);

		final Double scoreRight = Q_TABLE.get(stateWithActRIGHT);
		if (scoreRight > score) {
			score = scoreRight;
		}

		final Double scoreDown = Q_TABLE.get(stateWithActDOWN);
		if (scoreDown > score) {
			score = scoreDown;
		}

		final Double scoreLeft = Q_TABLE.get(stateWithActLEFT);
		if (scoreLeft > score) {
			score = scoreLeft;
		}

		return score;
	}

	private String getStateWithActionString(final String stateString, final int action) {
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
