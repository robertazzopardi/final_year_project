package robots;

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
        final int scaled = degree % 360;

        switch (scaled) {
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
            break;
        }
        return null;
    }
}
