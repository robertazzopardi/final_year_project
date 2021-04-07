import simulation.Mode;
import simulation.Env;

public class Main {

    public static void main(final String[] args) {
        final Env env = new Env("", Env.GRID_SIZE, Env.GRID_SIZE, Mode.EVAL);

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
