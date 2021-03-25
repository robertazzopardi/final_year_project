package robots;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

public enum Action {
    // LEFT, RIGHT, FORWARD, NOTHING;
    UP, DOWN, LEFT, RIGHT, NOTHING;

    private static final Action[] ACTIONS = values();

    public static final int LENGTH = ACTIONS.length;

    // private static final Action[] TURN_ACTIONS = new Action[] {LEFT, RIGHT, NOTHING};

    public static Action getRandomAction() {
        return ACTIONS[ThreadLocalRandom.current().nextInt(LENGTH)];
    }

    public static Action getActionByIndex(final int index) {
        return ACTIONS[index];
    }

    // public static Action getRandomTurn() {
    // return TURN_ACTIONS[ThreadLocalRandom.current().nextInt(TURN_ACTIONS.length)];
    // }

    public int getActionIndex() {
        return Arrays.asList(ACTIONS).indexOf(this);
    }
}
