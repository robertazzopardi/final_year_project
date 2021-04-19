package intelligence.Maddpg;

import java.io.File;
import java.io.IOException;
import org.deeplearning4j.core.storage.StatsStorage;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.BackpropType;
import org.deeplearning4j.nn.conf.GradientNormalization;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.WorkspaceMode;
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
import intelligence.Network;
import robots.Action;
import robots.Hunter;

public class Actor implements Network {
	private final MultiLayerNetwork net;
	private static final int HIDDEN_NEURONS = 64;
	private static final double LR_ACTOR = 6e-3;

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
			.updater(new Adam(LR_ACTOR, 0.9, 0.999, 0.1))
			// .updater(new Sgd(LR_ACTOR))
			// Gradient Notmalisation
			// .gradientNormalizationThreshold(0.5)
			.gradientNormalization(GradientNormalization.ClipL2PerLayer)
			// Drop out amount
			.dropOut(0.8)
			// Layers
			.list()
			// .layer(0,
			// new DenseLayer.Builder().nIn(Hunter.OBSERVATION_COUNT).nOut(512).dropOut(0.5)
			// .weightInit(WeightInit.RELU).activation(Activation.RELU).build())
			// .layer(1,
			// new DenseLayer.Builder().nIn(512).nOut(128).dropOut(0.5)
			// .weightInit(WeightInit.RELU).activation(Activation.RELU).build())
			// .layer(2,
			// new DenseLayer.Builder().nIn(HIDDEN_NEURONS).nOut(HIDDEN_NEURONS).dropOut(0.5)
			// .weightInit(WeightInit.RELU).activation(Activation.RELU).build())
			// .layer(3,
			// new OutputLayer.Builder(LossFunctions.LossFunction.MSE).nIn(HIDDEN_NEURONS)
			// .nOut(Action.LENGTH).weightInit(WeightInit.RELU)
			// .activation(Activation.TANH).build())

			.layer(0,
					new DenseLayer.Builder().nIn(Hunter.OBSERVATION_COUNT).nOut(512).dropOut(0.5)
							.weightInit(WeightInit.RELU).activation(Activation.RELU).build())
			.layer(1,
					new DenseLayer.Builder().nIn(512).nOut(128).dropOut(0.5)
							.weightInit(WeightInit.RELU).activation(Activation.RELU).build())
			.layer(2,
					new DenseLayer.Builder().nIn(HIDDEN_NEURONS).nOut(HIDDEN_NEURONS).dropOut(0.5)
							.weightInit(WeightInit.RELU).activation(Activation.RELU).build())
			.layer(3,
					new OutputLayer.Builder(LossFunctions.LossFunction.MSE).nIn(HIDDEN_NEURONS)
							.nOut(Action.LENGTH).weightInit(WeightInit.RELU)
							.activation(Activation.IDENTITY).build())

			.backpropType(BackpropType.Standard).build();

	public Actor(final String type) {
		this.net = new MultiLayerNetwork(conf);
		this.net.init();

		// if (type != "TARGET") {
		// enableUIServer(this.net);
		// }
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
	public INDArray toINDArray(final Float[] states) {
		final float[] arr = new float[states.length];
		for (int i = 0; i < arr.length; i++) {
			arr[i] = states[i];
		}

		// double[] array = Arrays.stream(states).mapToDouble(i -> i).toArray();
		return Nd4j.create(new float[][] {arr});
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
		} catch (IOException e) {
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
			System.out.println("Loading Network: " + file.getName());
			return MultiLayerNetwork.load(file, true);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}

}
