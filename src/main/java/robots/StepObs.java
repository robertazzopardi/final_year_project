package robots;

public class StepObs {
    private final Float[][] nextStates;
    private final Float[] rewards;
    private final boolean done;

    public StepObs(final Float[][] nextStates, final Float[] rewards, final boolean done) {
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

    public Float[][] getNextStates() {
        return nextStates;
    }
}
