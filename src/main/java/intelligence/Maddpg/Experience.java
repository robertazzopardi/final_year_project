package intelligence.Maddpg;

import robots.Action;

/**
 * Past Experience N
 */
public class Experience {
    public final Float[][] state;
    public final Action[] action;
    public final Float[] reward;
    public final Float[][] nextState;
    public final Integer[] dones;

    public Experience(final Float[][] state, final Action[] action, final Float[] reward,
            final Float[][] nextState, final Integer[] dones) {
        this.state = state;
        this.action = action;
        this.reward = reward;
        this.nextState = nextState;
        this.dones = dones;
    }
}
