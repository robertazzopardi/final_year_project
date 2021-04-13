package intelligence.Maddpg;

import org.deeplearning4j.core.storage.StatsStorage;
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
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.learning.config.Sgd;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import robots.Action;
import robots.RobotController;

/**
 * Defines the Critic Neural Network
 */
public class Critic {
	public final MultiLayerNetwork net;
	private static final double LR_CRITIC = 3e-4;

	private static final MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
			.seed(12345)
			// Optimiser
			.optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
			// Workspace
			.trainingWorkspaceMode(WorkspaceMode.ENABLED)
			// Weight Init
			.weightInit(WeightInit.RELU)
			// Updater
			// .updater(new Adam(LR_CRITIC))
			// .updater(new Adam(0.006, 0.9, 0.999, 1e-08))
			.updater(new Adam(6e-3, 0.9, 0.999, 0.1))
			// .updater(new Adam(0.0005, 0.9, 0.999, 0.1))
			// .updater(new Sgd(LR_CRITIC))
			// Gradient Normaliser
			.gradientNormalization(GradientNormalization.ClipL2PerLayer)
			.gradientNormalizationThreshold(0.5)
			// Dropout amount
			.dropOut(0.8).list()
			.layer(0, new DenseLayer.Builder()
					.nIn((RobotController.OBSERVATION_COUNT * 4) + Action.LENGTH).nOut(512)
					.dropOut(0.5).weightInit(WeightInit.RELU).activation(Activation.RELU).build())
			.layer(1,
					new DenseLayer.Builder().nIn(512).nOut(256).dropOut(0.5)
							.weightInit(WeightInit.RELU).activation(Activation.RELU).build())
			.layer(2,
					new OutputLayer.Builder(LossFunctions.LossFunction.MSE).nIn(150).nOut(1)
							.weightInit(WeightInit.RELU).activation(Activation.IDENTITY).build())
			.backpropType(BackpropType.Standard).build();

	public Critic(final String type) {
		this.net = new MultiLayerNetwork(conf);
		this.net.init();

		if (type != "TARGET") {
			enableUIServer();
		}
	}

	private void enableUIServer() {
		// Initialize the user interface backend
		final UIServer uiServer = UIServer.getInstance();
		final StatsStorage statsStorage = new InMemoryStatsStorage();
		uiServer.attach(statsStorage);
		this.net.setListeners(new StatsListener(statsStorage));

		// this will limit frequency of gc calls to 5000 milliseconds
		Nd4j.getMemoryManager().togglePeriodicGc(false);
	}

	/**
	 * Forward pass of the data to the network
	 *
	 * @param states
	 * @param actions
	 * @return QValue
	 */
	public INDArray forward(final INDArray states, final INDArray actions) {
		return this.net.output(Nd4j.concat(1, states, actions));
	}

}
