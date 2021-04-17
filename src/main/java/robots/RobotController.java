package robots;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import intelligence.Maddpg.Maddpg;
import simulation.Env;

/**
 * Main utility class to handle the agents
 */
public class RobotController {
	public static final int AGENT_COUNT = 4;
	public static final int OBSERVATION_COUNT = 10;
	private static final int DELAY = 1000;
	private static final int CAPACITY = 1000000;
	private static final int MAX_EPISODE = 500;
	private static final int MAX_STEP = 320;
	// private static final int BATCH_SIZE = 64;
	private static final int BATCH_SIZE = 32;
	private static final ExecutorService executor = Executors.newFixedThreadPool(AGENT_COUNT + 1);

	private final Hunter[] hunters = new Hunter[AGENT_COUNT];

	private Prey prey;

	private final Env env;

	public RobotController(final Env env) {
		this.env = env;

		new Maddpg(CAPACITY, hunters, this, MAX_EPISODE, MAX_STEP, BATCH_SIZE).run();
	}

	public Env getEnv() {
		return env;
	}

	public Hunter[] getHunters() {
		return hunters;
	}

	public Prey getPrey() {
		return prey;
	}

	/**
	 * Reset environment and get hunter observations
	 *
	 * @return
	 */
	public Float[][] reset() {
		initRobots();
		return Arrays.stream(hunters).map(Hunter::getObservation).toArray(Float[][]::new);
	}

	// public static <T> void invokeCallables(final List<T> callables) throws Exception {
	// executor.invokeAll((List<Callable<Void>>) callables);
	// }

	/**
	 * Step through the simulation environment and return the a new observation
	 *
	 * @param actions
	 * @return
	 */
	public StepObs step(final Action[] actions) {
		Float[] rewards = new Float[hunters.length];
		final Float[][] nextStates = new Float[hunters.length][];

		// Set next action
		for (int i = 0; i < hunters.length; i++) {
			hunters[i].setAction(actions[i]);
			// rewards[i] = hunters[i].getScoreForAction(actions[i]);
		}

		executeAction();

		// Collect the states after the agents have moved
		for (int i = 0; i < hunters.length; i++) {
			nextStates[i] = hunters[i].getObservation();
			// rewards[i] = hunters[i].isAtGoal() ? 1f : -1f;
			rewards[i] = getReward(hunters[i]);
		}

		final double sum = Arrays.stream(rewards).mapToDouble(a -> a).sum();
		Arrays.fill(rewards, (float) sum);

		// System.out.println(Arrays.toString(rewards));

		return new StepObs(nextStates, rewards, prey.isTrapped());
	}

