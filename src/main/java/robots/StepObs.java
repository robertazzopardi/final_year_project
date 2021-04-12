package robots;

public class StepObs {
    public final Float[][] nextStates;
    public final Float[] rewards;
    public final boolean done;

    public StepObs(final Float[][] nextStates, final Float[] rewards, final boolean done) {
        this.nextStates = nextStates;
        this.rewards = rewards;
        this.done = done;
    }
}
