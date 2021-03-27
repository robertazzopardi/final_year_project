import simulation.Mode;
import simulation.SimulationEnv;

public class Main {

    public static void main(final String[] args) {
        final SimulationEnv env =
                new SimulationEnv("", SimulationEnv.GRID_SIZE, SimulationEnv.GRID_SIZE, Mode.TRAIN);

        env.startController();

        // Keep thread alive so that the learning visualisation ui server keeps running
        while (Boolean.TRUE.equals(env.isRunning())) {
            try {
                Thread.sleep(1000);
            } catch (final InterruptedException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
            }
        }
    }
}
