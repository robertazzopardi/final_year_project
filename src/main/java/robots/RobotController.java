package robots;

import java.util.logging.Logger;

import comp329robosim.SimulatedRobot;
import intelligence.QLearning;
import simulation.SimulationEnv;

public class RobotController {

	private final Hunter[] hunters = new Hunter[4];

	private static final Logger logger = Logger.getLogger("final_year_project." + RobotController.class.getName());

	private Prey prey;

	private final SimulationEnv env;

	public RobotController(final SimulationEnv env) {

		this.env = env;

		initRobots();

		startRobots();

	}

	private void initRobots() {
		// initialise the robots from the environment
		final SimulatedRobot preyRobot = env.getAndSetPrey();
		prey = new Prey(preyRobot, 1000, env, this);

		for (int i = 0; i < hunters.length; i++) {
			final SimulatedRobot simulatedRobot = env.getAndSetHunter(i);
			hunters[i] = new Hunter(simulatedRobot, 1000, env, this, new QLearning(env.getGrid()));
		}
	}

	public void restartRobots() {
		final SimulatedRobot preyRobot = env.getAndSetPrey();
		prey = new Prey(preyRobot, 1000, env, this);

		for (int i = 0; i < 4; i++) {
			final SimulatedRobot simulatedHunter = env.getAndSetHunter(i);
			hunters[i] = new Hunter(simulatedHunter, 1000, env, this, hunters[i].getLearning());
		}

		startRobots();
	}

	public void resumeHunters() {
		for (Hunter hunter : hunters) {
			if (hunter.isPaused()) {
				final String rlog = "resuming hunter: " + Hunter.getHunterCount();
				logger.info(rlog);
				hunter.resumeRobot();
			}
		}
	}

	private void startRobots() {
		prey.start();
		for (final Hunter hunter : hunters) {
			hunter.start();
		}
	}

	public void stopRobots() {
		prey.stopRobot();
		for (final Hunter hunter : hunters) {
			hunter.stopRobot();
		}
	}

}
