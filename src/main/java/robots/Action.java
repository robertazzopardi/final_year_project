package robots;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

public enum Action {
    RIGHT, DOWN, LEFT, UP, NOTHING;

    private static final Action[] ACTIONS = values();

    public static final int LENGTH = ACTIONS.length;

    public static Action getRandomAction() {
        return ACTIONS[ThreadLocalRandom.current().nextInt(LENGTH)];
    }

    public static Action getActionByIndex(final int index) {
        return ACTIONS[index];
    }

    public int getActionIndex() {
        return Arrays.asList(ACTIONS).indexOf(this);
    }
}
