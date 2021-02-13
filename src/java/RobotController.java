import java.util.Random;
import java.util.logging.Logger;

import comp329robosim.SimulatedRobot;

public class RobotController {

	private SimulatedRobot preyRobot;

	private SimulatedRobot[] simulatedHunters = new SimulatedRobot[4];

	private static final int[] xyPositions = new int[] { 3, 5, 7, 9, 11, 13, 15, 17 };

	private static final Random RANDOM = new Random();

	private static int getRandomDirection() {
		final int rnd = RANDOM.nextInt(xyPositions.length);
		return xyPositions[rnd];
	}

	////////

	private final SimulationEnv env;

	private Hunter[] hunters;

	private final Logger logger = Logger.getLogger("final_year_project." + RobotController.class.getName());

	private Prey prey;

	private final PrintGridThread printGridThread;

	public RobotController(final SimulationEnv env) {
		this.env = env;
		///////

		setSimulatedRobots(env);

		///////
		this.printGridThread = new PrintGridThread(env);

		initRobots();

		startRobots();

		printGridThread.start();
	}

	public void setSimulatedRobots(final SimulationEnv env) {
		int preyX = getRandomDirection();
		int preyY = getRandomDirection();

		preyRobot = new SimulatedRobot(env.getController(), env.getController().getModel(), preyX, preyY);
		env.getController().setPrey(this.preyRobot);
		preyRobot.start();

		for (int i = 0; i < 4; i++) {
			int hunterX = getRandomDirection();
			int hunterY = getRandomDirection();

			for (int j = 0; j < i; j++) {
				while ((hunterX == preyX && hunterY == preyY)
						|| (hunterX == simulatedHunters[j].getX() && hunterY == simulatedHunters[j].getY())) {
					hunterX = getRandomDirection();
					hunterY = getRandomDirection();
				}
			}

			final SimulatedRobot sr = new SimulatedRobot(env.getController(), env.getController().getModel(), hunterX,
					hunterY);

			simulatedHunters[i] = sr;

			env.getController().setHunter(sr, i);

			sr.start();
		}
	}

	public PrintGridThread getPrintGridThread() {
		return printGridThread;
	}

	private void initRobots() {
		// initialise the robots from the environment
		// final SimulatedRobot smPrey = env.getController().getSimulatedRobot();
		final SimulatedRobot smPrey = preyRobot;
		prey = new Prey(smPrey, 1000, env, this);

		// final SimulatedRobot[] smHunters = env.getController().getHunters();
		final SimulatedRobot[] smHunters = simulatedHunters;
		hunters = new Hunter[smHunters.length];
		for (int i = 0; i < smHunters.length; i++) {
			hunters[i] = new Hunter(smHunters[i], 1000, env, this, i + 1, new QLearning(env.getGrid()));
		}
	}

	public void resumeHunters() {
		for (int i = 0; i < hunters.length; i++) {
			if (hunters[i].isPaused()) {
				final String rlog = "resuming hunter: " + (i + 1);
				logger.info(rlog);
				hunters[i].resumeRobot();
			}
		}
	}

	private void startRobots() {
		prey.start();
		for (final Hunter hunter : hunters) {
			hunter.start();
		}
	}

	public void stopHunters() {
		for (final Hunter hunter : hunters) {
			hunter.stopRobot();
		}
	}

}
