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
import org.nd4j.linalg.lossfunctions.LossFunctions;
import robots.Action;
import robots.RobotController;

public class Actor {
	public final MultiLayerNetwork net;
	private static final int HIDDEN_NEURONS = 64;
	private static final double LR_ACTOR = 1e-4;

	private static final MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
			.seed(12345)
			// Optimiser
			.optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
			// Workspace
			.trainingWorkspaceMode(WorkspaceMode.ENABLED)
			// Weight init
			.weightInit(WeightInit.RELU)
			// Updater
			// .updater(new Adam(LR_ACTOR))
			.updater(new Adam(0.001, 0.9, 0.999, 1e-08))
			// .updater(new Adam(3e-4, 0.9, 0.999, 0.1))
			// .updater(new Adam(0.0005, 0.9, 0.999, 0.1))
			// .updater(new Sgd(LR_ACTOR))
			// Gradient Notmalisation
			.gradientNormalizationThreshold(0.5)
			.gradientNormalization(GradientNormalization.ClipL2PerLayer)
			// Drop out amount
			.dropOut(0.8)
			// Layers
			.list()
			.layer(0, new DenseLayer.Builder().nIn(RobotController.OBSERVATION_COUNT).nOut(512)
					.dropOut(0.5).weightInit(WeightInit.RELU).activation(Activation.RELU).build())
			.layer(1,
					new DenseLayer.Builder().nIn(512).nOut(128).dropOut(0.5)
							.weightInit(WeightInit.RELU).activation(Activation.RELU).build())
			.layer(2,
					new DenseLayer.Builder().nIn(HIDDEN_NEURONS).nOut(HIDDEN_NEURONS).dropOut(0.5)
							.weightInit(WeightInit.RELU).activation(Activation.RELU).build())
			.layer(3,
					new OutputLayer.Builder(LossFunctions.LossFunction.MSE).nIn(HIDDEN_NEURONS)
							.nOut(Action.LENGTH).weightInit(WeightInit.RELU)
							.activation(Activation.TANH).weightInit(WeightInit.RELU).build())
			.backpropType(BackpropType.Standard).build();

	public Actor() {
		this.net = new MultiLayerNetwork(conf);
		this.net.init();
	}

	/**
	 * Forward pass with given data
	 */
	public INDArray forward(final INDArray state) {
		// INDArray x = this.net.getLayer(0).activate(toINDArray(state), false, null);
		// x = this.net.getLayer(1).activate(x, false, null);
		// x = this.net.getLayer(2).activate(x, false, null);
		// return Action.getActionByIndex(
		// getMaxValueIndex(this.net.getLayer(3).activate(x, false, null).toFloatVector()));
		return this.net.output(state);
	}

	/**
	 * Convert Array to INDArray and cast to int
	 *
	 * @param states
	 * @return
	 */
	public INDArray toINDArray(final Float[] states) {
		float[] arr = new float[states.length];
		for (int i = 0; i < arr.length; i++) {
			arr[i] = states[i];
		}

		// double[] array = Arrays.stream(states).mapToDouble(i -> i).toArray();
		return Nd4j.create(new float[][] {arr});
	}

}
