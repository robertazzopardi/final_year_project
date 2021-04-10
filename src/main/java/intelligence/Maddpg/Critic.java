package intelligence.Maddpg;

import org.deeplearning4j.nn.api.Layer;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.BackpropType;
import org.deeplearning4j.nn.conf.GradientNormalization;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.FeedForwardLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.gradient.Gradient;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.nn.workspace.LayerWorkspaceMgr;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.activations.impl.ActivationReLU;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import robots.Action;
import robots.RobotController;

public class Critic {

	public final MultiLayerNetwork net;
	private static final int HIDDEN_NEURONS = 64;
	private static final double LR_CRITIC = 3e-4;

	private static final MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
			.seed(12345).optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
			.weightInit(WeightInit.RELU).updater(new Adam(LR_CRITIC))
			.gradientNormalization(GradientNormalization.ClipL2PerLayer)
			.gradientNormalizationThreshold(0.5).miniBatch(true).dropOut(0.8).list()
			.layer(0, new DenseLayer.Builder()
					.nIn((RobotController.OBSERVATION_COUNT * 4) + Action.LENGTH).nOut(1024)
					.dropOut(0.5).weightInit(WeightInit.RELU).activation(Activation.RELU).build())
			.layer(1,
					new DenseLayer.Builder().nIn(1024).nOut(512).dropOut(0.5)
							.weightInit(WeightInit.RELU).activation(Activation.RELU).build())
			.layer(2,
					new DenseLayer.Builder().nIn(512).nOut(300).dropOut(0.5)
							.weightInit(WeightInit.RELU).activation(Activation.RELU).build())
			.layer(3,
					new OutputLayer.Builder(LossFunctions.LossFunction.MSE).nIn(300).nOut(1)
							.weightInit(WeightInit.RELU).activation(Activation.IDENTITY)
							.weightInit(WeightInit.RELU).build())
			.backpropType(BackpropType.Standard).build();

	public Critic() {
		this.net = new MultiLayerNetwork(conf);
		this.net.init();
	}

	// public INDArray forward(final Boolean[] states, final Action[] actions) {
	// // System.out.println(states.shapeInfoToString() + " " + actions.shapeInfoToString());
	// final INDArray x = this.net.getLayer(0).activate(toINDArray(states), false,
	// LayerWorkspaceMgr.noWorkspaces());
	// final INDArray xaCat = Nd4j.concat(1, x, toINDArray(actions));
	// INDArray xa = this.net.getLayer(1).activate(xaCat, false, LayerWorkspaceMgr.noWorkspaces());
	// xa = this.net.getLayer(2).activate(xa, false, LayerWorkspaceMgr.noWorkspaces());
	// return this.net.getLayer(3).activate(xa, false, LayerWorkspaceMgr.noWorkspaces());
	// }

	// public INDArray forward(final INDArray states, final INDArray actions) {
	// // System.out.println(states.shapeInfoToString() + " " + actions.shapeInfoToString());
	// final INDArray x =
	// this.net.getLayer(0).activate(states, false, LayerWorkspaceMgr.noWorkspaces());
	// final INDArray xaCat = Nd4j.concat(1, x, actions);

	// System.out.println(xaCat.shapeInfoToString() + "\n" + this.net.getLayer(1).toString());

	// INDArray xa = this.net.getLayer(1).activate(xaCat, false, LayerWorkspaceMgr.noWorkspaces());

	// xa = this.net.getLayer(2).activate(xa, false, LayerWorkspaceMgr.noWorkspaces());
	// return this.net.getLayer(3).activate(xa, false, LayerWorkspaceMgr.noWorkspaces());
	// }

	public INDArray forward(final INDArray states, final INDArray actions) {
		// System.out.println(states.shapeInfoToString() + " " + actions.shapeInfoToString());
		// final INDArray x =
		// this.net.getLayer(0).activate(states, false, LayerWorkspaceMgr.noWorkspaces());
		final INDArray xaCat = Nd4j.concat(1, states, actions);

		// System.out.println(xaCat.shapeInfoToString() + "\n" + this.net.getLayer(1).toString());

		// INDArray xa = this.net.getLayer(1).activate(xaCat, false,
		// LayerWorkspaceMgr.noWorkspaces());

		// xa = this.net.getLayer(2).activate(xa, false, LayerWorkspaceMgr.noWorkspaces());
		// return this.net.getLayer(3).activate(xa, false, LayerWorkspaceMgr.noWorkspaces());
		return this.net.output(xaCat);
	}

	public INDArray toINDArray(final Boolean[] states) {
		// return Nd4j.create(
		// new float[][] {Arrays.stream(states).map(i -> i ? 1f : 0f).toArray(Float[]::new)});
		float[] s = new float[states.length];
		for (int i = 0; i < states.length; i++) {
			s[i] = states[i] ? 1 : 0;
		}

		return Nd4j.create(new float[][] {s});
	}

	public INDArray toINDArray(final Action[] actions) {
		// return Nd4j.create(new double[][] {
		// Arrays.stream(actions).mapToDouble(i -> i.getActionIndex()).toArray()});
		float[] s = new float[actions.length];
		for (int i = 0; i < actions.length; i++) {
			s[i] = actions[i].getActionIndex();
		}

		return Nd4j.create(new float[][] {s});
	}

	public Gradient gradient(final INDArray input, final INDArray labels) {
		net.setInput(input);
		net.setLabels(labels);
		net.computeGradientAndScore();
		return net.gradient();
	}

	public void update(final Boolean[] globalStateBatch, final Action[] globalActionBatch) {
		this.net.fit(toINDArray(globalStateBatch), toINDArray(globalActionBatch));
	}

	// public void update(final Boolean[] states, final Action action, final double score,
	// final Boolean[] newObservations) {

	// // Get max q score for next state
	// final double maxQScore = getMaxQScore(newObservations);

	// // Calculate target score
	// final double targetScore = score + (0.9 * maxQScore);

	// // Update the table with new score
	// qTable.put(makeKey(Arrays.toString(states), action), targetScore);

	// // Update network
	// final INDArray stateObservation = toINDArray(states);
	// final INDArray output = network.output(stateObservation);
	// final INDArray updatedOutput = output.putScalar(action.getActionIndex(), targetScore);

	// // System.out.println(stateObservation + " " + updatedOutput);

	// this.net.fit(stateObservation, updatedOutput);
	// }

}
