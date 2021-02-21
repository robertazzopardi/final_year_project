package robots;

import comp329robosim.SimulatedRobot;
import intelligence.Intelligence;
import intelligence.NoAIException;
import simulation.SimulationEnv;

public class RobotController {

	private final Hunter[] hunters = new Hunter[4];

	private Prey prey;

	private final SimulationEnv env;

	private final String learningMethod;

	public RobotController(final SimulationEnv env, final String learningMethod) {
		this.env = env;

		this.learningMethod = learningMethod;

		initRobots();

		startRobots();
	}

	private void initRobots() {
		// initialise the robots from the environment
		final SimulatedRobot preyRobot = env.getAndSetPrey();
		prey = new Prey(preyRobot, 1000, env, this);

		for (int i = 0; i < hunters.length; i++) {
			final SimulatedRobot simulatedRobot = env.getAndSetHunter(i);

			try {
				do {
					hunters[i] = new Hunter(simulatedRobot, 1000, env, this,
							Intelligence.getIntelligence(this.learningMethod, env));
				} while (isSamePosition(i));
			} catch (NoAIException e) {
				e.printStackTrace();
			}
		}

		for (Hunter hunter : hunters) {
			hunter.setOthers(hunters);
		}
	}

	private boolean isSamePosition(int i) {
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
		prey = new Prey(preyRobot, 1000, env, this);

		for (int i = 0; i < 4; i++) {
			final SimulatedRobot simulatedHunter = env.getAndSetHunter(i);

			do {
				hunters[i] = new Hunter(simulatedHunter, 1000, env, this, hunters[i].getLearning());
			} while (isSamePosition(i));
		}

		for (Hunter hunter : hunters) {
			hunter.setOthers(hunters);
		}

		startRobots();
	}

	public void resumeHunters() {
		for (Hunter hunter : hunters) {
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
