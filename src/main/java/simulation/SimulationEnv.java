package simulation;

import comp329robosim.EnvController;
import comp329robosim.MyGridCell;
import comp329robosim.OccupancyType;

import robots.RobotController;

public class SimulationEnv extends EnvController {
    // public static final String CONFIG_FILE = "resources/defaultConfig.txt";

    public enum Mode {
        TRAIN, EVAL
    }

    public static final Mode MODE = Mode.TRAIN;
    // public static final Mode MODE = Mode.EVAL;

    public static final int GRID_SIZE = 6;

    private int episode = 1;

    private MyGridCell[][] grid;

    public SimulationEnv(final String confFileName, final int cols, final int rows) {
        super(confFileName, cols, rows);

        updateTitle(getEpisode());

        grid = getGrid();

        addBoundaries();

        new RobotController(this);

        // new GridPrinter(grid).start();

    }

    public int getEpisode() {
        return episode++;
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
        new SimulationEnv("", GRID_SIZE, GRID_SIZE);
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

    public void resetGrid() {
        for (MyGridCell[] myGridCells : grid) {
            for (MyGridCell myGridCell : myGridCells) {
                if (myGridCell.getCellType() == OccupancyType.HUNTER
                        || myGridCell.getCellType() == OccupancyType.PREY) {
                    myGridCell.setEmpty();
                }
            }
        }
    }
}
