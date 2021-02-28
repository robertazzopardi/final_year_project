package robots;

import comp329robosim.SimulatedRobot;
import intelligence.DeepQLearning;
import simulation.SimulationEnv;

public class RobotController {

	private static final int DELAY = 1000;

	private final Hunter[] hunters = new Hunter[4];

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
		prey = new Prey(preyRobot, DELAY, env, this);

		for (int i = 0; i < hunters.length; i++) {
			do {
				final SimulatedRobot simulatedRobot = env.getAndSetHunter(i);
				hunters[i] = new Hunter(simulatedRobot, DELAY, env,
						new DeepQLearning(Hunter.STATE_COUNT, Action.LENGTH), prey);
			} while (isSamePosition(i));
		}

		for (final Hunter hunter : hunters) {
			hunter.setOthers(hunters);
		}
	}

	private boolean isSamePosition(final int i) {
		if (hunters[i].getGridPosX() == prey.getGridPosX() && hunters[i].getGridPosY() == prey.getGridPosY()) {
			return true;
		}

		for (int j = 0; j < i; j++) {
			if (hunters[i].getGridPosX() == hunters[j].getGridPosX()
					&& hunters[i].getGridPosY() == hunters[j].getGridPosY()) {
				return true;
			}
		}

		return false;
	}

	public void restartRobots() {
		final SimulatedRobot preyRobot = env.getAndSetPrey();
		prey = new Prey(preyRobot, DELAY, env, this);

		for (int i = 0; i < 4; i++) {
			do {
				final SimulatedRobot simulatedHunter = env.getAndSetHunter(i);
				hunters[i] = new Hunter(simulatedHunter, DELAY, env, hunters[i].getLearning(), prey);
			} while (isSamePosition(i));
		}

		for (final Hunter hunter : hunters) {
			hunter.setOthers(hunters);
		}

		startRobots();
	}

	public void resumeHunters() {
		for (final Hunter hunter : hunters) {
			hunter.resumeRobot();
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