	private void executeAction() {
		// Step each agent through the world
		try {
			final List<Agent> agents = new ArrayList<>(Arrays.asList(hunters));
			agents.add(prey);
			executor.invokeAll(agents);
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	private Float getReward(final Hunter hunter) {
		Float reward = 0f;

		if (hunter.isAtGoal()) {
			for (Hunter h : hunters) {
				if (h.isAtGoal()) {
					// reward += 0.125f;
					reward += 1f;
				}
			}
		}

		return reward;
	}

	// private void initRobots() {
	// // initialise the robots from the environment
	// final SimulatedRobot preyRobot = env.getAndSetPrey();
	// prey = new Prey(preyRobot, DELAY, env, this);

	// // final Mode mode = env.getMode();

	// for (int i = 0; i < hunters.length; i++) {
	// do {
	// final SimulatedRobot simulatedRobot = env.getAndSetHunter(i);
	// // Inteligence learning = null;
	// // switch (mode) {
	// // case EVAL:
	// // if (env.getFiles().length == 0) {
	// // learning = new DeepQLearning(true);
	// // } else {
	// // learning = DeepQLearning.loadNetwork(env.getFiles()[i], false, true);
	// // }
	// // break;

	// // case TRAIN_ON:
	// // if (env.getFiles().length < 4) {
	// // learning = new DeepQLearning(false);

	// // } else {
	// // learning = DeepQLearning.loadNetwork(env.getFiles()[i], true, false);

	// // }
	// // break;

	// // case TRAIN:
	// // learning = new DeepQLearning(false);
	// // break;

	// // default:
	// // break;
	// // }
	// // hunters[i] = new Hunter(simulatedRobot, DELAY, env, learning, this, prey, i);
	// hunters[i] = new Hunter(simulatedRobot, DELAY, env, null, this, prey, i);
	// } while (isSamePosition(i));
	// }

	// // for (final Hunter hunter : hunters) {
	// // hunter.setOthers(hunters);
	// // }
	// }

	private void initRobots() {
		// initialise the robots from the environment
		if (prey == null) {
			prey = new Prey(env.getAndSetPrey(), DELAY, env, this);
		} else {
			prey.setPose(prey.getSimulatedRobot().getRandomPos(),
					prey.getSimulatedRobot().getRandomPos(), 0);
		}

		for (int i = 0; i < hunters.length; i++) {
			do {
				if (hunters[i] == null) {
					hunters[i] =
							new Hunter(env.getAndSetHunter(i), DELAY, env, null, this, prey, i);
				} else {
					int randomPosX = hunters[i].getSimulatedRobot().getRandomPos();
					int randomPosY = hunters[i].getSimulatedRobot().getRandomPos();
					hunters[i].setPose(randomPosX, randomPosY, 0);
				}
			} while (isSamePosition(i));
		}
	}

	private boolean isSamePosition(final int i) {
		if (hunters[i].getX() == prey.getX() && hunters[i].getY() == prey.getY()) {
			return true;
		}

		for (int j = 0; j < hunters.length; j++) {
			try {
				if (j != i && hunters[i].getX() == hunters[j].getX()
						&& hunters[i].getY() == hunters[j].getY()) {
					return true;
				}
			} catch (NullPointerException npe) {
				//
			}
		}

		return false;
	}

	// private boolean isSamePosition(final int i) {
	// if (hunters[i].getX() == prey.getX() && hunters[i].getY() == prey.getY()) {
	// return true;
	// }

	// for (int j = 0; j < i; j++) {
	// if (hunters[i].getX() == hunters[j].getX() && hunters[i].getY() == hunters[j].getY()) {
	// return true;
	// }
	// }

	// return false;
	// }

	// public void restartRobots() {
	// // final SimulatedRobot preyRobot = env.getAndSetPrey();
	// // prey = new Prey(preyRobot, DELAY, env, this);

	// for (int i = 0; i < 4; i++) {
	// do {
	// final SimulatedRobot simulatedHunter = env.getAndSetHunter(i);
	// hunters[i] = new Hunter(simulatedHunter, DELAY, env, hunters[i].getLearning(), this,
	// prey, i);
	// } while (isSamePosition(i));
	// }

	// startRobots();
	// }

	// public void resumeHunters() {
	// for (final Hunter hunter : hunters) {
	// hunter.resumeRobot();
	// }
	// }

	// private void startRobots() {
	// // prey.start();
	// for (final Hunter hunter : hunters) {
	// hunter.start();
	// }
	// }

	// public void pauseRobots() {
	// for (final Hunter hunter : hunters) {
	// hunter.pauseRobot();
	// }
	// }

	// public void stopRobots() {
	// // prey.stopRobot();
	// for (final Hunter hunter : hunters) {
	// hunter.stopRobot();
	// }
	// }

	// public void saveNetworks() {
	// for (int i = 0; i < hunters.length; i++) {
	// if (env.getMode() == Mode.TRAIN) {
	// DeepQLearning.saveNetwork(hunters[i].getNetwork(), i,
	// Integer.toString(Env.TOTAL_EPISODES));
	// } else if (env.getMode() == Mode.TRAIN_ON) {
	// if (env.getFiles().length != 0) {
	// // final String fileName = env.getFiles()[i].getName();
	// try {
	// Files.delete(env.getFiles()[i].toPath());
	// } catch (final IOException e) {
	// e.printStackTrace();
	// }

	// DeepQLearning.saveNetwork(hunters[i].getNetwork(), i,
	// Integer.toString(env.getEpisode() - 1));

	// } else {
	// DeepQLearning.saveNetwork(hunters[i].getNetwork(), i,
	// Integer.toString(Env.TOTAL_EPISODES));
	// }
	// }
	// }
	// }

}
