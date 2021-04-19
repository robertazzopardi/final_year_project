package robots;

import java.io.File;
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
	private static final int DELAY = 1000;
	private static final int CAPACITY = 1000000;
	private static final int MAX_EPISODE = 500;
	private static final int MAX_STEP = 100 * Env.GRID_SIZE;
	// private static final int BATCH_SIZE = 32;
	private static final int BATCH_SIZE = 16;
	private static final ExecutorService executor = Executors.newFixedThreadPool(AGENT_COUNT + 1);

	public static final String OUTPUT_FOLDER = "src/main/resources/";
	private final File[] files =
			new File(OUTPUT_FOLDER).listFiles((dir1, filename) -> filename.endsWith(".zip"));

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

	/**
	 * Step through the simulation environment and return the a new observation
	 *
	 * @param actions
	 * @return
	 */
	public StepObs step(final Action[] actions, final int step) {
		final Float[] rewards = new Float[hunters.length];
		Arrays.fill(rewards, 0f);
		final Double[] oldDistances = new Double[hunters.length];
		final Float[][] nextStates = new Float[hunters.length][];

		// Set next action
		for (int i = 0; i < hunters.length; i++) {
			hunters[i].setAction(actions[i]);
			// oldDistances[i] = hunters[i].getDistanceFrom();
		}

		executeAction();

		// Collect the states after the agents have moved
		for (int i = 0; i < hunters.length; i++) {
			nextStates[i] = hunters[i].getObservation();
			// rewards[i] += getReward(hunters[i], oldDistances[i]);
		}

		final long count = Arrays.stream(hunters).filter(Hunter::isAtGoal).count();
		final int correctedStep = step == 0 ? 1 : step;
		Arrays.fill(rewards, prey.isTrapped() ? (MAX_STEP / (float) (Math.pow(correctedStep, 2)))
				// + count
				:
				// -((float) step / MAX_STEP));
				0);

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

	private Float getReward(final Hunter hunter, final Double oldDistance) {
		Float reward = 0f;

		// if (hunter.isAtGoal()) {
		// for (final Hunter h : hunters) {
		// if (h.isAtGoal()) {
		// // reward += 0.125f;
		// reward += 1f;
		// } else {
		// reward -= 1f;
		// }
		// }
		// }

		if (hunter.getDistanceFrom() < oldDistance) {
			reward += 1;
		}

		return reward;
	}

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
					hunters[i] = new Hunter(env.getAndSetHunter(i), DELAY, env, null, this, prey,
							files[i]);
				} else {
					final int randomPosX = hunters[i].getSimulatedRobot().getRandomPos();
					final int randomPosY = hunters[i].getSimulatedRobot().getRandomPos();
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
			} catch (final NullPointerException npe) {
				//
			}
		}

		return false;
	}

}
