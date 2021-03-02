import simulation.Mode;
import simulation.SimulationEnv;

public class Main {

    public static void main(final String[] args) {
        new SimulationEnv("", SimulationEnv.GRID_SIZE, SimulationEnv.GRID_SIZE, Mode.TRAIN_ON);
    }
}
