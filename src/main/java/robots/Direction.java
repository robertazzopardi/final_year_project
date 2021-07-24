package robots;

import environment.Env;

/**
 * Defines a possible direction
 */
public enum Direction {
    UP, DOWN, LEFT, RIGHT, NONE;

    /**
     * Scale the degree to -270 - 270 With 0 taking itself and 360 degrees And returns a direction
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
     * Get new x position based on actual environment dimensions
     *
     * @param x
     * @return
     */
    public int px(final int x) {
        switch (this) {
            case UP:
            case DOWN:
                return x;
            case LEFT:
                return x - Env.CELL_WIDTH;
            case RIGHT:
                return x + Env.CELL_WIDTH;
            default:
                return 0;
        }
    }

    /**
     * Get new y position based on actual environment dimensions
     *
     * @param y
     * @return
     */
    public int py(final int y) {
        switch (this) {
            case UP:
                return y - Env.CELL_WIDTH;
            case DOWN:
                return y + Env.CELL_WIDTH;
            case LEFT:
            case RIGHT:
                return y;
            default:
                return 0;
        }
    }
}
