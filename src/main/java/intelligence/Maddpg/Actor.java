package intelligence.Maddpg;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import org.deeplearning4j.core.storage.StatsStorage;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.BackpropType;
import org.deeplearning4j.nn.conf.GradientNormalization;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.WorkspaceMode;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.conf.layers.RnnOutputLayer;
import org.deeplearning4j.nn.gradient.Gradient;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
// import org.deeplearning4j.rl4j.network.ac.ActorCriticLoss;
import org.deeplearning4j.ui.api.UIServer;
import org.deeplearning4j.ui.model.stats.StatsListener;
import org.deeplearning4j.ui.model.storage.InMemoryStatsStorage;
import org.nd4j.common.primitives.Pair;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.activations.IActivation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.lossfunctions.ILossFunction;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.nd4j.linalg.lossfunctions.LossUtil;
import org.nd4j.linalg.ops.transforms.Transforms;
import org.nd4j.shade.guava.primitives.Booleans;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import intelligence.Network;
import robots.Action;
import robots.Hunter;
import simulation.Env;

public class Actor implements Network {
	private static final Logger LOG = LoggerFactory.getLogger(Actor.class.getName());
	private final MultiLayerNetwork net;
	private static final int HIDDEN_NEURONS = 64;
	private static final double LR_ACTOR = 5e-2;

	private final MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder().seed(12345)
			// Optimiser
			.optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
			// Workspace
			.trainingWorkspaceMode(WorkspaceMode.ENABLED)
			// Weight init
			.weightInit(WeightInit.RELU)
			// Updater
			// .updater(new Adam(LR_ACTOR))
			// .updater(new Adam(LR_ACTOR, 0.9, 0.999, 1e-08))
			.updater(new Adam(LR_ACTOR, 0.9, 0.999, 1))
			// .updater(new Sgd(LR_ACTOR))
			// Gradient Notmalisation
			// .gradientNormalizationThreshold(0.5)
			.gradientNormalization(GradientNormalization.ClipL2PerLayer)
			// Drop out amount
			.dropOut(0.8)
			// .l2(0.001)
			// Layers
			.list()
			.layer(0,
					new DenseLayer.Builder().nIn(Hunter.OBSERVATION_COUNT).nOut(512).dropOut(0.5)
							.weightInit(WeightInit.RELU).activation(Activation.RELU).build())
			.layer(1,
					new DenseLayer.Builder().nIn(512).nOut(128).dropOut(0.5)
							.weightInit(WeightInit.RELU).activation(Activation.RELU).build())
			.layer(2,
					new DenseLayer.Builder().nIn(128).nOut(32).dropOut(0.5)
							.weightInit(WeightInit.RELU).activation(Activation.RELU).build())
			.layer(3, new OutputLayer.Builder(LossFunctions.LossFunction.MSE).nIn(32)
					// new OutputLayer.Builder(new ActorCriticLoss()).nIn(32)
					.nOut(Action.LENGTH).weightInit(WeightInit.RELU).activation(Activation.IDENTITY)
					// .activation(Activation.SOFTMAX)
					.build())

			.backpropType(BackpropType.Standard).build();

	public Actor(final String type) {
		this.net = new MultiLayerNetwork(conf);
		this.net.init();

		if (!type.equals("TARGET")) {
			enableUIServer(this.net);
		}
	}

	public Actor(final File fileName) {
		this.net = loadNetwork(fileName, false);
		this.net.init();
	}

	private static void enableUIServer(final MultiLayerNetwork net) {
		// Initialize the user interface backend
		final UIServer uiServer = UIServer.getInstance();
		final StatsStorage statsStorage = new InMemoryStatsStorage();
		uiServer.attach(statsStorage);
		net.setListeners(new StatsListener(statsStorage));

		// this will limit frequency of gc calls to 5000 milliseconds
		Nd4j.getMemoryManager().togglePeriodicGc(false);
	}

	/**
	 * Forward pass with given data
	 */
	@Override
	public INDArray predict(final INDArray state) {
		return this.net.output(state);
	}

	/**
	 * Convert Array to INDArray and cast to int
	 *
	 * @param states
	 * @return
	 */
	public INDArray toINDArray(final Boolean[] states) {
		// final float[] arr = new float[states.length];
		// for (int i = 0; i < arr.length; i++) {
		// arr[i] = states[i];
		// }

		// // double[] array = Arrays.stream(states).mapToDouble(i -> i).toArray();
		// return Nd4j.create(new float[][] {arr});
		return Nd4j.create(new boolean[][] {Booleans.toArray(Arrays.asList(states))});
	}

