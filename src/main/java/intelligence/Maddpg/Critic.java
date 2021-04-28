package intelligence.Maddpg;

import java.io.File;
import java.util.Collection;
import org.deeplearning4j.core.storage.StatsStorage;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.BackpropType;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.WorkspaceMode;
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
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import intelligence.Network;
import robots.RobotController;

/**
 * Defines the Critic Neural Network
 */
public class Critic implements Network {
	private final MultiLayerNetwork net;

	public Critic(final String type, final int inputs) {
		this.net = new MultiLayerNetwork(getNetworkConfiguration(inputs));
		this.net.init();

		if (type != "TARGET") {
			enableUIServer(this.net);
		}
	}

	private MultiLayerConfiguration getNetworkConfiguration(final int inputs) {
		return new NeuralNetConfiguration.Builder().seed(12345)
				.optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
				.trainingWorkspaceMode(WorkspaceMode.ENABLED).weightInit(WeightInit.RELU)
				.updater(new Adam()).dropOut(0.8).list()
				.layer(0,
						new DenseLayer.Builder().nIn(inputs).nOut(512).dropOut(0.5)
								.weightInit(WeightInit.RELU).activation(Activation.RELU).build())
				.layer(1,
						new DenseLayer.Builder().nIn(512).nOut(300).dropOut(0.5)
								.weightInit(WeightInit.RELU).activation(Activation.RELU).build())
				.layer(2,
						new OutputLayer.Builder(LossFunctions.LossFunction.MSE)
								.activation(Activation.IDENTITY).nIn(300).nOut(1).build())
				.backpropType(BackpropType.Standard).build();
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
	 * Forward pass of the data to the network
	 *
	 * @param states
	 * @param actions
	 * @return QValue
	 */
	@Override
	public INDArray predict(final INDArray inputs) {
		return this.net.output(inputs);
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
		this.net.getUpdater().update(this.net, gradient, iterCount, epochs,
				RobotController.BATCH_SIZE, LayerWorkspaceMgr.noWorkspaces());
		this.net.params().subi(gradient.gradient());
		final Collection<TrainingListener> iterListeners = this.net.getListeners();
		if (iterListeners != null && !iterListeners.isEmpty()) {
			for (final TrainingListener listener : iterListeners) {
				listener.iterationDone(this.net, iterCount, epochs);
			}
		}
		config.setIterationCount(iterCount + 1);
	}

	@Override
	public void saveNetwork(final String fileName) {
		// Don't need to save the network
	}

	@Override
	public MultiLayerNetwork loadNetwork(final File file, final boolean moreTraining,
			final int inputs, final int outputs) {
		// Dont need the critic to evaluate
		return null;
	}

}
