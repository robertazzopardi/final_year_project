package intelligence.Maddpg;

import org.nd4j.linalg.api.ndarray.INDArray;
import robots.Action;

/**
 * Past Experience N
 */
public class Experience {
    public final INDArray[] state;
    public final Action[] action;
    public final Float[] reward;
    public final INDArray[] nextState;

    public Experience(final INDArray[] state, final Action[] action, final Float[] reward, final INDArray[] nextState) {
        this.state = state;
        this.action = action;
        this.reward = reward;
        this.nextState = nextState;
    }
}
