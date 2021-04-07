package intelligence.Maddpg;

import java.util.Arrays;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.BackpropType;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import intelligence.Inteligence;
import kotlin.NotImplementedError;
import robots.Action;
import robots.RobotController;

public class Actor {
    private final MultiLayerNetwork net;
    private static final int HIDDEN_NEURONS = 64;
    private static final double LR_ACTOR = 1e-4;

    private static final MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
            .seed(12345).optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
            .weightInit(WeightInit.RELU).updater(new Adam(LR_ACTOR))
            .gradientNormalizationThreshold(0.5).miniBatch(true).dropOut(0.8).list()
            .layer(0, new DenseLayer.Builder().nIn(RobotController.STATE_COUNT).nOut(HIDDEN_NEURONS)
                    .dropOut(0.5).weightInit(WeightInit.RELU).activation(Activation.RELU).build())
            .layer(1,
                    new DenseLayer.Builder().nIn(HIDDEN_NEURONS).nOut(HIDDEN_NEURONS).dropOut(0.5)
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

    public Action forward(final Boolean[] state) {
        // INDArray x = this.net.getLayer(0).activate(toINDArray(state), false, null);
        // x = this.net.getLayer(1).activate(x, false, null);
        // x = this.net.getLayer(2).activate(x, false, null);
        // return Action.getActionByIndex(
        // getMaxValueIndex(this.net.getLayer(3).activate(x, false, null).data().asFloat()));
        final var x = this.net.output(toINDArray(state));
        return Action.getActionByIndex(getMaxValueIndex(x.data().asFloat()));
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

    // network.fit(stateObservation, updatedOutput);
    // }


    // public void update(final Boolean[] state) {
    // final INDArray data = toINDArray(state);
    // this.net.fit(data, labels);
    // }

    // public INDArray forward(final Boolean[] state) {
    // INDArray x = this.net.getLayer(0).activate(toINDArray(state), false, null);
    // x = this.net.getLayer(1).activate(x, false, null);
    // x = this.net.getLayer(2).activate(x, false, null);
    // return this.net.getLayer(3).activate(x, false, null);
    // }

    private int getMaxValueIndex(final float[] values) {
        int maxAt = 0;

        for (int i = 0; i < values.length; i++) {
            maxAt = values[i] > values[maxAt] ? i : maxAt;
        }

        return maxAt;
    }

    private static INDArray toINDArray(final Boolean[] states) {
        return Nd4j.create(
                new double[][] {Arrays.stream(states).mapToDouble(i -> i ? 1 : 0).toArray()});
    }

    public double[] fromINDArrayVector(final INDArray indArray) {
        final double[] result = new double[Action.LENGTH];
        for (int i = 0; i < Action.LENGTH; i++) {
            result[i] = indArray.getDouble(i);
        }
        return result;
    }

}
