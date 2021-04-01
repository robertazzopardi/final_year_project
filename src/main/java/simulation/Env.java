package simulation;

import java.io.File;

import comp329robosim.EnvController;
import comp329robosim.MyGridCell;
import comp329robosim.OccupancyType;

import robots.RobotController;

public class Env extends EnvController {
    // public static final String CONFIG_FILE = "resources/defaultConfig.txt";

    public static final int TOTAL_EPISODES = 100;
    private int episode = 1;

    public static final int CELL_WIDTH = 350;
    public static final int CELL_RADIUS = CELL_WIDTH / 2;
    public static final int GRID_SIZE = 6;
    public static final int ENV_SIZE = Env.GRID_SIZE * Env.CELL_WIDTH;
    public static final String OUTPUT_FOLDER = "src/main/resources/";

    private Boolean isRunning = true;

    private final Mode mode;

    private final int trainedEpisodes;

    private final MyGridCell[][] grid;

    private final File[] files =
            new File(OUTPUT_FOLDER).listFiles((dir1, filename) -> filename.endsWith(".zip"));

    public Env(final String confFileName, final int cols, final int rows, final Mode mode) {
        super(confFileName, cols, rows);

        this.mode = mode;

        if (files.length == 0 || mode == Mode.TRAIN) {
            updateTitle(String.valueOf(episode));
            this.trainedEpisodes = 0;
        } else {
            final String fileName = files[0].getName();
            episode = Integer.parseInt(
                    fileName.substring(fileName.lastIndexOf('_') + 1, fileName.indexOf(".zip")));
            updateTitle(String.valueOf(episode));
            this.trainedEpisodes = episode;
        }

        grid = getGrid();

        addBoundaries();

        // new GridPrinter<MyGridCell>(grid).start();
    }

    public Boolean isRunning() {
        return isRunning;
    }

    public void stopRunning() {
        isRunning = false;
    }

    public int getTrainedEpisodes() {
        return trainedEpisodes;
    }

    public File[] getFiles() {
        return files;
    }

    public void startController() {
        new RobotController(this);
    }

    public Mode getMode() {
        return mode;
    }

    public int getEpisode() {
        return episode;
    }

    public int incrementEpisode() {
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

    // public void updateGridEmpty(final int x, final int y) {
    // synchronized (grid[y][x]) {
    // grid[y][x].setEmpty();
    // }
    // }

    // public void updateGrid(final int x, final int y, final OccupancyType occupancyType) {
    // synchronized (grid[y][x]) {
    // grid[y][x].setCellType(occupancyType);
    // }
    // }

    public void resetGrid() {
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
