package robots;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Represents a possible action the robots can make
 */
public enum Action {
    LEFT, RIGHT, FORWARD, NOTHING;

    private static final Action[] ACTIONS = values();

    public static final int LENGTH = ACTIONS.length;

    /**
     * Get a random action
     *
     * @return
     */
    public static Action getRandomAction() {
        return ACTIONS[ThreadLocalRandom.current().nextInt(LENGTH)];
    }

    /**
     * Get the action associated with the provided index
     *
     * @param index
     * @return
     */
    public static Action getActionByIndex(final int index) {
        return ACTIONS[index];
    }

    /**
     * Get the index of the current instance
     *
     * @return
     */
    public int getActionIndex() {
        return Arrays.asList(ACTIONS).indexOf(this);
    }
}
