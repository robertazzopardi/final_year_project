package robotstest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import org.junit.jupiter.api.Test;

import robots.Direction;
import simulation.Env;

class DirectionTest {

    private static final Direction NONE = Direction.NONE;
    private static final Direction LEFT = Direction.LEFT;
    private static final Direction UP = Direction.UP;
    private static final Direction RIGHT = Direction.RIGHT;
    private static final Direction DOWN = Direction.DOWN;

    @Test
    void checkScaledDegree() {
        // correct
        assertEquals(DOWN, Direction.fromDegree(0));
        assertEquals(DOWN, Direction.fromDegree(360));
        assertEquals(DOWN, Direction.fromDegree(-360));

        assertEquals(RIGHT, Direction.fromDegree(90));
        assertEquals(RIGHT, Direction.fromDegree(-270));

        assertEquals(UP, Direction.fromDegree(180));
        assertEquals(UP, Direction.fromDegree(-180));

        assertEquals(LEFT, Direction.fromDegree(270));
        assertEquals(LEFT, Direction.fromDegree(-90));

        assertEquals(NONE, Direction.fromDegree(2));
        assertEquals(NONE, Direction.fromDegree(-50));
        assertEquals(NONE, Direction.fromDegree(2000));
        assertEquals(NONE, Direction.fromDegree(-999));

        // false
        assertNotEquals(DOWN, Direction.fromDegree(270));
        assertNotEquals(RIGHT, Direction.fromDegree(180));
        assertNotEquals(UP, Direction.fromDegree(90));
        assertNotEquals(LEFT, Direction.fromDegree(360));
    }

    // @Test
    // void checkNewGridXY() {
    // final int x = 10;
    // final int y = 5;

    // assertEquals(11, RIGHT.x(x));
    // assertEquals(5, RIGHT.y(y));

    // assertEquals(9, LEFT.x(x));
    // assertEquals(5, LEFT.y(y));

    // assertEquals(10, UP.x(x));
    // assertEquals(4, UP.y(y));

    // assertEquals(10, DOWN.x(x));
    // assertEquals(6, DOWN.y(y));
    // }

    @Test
    void checkNewActualXY() {
        final int x = Env.CELL_WIDTH;
        final int y = Env.CELL_WIDTH;

        assertEquals(x + x, RIGHT.px(x));
        assertEquals(y, RIGHT.py(y));

        assertEquals(x - x, LEFT.px(x));
        assertEquals(y, LEFT.py(y));

        assertEquals(x, UP.px(x));
        assertEquals(y - y, UP.py(y));

        assertEquals(x, DOWN.px(x));
        assertEquals(y + y, DOWN.py(y));
    }

}
