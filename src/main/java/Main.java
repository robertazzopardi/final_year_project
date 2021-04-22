import simulation.Env;
import simulation.Mode;

/**
 * Program Start
 */
public class Main {

    private static Mode simMode = Mode.TRAIN;

    public static void main(final String[] args) {
        final Env env = new Env("", Env.GRID_SIZE, Env.GRID_SIZE, simMode);

        env.startController();

        // Keep thread alive so that the dl4j network visualisation ui server keeps running
        // if (simMode == Mode.TRAIN || simMode == Mode.TRAIN_ON)
        // while (env.isRunning()) {
        // try {
        // Thread.sleep(1000);
        // } catch (final InterruptedException e) {
        // e.printStackTrace();
        // Thread.currentThread().interrupt();
        // }
        // }

    }

}
