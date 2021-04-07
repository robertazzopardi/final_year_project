package intelligence.Maddpg;

import robots.Action;

public class Experience {
    public final Boolean[][] state;
    public final Action[] action;
    public final Double[] reward;
    public final Boolean[][] nextState;

    public Experience(final Boolean[][] state, final Action[] action, final Double[] reward,
            final Boolean[][] nextState) {
        this.state = state;
        this.action = action;
        this.reward = reward;
        this.nextState = nextState;
    }
}
