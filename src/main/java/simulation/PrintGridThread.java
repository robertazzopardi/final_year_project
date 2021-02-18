package simulation;

import java.util.logging.Logger;

public class PrintGridThread extends Thread {
    private static final Logger LOGGER = Logger.getLogger(PrintGridThread.class.getName());

    private final SimulationEnv env;

    private volatile boolean running = true;

    public PrintGridThread(final SimulationEnv env) {
        this.env = env;
    }

    @Override
    public void run() {
        while (running) {
            env.printGrid(LOGGER);

            try {
                Thread.sleep(2000);
            } catch (final InterruptedException e) {
                if (!running) {
                    break;
                }
                Thread.currentThread().interrupt();
            }
        }
    }

    public void stopThread() {
        running = false;
        interrupt();
    }

}
