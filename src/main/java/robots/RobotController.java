package robots;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.nd4j.linalg.api.ndarray.INDArray;
import intelligence.Maddpg.Maddpg;
import simulation.Env;
import simulation.Mode;

/**
 * Main utility class to handle the agents
 */
public class RobotController {
	public static final int AGENT_COUNT = 5;
	private static final int DELAY = 1000;
	private static final int CAPACITY = 1000000;// Should calculate actual capacity
	public static final int BATCH_SIZE = 64;
	private static final int MAX_EPISODE = 1001;
	public static final int MAX_STEP = 100 * Env.GRID_SIZE;
	private static final ExecutorService executor = Executors.newFixedThreadPool(AGENT_COUNT + 1);

	public static final String OUTPUT_FOLDER = "src/main/resources/";
	private final File[] files;

	private final Env env;

	private final List<Agent> agents;

	public RobotController(final Env env) {
		this.env = env;

		files = new File(OUTPUT_FOLDER).listFiles((dir1, filename) -> filename.endsWith(".zip"));

		agents = new ArrayList<>();

		new Maddpg(CAPACITY, this, MAX_EPISODE, MAX_STEP, BATCH_SIZE).run();
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
	public StepObs step(final Action[] actions) {
		Float[] rewards = new Float[4];

		final INDArray[] nextStates = new INDArray[4];

		// Set next action
		for (int i = 0; i < 4; i++) {
			agents.get(i).setAction(actions[i]);
		}
		// agents.get(4).setAction(Action.getRandomAction());

		// step agent through environment
		executeAction();

		// Collect the states after the agents have moved
		for (int i = 0; i < 4; i++) {
			nextStates[i] = agents.get(i).getObservation();
			rewards[i] = agents.get(i).getReward(actions[i]);
		}

		// Joint award
		// final long count = agents.subList(0, 3).stream().filter(Agent::isAtGoal).count();
		// if (count > 0) {
		// final float sum = (float) Arrays.stream(rewards).mapToDouble(i -> i).sum();
		// rewards = Arrays.stream(rewards).map(i -> i + sum).toArray(Float[]::new);
		// }

		return new StepObs(nextStates, rewards, ((Prey) agents.get(4)).isTrapped());
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
		for (int i = 0; i < AGENT_COUNT; i++) {
			if (agents.size() < AGENT_COUNT - 1) {
				agents.add(new Hunter(env.getAndSetHunter(i), DELAY, env, this,
						env.getMode() == Mode.EVAL && files.length > 0 ? files[i] : null));
			} else if (agents.size() < AGENT_COUNT) {
				agents.add(new Prey(env.getAndSetPrey(), DELAY, env, this, null));
			}
			// check if agents is on another agent
			do {
				final int randomPosX = agents.get(i).getSimulatedRobot().getRandomPos();
				final int randomPosY = agents.get(i).getSimulatedRobot().getRandomPos();
				agents.get(i).setPose(randomPosX, randomPosY, 0);
			} while (isSamePosition(agents.get(i)));
		}
	}

	private boolean isSamePosition(final Agent agent) {
		return agents.stream()
				.anyMatch(i -> i != agent && agent.getX() == i.getX() && agent.getY() == i.getY());
	}

}
