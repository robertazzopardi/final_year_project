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

    public static final int SIZE = 10;

    private final MyGridCell[][] grid;

    public SimulationEnv(final String confFileName, final int cols, final int rows) {
        super(confFileName, cols, rows);

        grid = getGrid();

        for (int i = 0; i < grid.length; i++) {
            for (int j = 0; j < grid.length; j++) {
                if (i == 0 || j == 0 || i == SIZE - 1 || j == SIZE - 1) {
                    grid[j][i].setCellType(OccupancyType.OBSTACLE);
                }
            }
        }

        new RobotController(this, Intelligence.QLEAR_STRING);

        // new GridPrinter(this).start();
    }

    public static void main(final String[] args) {
        new SimulationEnv(CONFIG_FILE, SIZE, SIZE);
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
