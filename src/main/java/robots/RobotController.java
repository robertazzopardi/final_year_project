package robots;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.nd4j.linalg.api.ndarray.INDArray;
import intelligence.Maddpg.Maddpg;
import simulation.Env;

/**
 * Main utility class to handle the agents
 */
public class RobotController {
	public static final int AGENT_COUNT = 5;
	private static final int DELAY = 1000;
	private static final int CAPACITY = 1000000;// Should calculate actual capacity
	public static final int BATCH_SIZE = 64;
	private static final int MAX_EPISODE = 1000;
	private static final int MAX_STEP = 100 * Env.GRID_SIZE;
	private static final ExecutorService executor = Executors.newFixedThreadPool(AGENT_COUNT + 1);

	public static final String OUTPUT_FOLDER = "src/main/resources/";
	private final File[] files =
			new File(OUTPUT_FOLDER).listFiles((dir1, filename) -> filename.endsWith(".zip"));

	private final Env env;

	private List<Agent> agents;

	public RobotController(final Env env) {
		this.env = env;

		agents = new ArrayList<>();

		new Maddpg(CAPACITY, agents, this, MAX_EPISODE, MAX_STEP, BATCH_SIZE).run();
	}

	public Env getEnv() {
		return env;
	}

	public List<Agent> getAgents() {
		return agents;
	}

	/**
	 * Reset environment and get hunter observations
	 *
	 * @return
	 */
	public INDArray[] reset() {
		initRobots();
		return agents.subList(0, 4).stream().map(Agent::getObservation).toArray(INDArray[]::new);
	}

	/**
	 * Step through the simulation environment and return the a new observation
	 *
	 * @param actions
	 * @return
	 */
	public StepObs step(final Action[] actions, final int step) {
		Float[] rewards = new Float[4];
		// Arrays.fill(rewards, 0f);
		Arrays.fill(rewards, -1f);

		final Double[] oldDistances = new Double[4];
		// final Boolean[][] nextStates = new Boolean[4][];
		final INDArray[] nextStates = new INDArray[4];

		// Set next action
		for (int i = 0; i < 4; i++) {
			agents.get(i).setAction(actions[i]);
			oldDistances[i] = ((Hunter) agents.get(i)).getDistanceFrom();
		}
		agents.get(4).setAction(Action.getRandomAction());

		executeAction();

		// final long count = Arrays.stream(hunters).filter(Hunter::isAtGoal).count();

		// Collect the states after the agents have moved
		for (int i = 0; i < 4; i++) {
			nextStates[i] = agents.get(i).getObservation();

			// rewards[i] = getReward(hunters[i], oldDistances[i], actions[i], i, rewards[i]);
			// rewards[i] = ((Hunter) agents.get(i)).getDistanceFrom() > oldDistances[i] ? -100f :
			// 0f;
		}

		boolean trapped = ((Prey) agents.get(4)).isTrapped();
		if (trapped) {
			Arrays.fill(rewards, 0f);
		}
		return new StepObs(nextStates, rewards, trapped);
	}

	private Float getReward(final Hunter hunter, final Double dxdy, final Action action,
			final int i, Float reward) {

		// if (hunter.isAtGoal()) {
		// for (final Hunter h : hunters) {
		// if (h.isAtGoal()) {
		// // reward += 0.125f;
		// reward += 1f;
		// } else {
		// // reward -= 1f;
		// }
		// }
		// }

		// if (!hunter.isAtGoal()) {
		// reward -= 1f;
		// }

		switch (action) {
			case FORWARD:
				if (hunter.isAtGoal()) {
					reward = 1f;
				} else {
					reward = -1f;
				}
				break;

			case LEFT:
			case RIGHT:
				// if (canSeePrey(i)) {
				// reward = 1f;
				// } else {
				// reward = -1f;
				// }
				break;

			case NOTHING:
				if (hunter.isAtGoal()) {
					reward = 1f;
				} else {
					reward = -1f;
				}
				break;

			default:
				break;
		}

		return reward;
	}

	private void executeAction() {
		// Step each agent through the world
		try {
			executor.invokeAll(agents);
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	private void initRobots() {
		// initialise the robots from the environment
		for (int i = 0; i < 4; i++) {
			do {
				if (i < 4) {
					if (agents.size() < 4) {
						agents.add(new Hunter(env.getAndSetHunter(i), DELAY, env, this,
								files.length > 0 ? files[i] : null));
					} else {
						final int randomPosX = agents.get(i).getSimulatedRobot().getRandomPos();
						final int randomPosY = agents.get(i).getSimulatedRobot().getRandomPos();
						agents.get(i).setPose(randomPosX, randomPosY, 0);
					}
				}
			} while (samePosition(agents.get(i)));
		}

		if (agents.size() < 5) {
			Prey prey = null;
			do {
				prey = new Prey(env.getAndSetPrey(), DELAY, env, this, null);
			} while (samePosition(prey));
			agents.add(prey);
		} else {
			agents.get(4).setPose(agents.get(4).getSimulatedRobot().getRandomPos(),
					agents.get(4).getSimulatedRobot().getRandomPos(), 0);
		}
	}

	private boolean samePosition(final Agent agent) {
		return agents.stream()
				.anyMatch(i -> i != agent && agent.getX() == i.getX() && agent.getY() == i.getY());
	}

}
