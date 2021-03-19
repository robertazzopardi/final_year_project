package robots;

import simulation.SimulationEnv;

/**
 * Defines a possible direction
 */
public enum Direction {
    UP, DOWN, LEFT, RIGHT, NONE;

    /**
     * Scale the degree to -270 - 270 With 0 taking itself and 360 degrees And
     * returns a direction
     *
     * @param degree
     * @return
     */
    public static Direction fromDegree(final int degree) {
        switch (degree % 360) {
        case 0:
            return DOWN;
        case 90:
        case -270:
            return RIGHT;
        case 180:
        case -180:
            return UP;
        case 270:
        case -90:
            return LEFT;
        default:
            return NONE;
        }
    }

    /**
     * Get new x position
     *
     * @param x
     * @return
     */
    public int x(final int x) {
        switch (this) {
        case UP:
            return x;
        case DOWN:
            return x;
        case LEFT:
            return x - 1;
        case RIGHT:
            return x + 1;
        default:
            return 0;
        }
    }

    /**
     * Get new y position
     *
     * @param y
     * @return
     */
    public int y(final int y) {
        switch (this) {
        case UP:
            return y - 1;
        case DOWN:
            return y + 1;
        case LEFT:
            return y;
        case RIGHT:
            return y;
        default:
            return 0;
        }
    }

    /**
     * Get new x position
     *
     * @param x
     * @return
     */
    public int px(final int x) {
        switch (this) {
        case UP:
            return x;
        case DOWN:
            return x;
        case LEFT:
            return x - SimulationEnv.CELL_DISTANCE;
        case RIGHT:
            return x + SimulationEnv.CELL_DISTANCE;
        default:
            return 0;
        }
    }

    /**
     * Get new y position
     *
     * @param y
     * @return
     */
    public int py(final int y) {
        switch (this) {
        case UP:
            return y - SimulationEnv.CELL_DISTANCE;
        case DOWN:
            return y + SimulationEnv.CELL_DISTANCE;
        case LEFT:
            return y;
        case RIGHT:
            return y;
        default:
            return 0;
        }
    }
}
