package intelligence;

import simulation.SimulationEnv;

public abstract class Intelligence {

    public abstract int predict(final int state);

    public abstract void train();

    public static final String DQN_STRING = "DeepQLearning";
    public static final String QLEAR_STRING = "QLearning";

    public static Intelligence getIntelligence(String method, SimulationEnv env) throws NoAIException {
        Intelligence learning;
        switch (method) {
            case DQN_STRING:
                learning = new DeepQLearning();
                break;
            case QLEAR_STRING:
                learning = new QLearning(env.getGrid());
                break;
            default:
                throw new NoAIException();
        }
        return learning;
    }
}
