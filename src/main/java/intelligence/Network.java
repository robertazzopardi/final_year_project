package intelligence;

import java.io.File;
import org.deeplearning4j.nn.gradient.Gradient;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.linalg.api.ndarray.INDArray;

/**
 * Definition of the implementation of the machine learning algorithms used
 */
public interface Network {

    /**
     * Get the networks predictions on the input data
     *
     * @param inputs
     * @return
     */
    public INDArray predict(final INDArray inputs);

    /**
     * Fit the network to the data
     *
     * @param inputs
     * @param outputs
     */
    public void update(final INDArray inputs, final INDArray outputs);

    /**
     * Get the network model
     *
     * @return
     */
    public MultiLayerNetwork getNetwork();

    /**
     * Update the networks gradient
     *
     * @param inputs
     * @param labels
     * @return
     */
    public Gradient getGradient(final INDArray inputs, final INDArray labels);

    /**
     * Saves the network
     *
     * @param fileName
     */
    public void saveNetwork(final String fileName);

    /**
     * Loads the network
     *
     * @param fileName
     * @param moreTraining
     * @return
     */
    public MultiLayerNetwork loadNetwork(final File fileName, final boolean moreTraining);
}
