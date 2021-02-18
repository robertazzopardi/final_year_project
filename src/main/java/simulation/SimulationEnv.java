package simulation;

import java.util.Arrays;
import java.util.logging.Logger;

import comp329robosim.EnvController;
import comp329robosim.MyGridCell;
import comp329robosim.OccupancyType;
import intelligence.DeepQLearning;
import robots.RobotController;

public class SimulationEnv extends EnvController {
    public static final String CONFIG_FILE = "./resources/defaultConfig.txt";

    public static final int HEIGHT = 10;

    public static final int WIDTH = 10;

    private final MyGridCell[][] grid;

    public SimulationEnv(final String confFileName, final int cols, final int rows) {
        super(confFileName, cols, rows);

        grid = getGrid();

        new RobotController(this);

        new DeepQLearning();
    }

    public static void main(final String[] args) {
        new SimulationEnv(CONFIG_FILE, WIDTH, HEIGHT);
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
