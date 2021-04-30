package intelligence.Maddpg;

import static org.nd4j.linalg.ops.transforms.Transforms.exp;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Random;

import org.deeplearning4j.core.storage.StatsStorage;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.BackpropType;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.WorkspaceMode;
import org.deeplearning4j.nn.conf.inputs.InputType;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.gradient.Gradient;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.nn.workspace.LayerWorkspaceMgr;
import org.deeplearning4j.optimize.api.TrainingListener;
import org.deeplearning4j.ui.api.UIServer;
import org.deeplearning4j.ui.model.stats.StatsListener;
import org.deeplearning4j.ui.model.storage.InMemoryStatsStorage;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.nd4j.shade.guava.primitives.Booleans;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import intelligence.Network;
import robots.Agent;
import simulation.Env;

public class Actor implements Network {
	private static final Logger LOG = LoggerFactory.getLogger(Actor.class.getName());
	private final MultiLayerNetwork net;
	private static final Random RANDOM = new Random(12345);

	public Actor(final String type, final int inputs, final int outputs) {
		this.net = new MultiLayerNetwork(getNetworkConfiguration(inputs, outputs));
		this.net.init();

		if (!type.equals(Agent.TARGET)) {
			enableUIServer(this.net);
		}
	}

	public Actor(final String type, final File fileName, final int inputs, final int outputs,
			final boolean moreTraining) {
		this.net = loadNetwork(fileName, moreTraining, inputs, outputs);
		this.net.init();

		if (!type.equals(Agent.TARGET)) {
			enableUIServer(this.net);
		}
	}

	public Actor(final String type, final MultiLayerNetwork network) {
		this.net = network;
		this.net.init();

		if (!type.equals(Agent.TARGET)) {
			enableUIServer(this.net);
		}
	}

	// private MultiLayerConfiguration getNetworkConfiguration(final int inputs,
	// final int outputs) {
	// final NeuralNetConfiguration.ListBuilder conf = new
	// NeuralNetConfiguration.Builder().seed(123)
	// .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT).updater(new
	// Adam())
	// .weightInit(WeightInit.XAVIER).l2(0.001).list()
	// .layer(0, new
	// DenseLayer.Builder().nIn(inputs).nOut(512).activation(Activation.RELU).build());

	// conf.layer(1, new
	// DenseLayer.Builder().nIn(512).nOut(128).activation(Activation.RELU).build());
	// conf.layer(2, new
	// OutputLayer.Builder(LossFunctions.LossFunction.MSE).activation(
	// // Activation.SOFTMAX
	// // Activation.RELU
	// Activation.IDENTITY).nIn(128).nOut(outputs).build());

	// conf.setInputType(InputType.feedForward(inputs));
	// return conf.backpropType(BackpropType.Standard).build();
	// }

	private MultiLayerConfiguration getNetworkConfiguration(final int inputs, final int outputs) {
		return new NeuralNetConfiguration.Builder().seed(12345)
				.optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
				.trainingWorkspaceMode(WorkspaceMode.ENABLED).weightInit(WeightInit.RELU).activation(Activation.RELU)
				.updater(new Adam()).dropOut(0.8).list()
				.layer(0,
						new DenseLayer.Builder().nIn(inputs).nOut(512).dropOut(0.5).weightInit(WeightInit.RELU)
								.activation(Activation.RELU).build())
				.layer(1,
						new DenseLayer.Builder().nIn(512).nOut(300).dropOut(0.5).weightInit(WeightInit.RELU)
								.activation(Activation.RELU).build())
				.layer(2, new OutputLayer.Builder(LossFunctions.LossFunction.MSE).activation(
						// Activation.TANH
						// Activation.IDENTITY
						Activation.LEAKYRELU
				// Activation.SOFTMAX
				).nIn(300).nOut(outputs).build()).backpropType(BackpropType.Standard).build();
	}

	private static void enableUIServer(final MultiLayerNetwork net) {
		// Initialize the user interface backend
		final UIServer uiServer = UIServer.getInstance();
		final StatsStorage statsStorage = new InMemoryStatsStorage();
		uiServer.attach(statsStorage);
		net.setListeners(new StatsListener(statsStorage));

		// this will limit frequency of gc calls to 5000 milliseconds
		// Nd4j.getMemoryManager().togglePeriodicGc(false);
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
		return Nd4j.create(new boolean[][] { Booleans.toArray(Arrays.asList(states)) });
	}

	@Override
	public MultiLayerNetwork getNetwork() {
		return this.net;
	}

	@Override
	public void update(final INDArray inputs, final INDArray outputs) {
		this.net.fit(inputs, outputs);
	}

	@Override
	public Gradient getGradient(final INDArray inputs, final INDArray labels) {
		this.net.setInput(inputs);
		this.net.setLabels(labels);
		this.net.computeGradientAndScore();
		final Collection<TrainingListener> iterListeners = this.net.getListeners();
		if (iterListeners != null && !iterListeners.isEmpty()) {
			for (final TrainingListener l : iterListeners) {
				l.onGradientCalculation(this.net);
			}
		}

		return this.net.gradient();
	}

	@Override
	public void updateGradient(final Gradient gradient) {
		final MultiLayerConfiguration config = this.net.getLayerWiseConfigurations();
		final int iterCount = config.getIterationCount();
		final int epochs = config.getEpochCount();
		this.net.getUpdater().update(this.net, gradient, iterCount, epochs, Env.BATCH_SIZE,
				LayerWorkspaceMgr.noWorkspaces());
		this.net.params().subi(gradient.gradient());
		final Collection<TrainingListener> iterListeners = this.net.getListeners();
		if (iterListeners != null && !iterListeners.isEmpty()) {
			for (final TrainingListener listener : iterListeners) {
				listener.iterationDone(this.net, iterCount, epochs);
			}
		}
		config.setIterationCount(iterCount + 1);
	}

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
	public MultiLayerNetwork loadNetwork(final File file, final boolean moreTraining, final int inputs,
			final int outputs) {
		if (file == null) {
			LOG.info("Loading untrained network");
			return new MultiLayerNetwork(getNetworkConfiguration(inputs, outputs));
		}
		try {
			final String msg = "Loading Network: " + file.getName();
			LOG.info(msg);
			return MultiLayerNetwork.load(file, true);
		} catch (final IOException e) {
			e.printStackTrace();
		}

		return null;
	}

	public int boltzmannDistribution(final INDArray output, final int shape) {
		final INDArray exp = exp(output);
		final double sum = exp.sum(shape).getDouble(0);

		double picked = RANDOM.nextDouble() * sum;

		for (int i = 0; i < exp.columns(); i++) {
			if (picked < exp.getDouble(i))
				return i;
			picked -= exp.getDouble(i);
		}
		return (int) output.length() - 1;
	}

	public int distribution(final INDArray output) {
		float rVal = RANDOM.nextFloat();
		for (int i = 0; i < output.length(); i++) {
			if (rVal < output.getFloat(i)) {
				return i;
			} else
				rVal -= output.getFloat(i);
		}

		throw new RuntimeException("Output from network is not a probability distribution: " + output);
	}

	public int nextAction(final INDArray output, final int shape) {
		return boltzmannDistribution(output, shape);
		// return distribution(output);
	}

}
