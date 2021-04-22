package intelligence.Maddpg;

import robots.Action;

/**
 * Past Experience N
 */
public class Experience {
    public final Boolean[][] state;
    public final Action[] action;
    public final Float[] reward;
    public final Boolean[][] nextState;
    public final Integer[] dones;

    public Experience(final Boolean[][] state, final Action[] action, final Float[] reward,
            final Boolean[][] nextState, final Integer[] dones) {
        this.state = state;
        this.action = action;
        this.reward = reward;
        this.nextState = nextState;
        this.dones = dones;
    }
}
