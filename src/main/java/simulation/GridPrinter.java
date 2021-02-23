package simulation;

import java.util.logging.Logger;

public class GridPrinter extends Thread {
    private static final Logger LOGGER = Logger.getLogger(GridPrinter.class.getName());

    private final SimulationEnv env;

    public GridPrinter(final SimulationEnv env) {
        this.env = env;
    }

    @Override
    public void run() {
        while (true) {
            env.printGrid(LOGGER);

            try {
                Thread.sleep(2000);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

}
