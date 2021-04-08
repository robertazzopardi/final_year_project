package robots;

public class StepObs {
    public final Boolean[][] nextStates;
    public final Double[] rewards;
    public final Boolean[] dones;

    public StepObs(final Boolean[][] nextStates, final Double[] rewards, final Boolean[] dones) {
        this.nextStates = nextStates;
        this.rewards = rewards;
        this.dones = dones;
    }
}
