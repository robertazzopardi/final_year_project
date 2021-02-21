package simulation;

import java.util.Arrays;
import java.util.logging.Logger;

import comp329robosim.EnvController;
import comp329robosim.MyGridCell;
import comp329robosim.OccupancyType;
import intelligence.Intelligence;
import robots.RobotController;

public class SimulationEnv extends EnvController {
    public static final String CONFIG_FILE = "resources/defaultConfig.txt";

    public static final int GRID_SIZE = 5;

    private final MyGridCell[][] grid;

    public SimulationEnv(final String confFileName, final int cols, final int rows) {
        super(confFileName, cols, rows);

        grid = getGrid();

        addBoundaries();

        // new RobotController(this, Intelligence.QLEAR_STRING);
        new RobotController(this, Intelligence.DQN_STRING);

        // new GridPrinter(this).start();
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

    public static void main(final String[] args) {
        new SimulationEnv(CONFIG_FILE, GRID_SIZE, GRID_SIZE);
    }

    public void printGrid(final Logger inLogger) {
        for (final MyGridCell[] myGridCells : grid) {
            final String row = Arrays.toString(myGridCells);
            inLogger.info(row);
        }
        inLogger.info("");
    }

    public void updateGridEmpty(final int x, final int y) {
        synchronized (grid[y][x]) {
            grid[y][x].setEmpty();
        }
    }

    public void updateGridGoal(final int x, final int y) {
        synchronized (grid[y][x]) {
            grid[y][x].setCellType(OccupancyType.GOAL);
        }
    }

    public void updateGridHunter(final int x, final int y) {
        synchronized (grid[y][x]) {
            grid[y][x].setCellType(OccupancyType.HUNTER);
        }
    }

    public void updateGridPrey(final int x, final int y) {
        synchronized (grid[y][x]) {
            grid[y][x].setCellType(OccupancyType.PREY);
        }
    }
}
