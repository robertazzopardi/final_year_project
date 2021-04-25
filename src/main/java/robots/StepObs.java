package robots;

import org.nd4j.linalg.api.ndarray.INDArray;

public class StepObs {
    private final INDArray[] nextStates;
    private final Float[] rewards;
    private final boolean done;

    public StepObs(final INDArray[] nextStates, final Float[] rewards, final boolean done) {
        this.nextStates = nextStates;
        this.rewards = rewards;
        this.done = done;
    }

    public boolean isDone() {
        return done;
    }

    public Float[] getRewards() {
        return rewards;
    }

    public INDArray[] getNextStates() {
        return nextStates;
    }
}
