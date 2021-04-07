package robots;

public class StepObs {
    public final Boolean[][] nextStates;
    public final Double[] rewards;

    public StepObs(final Boolean[][] nextStates, final Double[] rewards) {
        this.nextStates = nextStates;
        this.rewards = rewards;
    }
}
