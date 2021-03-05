package simulation;

import java.io.File;

import comp329robosim.EnvController;
import comp329robosim.MyGridCell;
import comp329robosim.OccupancyType;

import robots.RobotController;

public class SimulationEnv extends EnvController {
    // public static final String CONFIG_FILE = "resources/defaultConfig.txt";

    private Boolean isRunning = true;

    public Boolean isRunning() {
        return isRunning;
    }

    public void stopRunning() {
        isRunning = false;
    }

    public static final String OUTPUT_FOLDER = "./resources/";

    public static final int EPISODES = 50;

    private final Mode mode;

    public static final int GRID_SIZE = 6;

    private int episode = 1;

    private final int trainedEpisodes;

    public int getTrainedEpisodes() {
        return trainedEpisodes;
    }

    private MyGridCell[][] grid;

    private final File[] files = new File(OUTPUT_FOLDER).listFiles((dir1, filename) -> filename.endsWith(".zip"));

    public File[] getFiles() {
        return files;
    }

    public SimulationEnv(final String confFileName, final int cols, final int rows, final Mode mode) {
        super(confFileName, cols, rows);

        this.mode = mode;

        if (files.length == 0 || mode == Mode.TRAIN) {
            updateTitle(episode);
            this.trainedEpisodes = 0;
        } else {
            final String fileName = files[0].getName();
            episode = Integer.parseInt(fileName.substring(fileName.lastIndexOf('_') + 1, fileName.indexOf(".zip")));
            updateTitle(episode);
            this.trainedEpisodes = episode;
        }

        grid = getGrid();

        addBoundaries();

        new RobotController(this);

        // new GridPrinter(grid).start();

    }

    public Mode getMode() {
        return mode;
    }

    public int getEpisode() {
        return episode + 1;
    }

    public int incrementEpisode() {
        return ++episode;
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
