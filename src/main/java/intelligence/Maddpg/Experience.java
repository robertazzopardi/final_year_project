package intelligence.Maddpg;

import java.io.Serializable;
import java.util.Arrays;

import org.nd4j.linalg.api.ndarray.INDArray;
import robots.Action;

/**
 * Past Experience N
 */
public class Experience implements Serializable {
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(action);
        result = prime * result + Arrays.hashCode(nextState);
        result = prime * result + Arrays.hashCode(reward);
        result = prime * result + Arrays.hashCode(state);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Experience other = (Experience) obj;
        if (!Arrays.equals(action, other.action))
            return false;
        if (!Arrays.equals(nextState, other.nextState))
            return false;
        if (!Arrays.equals(reward, other.reward))
            return false;
        if (!Arrays.equals(state, other.state))
            return false;
        return true;
    }
}
