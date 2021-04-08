package intelligence;

import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import robots.Action;

/**
 * Definition of the implementation of the machine learning algorithms used
 */
public interface Inteligence {

    /**
     * Get the next Action from the model
     *
     * @param state
     * @return
     */
    Action getAction(final Boolean[] state);

    /**
     * Get the network model
     *
     * @return
     */
    MultiLayerNetwork getNetwork();

    /**
     * Fit the model to the current state, action and the next state
     *
     * @param state
     * @param action
     * @param score
     * @param newObservation
     */
    void update(final Boolean[] state, final Action action, final double score,
            final Boolean[] newObservation);

}
