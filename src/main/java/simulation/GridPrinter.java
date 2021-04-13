package simulation;

import java.util.Arrays;
import java.util.logging.Logger;

public class GridPrinter<T> implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(GridPrinter.class.getName());

    private final T[][] grid;

    public GridPrinter(final T[][] grid) {
        this.grid = grid;
    }

    @Override
    public void run() {
        while (true) {
            printGrid(grid);

            try {
                Thread.sleep(2000);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public static <T> void printGrid(final T[][] grid) {
        for (final T[] myGridCells : grid) {
            final String row = Arrays.toString(myGridCells);
            // inLogger.info(row);
            System.out.println(row);
        }
        LOGGER.info("");
    }

}
