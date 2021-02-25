package simulation;

import java.util.Arrays;
import java.util.logging.Logger;

import comp329robosim.MyGridCell;

public class GridPrinter extends Thread {
    private static final Logger LOGGER = Logger.getLogger(GridPrinter.class.getName());

    private final MyGridCell[][] grid;

    public GridPrinter(final MyGridCell[][] grid) {
        this.grid = grid;
    }

    @Override
    public void run() {
        while (true) {
            printGrid(LOGGER);

            try {
                Thread.sleep(2000);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public void printGrid(final Logger inLogger) {
        for (final MyGridCell[] myGridCells : grid) {
            final String row = Arrays.toString(myGridCells);
            // inLogger.info(row);
            System.out.println(row);
        }
        inLogger.info("");
    }

}
