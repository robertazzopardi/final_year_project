package robotstest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import robots.Action;

class ActionTest {

    @Test
    void checkActionAtIndex() {
        int i = 0;
        for (final Action action : Action.values()) {
            assertEquals(action, Action.getActionByIndex(i++));
        }
    }

    @Test
    void checkActionIsIndex() {
        int i = 0;
        for (final Action action : Action.values()) {
            assertEquals(i++, action.getActionIndex());
        }
    }

    @Test
    void testRandomAction() {
        final List<Action> actions = Arrays.asList(Action.values());
        for (int i = 0; i < 10; i++) {
            assertTrue(actions.contains(Action.getRandomAction()));
        }
    }
}
