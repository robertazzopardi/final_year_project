package robotstest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import robots.Action;

class EnvTest {

    @Test
    void checkActionAtIndex() {
        // assertEquals(left, Action.getActionByIndex(0));
        // assertEquals(right, Action.getActionByIndex(1));
        // assertEquals(forward, Action.getActionByIndex(2));
        // assertEquals(nothing, Action.getActionByIndex(3));

        int i = 0;
        for (Action action : Action.values()) {
            assertEquals(action, Action.getActionByIndex(i++));
        }
    }

    @Test
    void checkActionIsIndex() {
        // assertEquals(0, left.getActionIndex());
        // assertEquals(1, right.getActionIndex());
        // assertEquals(2, forward.getActionIndex());
        // assertEquals(3, nothing.getActionIndex());

        int i = 0;
        for (Action action : Action.values()) {
            assertEquals(i++, action.getActionIndex());
        }
    }

    @Test
    void testRandomAction() {
        List<Action> actions = Arrays.asList(Action.values());
        for (int i = 0; i < 10; i++) {
            assertTrue(actions.contains(Action.getRandomAction()));
        }
    }
}
