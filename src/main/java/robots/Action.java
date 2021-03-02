package robots;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

public enum Action {
    LEFT_TURN, RIGHT_TURN, TRAVEL, NOTHING;

    private static final Action[] ACTIONS = values();

    public static final int LENGTH = ACTIONS.length;

    private static final Action[] TURN_ACTIONS = new Action[] { LEFT_TURN, RIGHT_TURN, NOTHING };

    public static Action getRandomAction() {
        return ACTIONS[ThreadLocalRandom.current().nextInt(LENGTH)];
    }

    public static Action getActionByIndex(final int index) {
        return ACTIONS[index];
    }

    public static Action getRandomTurn() {
        return TURN_ACTIONS[ThreadLocalRandom.current().nextInt(TURN_ACTIONS.length)];
    }

    public int getActionIndex() {
        return Arrays.asList(ACTIONS).indexOf(this);
    }
}
