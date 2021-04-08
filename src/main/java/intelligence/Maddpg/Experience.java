package intelligence.Maddpg;

import org.nd4j.linalg.api.ndarray.INDArray;
import robots.Action;

public class Experience {
    public final Boolean[][] state;
    public final Action[] action;
    public final Double[] reward;
    public final Boolean[][] nextState;
    public final Integer[] dones;

    public Experience(final Boolean[][] state, final Action[] action, final Double[] reward,
            final Boolean[][] nextState, final Integer[] dones) {
        this.state = state;
        this.action = action;
        this.reward = reward;
        this.nextState = nextState;
        this.dones = dones;
    }

    // public final INDArray state;
    // public final INDArray action;
    // public final INDArray reward;
    // public final INDArray nextState;
    // public final INDArray dones;

    // public Experience(final INDArray state, final INDArray action, final INDArray reward,
    // final INDArray nextState, final INDArray dones) {
    // this.state = state;
    // this.action = action;
    // this.reward = reward;
    // this.nextState = nextState;
    // this.dones = dones;
    // }
}
