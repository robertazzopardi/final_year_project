package robots;

public class StepObs {
    private final Boolean[][] nextStates;
    private final Float[] rewards;
    private final boolean done;

    public StepObs(final Boolean[][] nextStates, final Float[] rewards, final boolean done) {
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

    public Boolean[][] getNextStates() {
        return nextStates;
    }
}
