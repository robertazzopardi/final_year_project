import simulation.Env;
import simulation.Mode;

/**
 * Program Start
 */
public class Main {

    private static final Mode simMode = Mode.TRAIN_ON;

    public static void main(final String[] args) {
        new Env("", Env.GRID_SIZE, Env.GRID_SIZE, simMode);
    }
}
