package simulation;

import comp329robosim.EnvController;
import comp329robosim.MyGridCell;
import comp329robosim.OccupancyType;
import robots.RobotController;

public class Env extends EnvController {
    public static final int TOTAL_EPISODES = 100;
    public static final int CELL_WIDTH = 350;
    public static final int CELL_RADIUS = CELL_WIDTH / 2;
    public static final int GRID_SIZE = 10;
    public static final int ENV_SIZE = Env.GRID_SIZE * Env.CELL_WIDTH;

    private final Mode mode;

    private final MyGridCell[][] grid;

    public Env(final String confFileName, final int cols, final int rows, final Mode mode) {
        super(confFileName, cols, rows);

        this.mode = mode;

        grid = getGrid();

        addBoundaries();
    }

    /**
     * Initialise the robot controller
     */
    public void startController() {
        new RobotController(this);
    }

    /**
     * Get the game mode
     *
     * @return
     */
    public Mode getMode() {
        return mode;
    }

    private void addBoundaries() {
        for (int x = 0; x < grid.length; x++) {
            for (int y = 0; y < grid.length; y++) {
                if (x == 0 || y == 0 || x == GRID_SIZE - 1 || y == GRID_SIZE - 1) {
                    grid[x][y].setCellType(OccupancyType.OBSTACLE);
                }
            }
        }
    }

    /**
     * Set all agent cells to empty
     */
    public void resetGridToEmpty() {
        for (final MyGridCell[] myGridCells : grid) {
            for (final MyGridCell myGridCell : myGridCells) {
                if (myGridCell.getCellType() == OccupancyType.HUNTER
                        || myGridCell.getCellType() == OccupancyType.PREY) {
                    myGridCell.setEmpty();
                }
            }
        }
    }
}