	public void updateGradient(final Gradient gradient) {
		// this.net.getUpdater().update(this.net, gradient, 0, 0, 1,
		// LayerWorkspaceMgr.noWorkspaces());
		// this.net.update(gradient);
		this.net.computeGradientAndScore();
		this.net.params().subi(gradient.gradient());
	}

	@Override
	public MultiLayerNetwork getNetwork() {
		return this.net;
	}

	@Override
	public void update(final INDArray inputs, final INDArray outputs) {
		// Not needed for this model, as gradient is updated from critic method
	}

	@Override
	public Gradient getGradient(final INDArray inputs, final INDArray labels) {
		this.net.setInput(inputs);
		this.net.setLabels(labels);
		this.net.computeGradientAndScore();
		return this.net.gradient();
	}

	public void applyGradient(final Gradient gradient) {
		this.net.params().subi(gradient.gradient());
	}

	public void applyGradient(final INDArray gradient) {
		this.net.params().subi(gradient);
	}

	// private double get_OUnoise(double thresholdUtility) {
	// // https://towardsdatascience.com/deep-deterministic-policy-gradients-explained-2d94655a9b7b
	// double low = 0.85;
	// double high = 1.0;
	// double ouNoise = 0.3 * Math.random(); // random num between 0.0 and 1.0
	// double result = thresholdUtility + ouNoise;
	// if (result < low)
	// result = low;
	// if (result > high)
	// result = high;
	// return result;
	// }

	/**
	 * Saves Actor the network
	 *
	 * @param fileName
	 */
	@Override
	public void saveNetwork(final String fileName) {
		try {
			this.net.save(new File(fileName), true);
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Loads network
	 *
	 * @param fileName
	 * @return Multi Layered Network
	 */
	@Override
	public MultiLayerNetwork loadNetwork(final File file, final boolean moreTraining) {
		try {
			final String msg = "Loading Network: " + file.getName();
			LOG.info(msg);
			return MultiLayerNetwork.load(file, true);
		} catch (final IOException e) {
			e.printStackTrace();
		}

		return null;
	}

	// class ActorCriticLoss implements ILossFunction {

	// public static final double BETA = 0.01;

	// private INDArray scoreArray(INDArray labels, INDArray preOutput, IActivation activationFn,
	// INDArray mask) {
	// INDArray output = activationFn.getActivation(preOutput.dup(), true).addi(1e-5);
	// INDArray logOutput = Transforms.log(output, true);
	// INDArray entropy = output.muli(logOutput);
	// INDArray scoreArr = logOutput.muli(labels).subi(entropy.muli(BETA));

	// if (mask != null) {
	// LossUtil.applyMask(scoreArr, mask);
	// }
	// return scoreArr;
	// }

	// @Override
	// public double computeScore(INDArray labels, INDArray preOutput, IActivation activationFn,
	// INDArray mask, boolean average) {
	// INDArray scoreArr = scoreArray(labels, preOutput, activationFn, mask);
	// double score = -scoreArr.sumNumber().doubleValue();
	// return average ? score / scoreArr.size(0) : score;
	// }

	// @Override
	// public INDArray computeScoreArray(INDArray labels, INDArray preOutput,
	// IActivation activationFn, INDArray mask) {
	// INDArray scoreArr = scoreArray(labels, preOutput, activationFn, mask);
	// return scoreArr.sum(1).muli(-1);
	// }

	// @Override
	// public INDArray computeGradient(INDArray labels, INDArray preOutput,
	// IActivation activationFn, INDArray mask) {
	// INDArray output = activationFn.getActivation(preOutput.dup(), true).addi(1e-5);
	// INDArray logOutput = Transforms.log(output, true);
	// INDArray entropyDev = logOutput.addi(1);
	// INDArray dLda = output.rdivi(labels).subi(entropyDev.muli(BETA)).negi();
	// INDArray grad = activationFn.backprop(preOutput, dLda).getFirst();

	// if (mask != null) {
	// LossUtil.applyMask(grad, mask);
	// }
	// return grad;
	// }

	// @Override
	// public Pair<Double, INDArray> computeGradientAndScore(INDArray labels, INDArray preOutput,
	// IActivation activationFn, INDArray mask, boolean average) {
	// return new Pair<>(computeScore(labels, preOutput, activationFn, mask, average),
	// computeGradient(labels, preOutput, activationFn, mask));
	// }

	// @Override
	// public String toString() {
	// return "ActorCriticLoss()";
	// }

	// @Override
	// public String name() {
	// return toString();
	// }
	// }
}
