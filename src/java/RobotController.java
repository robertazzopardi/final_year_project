import java.util.logging.Logger;

import comp329robosim.SimulatedRobot;

public class RobotController {
	private final SimulationEnv env;

	private Hunter[] hunters;

	private final Logger logger = Logger.getLogger("final_year_project." + RobotController.class.getName());

	private Prey prey;

	public RobotController(final SimulationEnv env) {
		this.env = env;

		initRobots();

		startRobots();
	}

	private void initRobots() {
		// initialise the robots from the environment
		final SimulatedRobot smPrey = env.getController().getSimulatedRobot();
		prey = new Prey(smPrey, 1000, env, this);

		final SimulatedRobot[] smHunters = env.getController().getHunters();
		hunters = new Hunter[smHunters.length];
		for (int i = 0; i < smHunters.length; i++) {
			hunters[i] = new Hunter(smHunters[i], 1000, env, this);
		}
	}

	public void resumeHunters() {
		for (final Hunter hunter : hunters) {
			if (hunter.isPaused()) {
				final String rlog = "resuming hunter: " + hunter.getName();
				logger.info(rlog);
				hunter.resumeRobot();
			}
		}
	}

	private void startRobots() {
		prey.start();
		for (int i = 0; i < hunters.length; i++) {
			hunters[i].start();
		}
	}
}
