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
    }
}
