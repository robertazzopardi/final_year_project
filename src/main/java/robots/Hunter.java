package robots;

import java.util.Random;
import java.util.logging.Logger;
import comp329robosim.OccupancyType;
import comp329robosim.SimulatedRobot;
import intelligence.DeepQLearning;
import simulation.Mode;
import simulation.SimulationEnv;

/**
 *
 */
final class Hunter extends RobotRunner {

	private static final double REWARD = 1;

	private static final Random RANDOM = new Random();

	private static int hunterCount = 1;

	private static void resetHunterCount() {
		hunterCount = 1;
	}

	private final DeepQLearning learning;

	private final int number;

	private volatile boolean paused = false;

	private final Object pauseLock = new Object();

	private final Hunter[] otherHunters = new Hunter[3];

	private static Prey prey;

	// Grid min max
	private static final int MIN_GRID = 1;
	private static final int MAX_GRID = SimulationEnv.GRID_SIZE;

	// Range scanners
	// private static final int SENSOR_SCAN_MIN = 0;
	// private static final int SENSOR_SCAN_MAX = 2550;

	public void setOthers(final Hunter[] hunters) {
		int index = 0;

		for (final Hunter hunter : hunters) {
			if (!hunter.equals(this)) {
				otherHunters[index++] = hunter;
			}
		}
	}

	public Hunter(final SimulatedRobot r, final int d, final SimulationEnv env,
			final DeepQLearning learning, final RobotController controller, final Prey prey) {
		super(r, d, env, controller);

		this.number = hunterCount++;

		this.logger = Logger.getLogger("Hunter " + number);

		env.updateGridHunter(getGridPosX(), getGridPosY());

		this.learning = learning;

		Hunter.prey = prey;
	}

	@Override
	boolean canMove(final int x, final int y) {
		return grid[y][x].getCellType() != OccupancyType.OBSTACLE
				&& grid[y][x].getCellType() != OccupancyType.HUNTER
				&& grid[y][x].getCellType() != OccupancyType.PREY;
	}

	public DeepQLearning getLearning() {
		return learning;
	}

	public boolean isAdjacentToPrey() {
		final int x = getGridPosX();
		final int y = getGridPosY();
		return grid[y][x - 1].getCellType() == OccupancyType.PREY
				|| grid[y][x + 1].getCellType() == OccupancyType.PREY
				|| grid[y - 1][x].getCellType() == OccupancyType.PREY
				|| grid[y + 1][x].getCellType() == OccupancyType.PREY;
	}

	public boolean isAdjacentToHunter() {
		final int x = getGridPosX();
		final int y = getGridPosY();
		return grid[y][x - 1].getCellType() == OccupancyType.HUNTER
				|| grid[y][x + 1].getCellType() == OccupancyType.HUNTER
				|| grid[y - 1][x].getCellType() == OccupancyType.HUNTER
				|| grid[y + 1][x].getCellType() == OccupancyType.HUNTER;
	}

	// public boolean isAdjacentToObstacle() {
	// final int x = getGridPosX();
	// final int y = getGridPosY();
	// return grid[y][x - 1].getCellType() == OccupancyType.OBSTACLE
	// || grid[y][x + 1].getCellType() == OccupancyType.OBSTACLE
	// || grid[y - 1][x].getCellType() == OccupancyType.OBSTACLE
	// || grid[y + 1][x].getCellType() == OccupancyType.OBSTACLE;
	// }

	// private boolean isAdjacentTo(final int x, final int y, final OccupancyType occupancy) {
	// try {
	// return grid[y][x - 1].getCellType() == occupancy
	// || grid[y][x + 1].getCellType() == occupancy
	// || grid[y - 1][x].getCellType() == occupancy
	// || grid[y + 1][x].getCellType() == occupancy;
	// } catch (final ArrayIndexOutOfBoundsException ignored) {
	// return false;
	// }
	// }

	public boolean isAdjacentToPrey(final int x, final int y) {
		try {
			return grid[y][x - 1].getCellType() == OccupancyType.PREY
					|| grid[y][x + 1].getCellType() == OccupancyType.PREY
					|| grid[y - 1][x].getCellType() == OccupancyType.PREY
					|| grid[y + 1][x].getCellType() == OccupancyType.PREY;
		} catch (final ArrayIndexOutOfBoundsException ignored) {
			return false;
		}
	}

	public boolean isPaused() {
		return paused;
	}

	// private void pauseRobot() {
	// paused = true;
	// }

	public void resumeRobot() {
		synchronized (pauseLock) {
			paused = false;
			pauseLock.notifyAll();
		}
	}

	private void deepLearningRunning() {
		// int[] currState = getGridState();
		// int[] newState = null;
		float[] currState = getStates();
		float[] newState = null;

		Action action = null;

		double score = 0;
		final boolean gameMode = env.getMode() != Mode.EVAL;

		// final Hunter[] hunters =
		// new Hunter[] {otherHunters[0], otherHunters[1], otherHunters[2], this};

		while (!exit) {
			action = learning.getActionFromStates(currState);


			final Direction direction = Direction.fromDegree(getHeading());
			System.out.println(action + " " + isAdjacentToPrey() + " "
					+ canMove(direction.x(getGridPosX()), direction.y(getGridPosY())) + " "
					+ grid[direction.y(getGridPosY())][direction.x(getGridPosX())].getCellType()
					+ " hunter: " + number);


			if (gameMode) {
				learning.updateEpsilon();

				score = getScoreForAction(action);
			}

			doAction(action);

			newState = getStates();

			if (gameMode) {
				learning.update(currState, action, score, newState);
			}

			currState = newState;
		}

		if (prey.isCaptured()) {
			// score = Arrays.stream(hunters).filter(Hunter::isAdjacentToPrey).count();
			score = REWARD;
			learning.update(currState, action, score, newState);
		}
	}

	public double getScoreForAction(final Action action) {
		final Direction direction = Direction.fromDegree(getHeading());
		double score = 0;

		switch (action) {
			case FORWARD:
				if (!canMove(direction.x(getGridPosX()), direction.y(getGridPosY()))) {
					score = -REWARD;
				} else if (!isAdjacentToPrey(direction.x(getGridPosX()),
						direction.y(getGridPosY()))) {
					score = -REWARD;
				} else if (isAdjacentToPrey()) {
					score = REWARD;
				}
				break;

			case LEFT:
				final Direction left = Direction.fromDegree(getHeading() - 90);

				if (!isAdjacentToPrey(left.x(getGridPosX()), left.y(getGridPosY()))) {
					score = -REWARD;
				}
				break;

			case RIGHT:
				final Direction right = Direction.fromDegree(getHeading() + 90);

				if (!isAdjacentToPrey(right.x(getGridPosX()), right.y(getGridPosY()))) {
					score = -REWARD;
				}
				break;

			case NOTHING:
				if (isAdjacentToPrey() && !isAdjacentToHunter()) {
					score = REWARD;
				} else if (!isAdjacentToPrey()) {
					score = -REWARD;
				}
				break;

			default:
				break;
		}

		// switch (action) {
		// case UP:
		// if (!isAdjacentToPrey(direction.x(getGridPosX()), direction.y(getGridPosY()))) {
		// score = -REWARD;
		// }
		// break;
		// case DOWN:
		// if (!isAdjacentToPrey(direction.x(getGridPosX()), direction.y(getGridPosY()))) {
		// score = -REWARD;
		// }
		// break;
		// case LEFT:
		// if (!isAdjacentToPrey(direction.x(getGridPosX()), direction.y(getGridPosY()))) {
		// score = -REWARD;
		// }
		// break;
		// case RIGHT:
		// if (!isAdjacentToPrey(direction.x(getGridPosX()), direction.y(getGridPosY()))) {
		// score = -REWARD;
		// }
		// break;
		// case NOTHING:
		// if (!isAdjacentToPrey()) {
		// score = -REWARD;
		// }
		// break;

		// default:
		// break;
		// }


		return score;
	}

	@Override
	final void doAction(final Action direction) {
		super.doAction(direction);
		if (direction != Action.NOTHING) {
			incrementMoves();
		}
	}

	private static int getManhattenDistance(final int x1, final int y1, final int x2,
			final int y2) {
		// if (!grid[y2][x2].isEmpty()) {
		// return SimulationEnv.GRID_SIZE;
		// }
		// final int i = Math.abs(x2 - x1) + Math.abs(y2 - y1);
		// return i == 0 ? SimulationEnv.GRID_SIZE + 1 : i;
		return Math.abs(x2 - x1) + Math.abs(y2 - y1);
	}

	private static float getNormalisedManhattenDistance(final int x1, final int y1, final int x2,
			final int y2) {
		return normalise(getManhattenDistance(x1, y1, x2, y2), 1, SimulationEnv.GRID_SIZE);
	}

	public float[] getStates() {
		final float[] states = new float[RobotController.STATE_COUNT];

		// normalised x and y positions
		final int x = getGridPosX();
		final int y = getGridPosY();
		final int preyX = prey.getGridPosX();
		final int preyY = prey.getGridPosY();

		states[0] = normalise(x, MIN_GRID, MAX_GRID);
		states[1] = normalise(y, MIN_GRID, MAX_GRID);

		states[2] = normalise(otherHunters[0].getGridPosX(), MIN_GRID, MAX_GRID);
		states[3] = normalise(otherHunters[0].getGridPosY(), MIN_GRID, MAX_GRID);

		states[4] = normalise(otherHunters[1].getGridPosX(), MIN_GRID, MAX_GRID);
		states[5] = normalise(otherHunters[1].getGridPosY(), MIN_GRID, MAX_GRID);

		states[6] = normalise(otherHunters[2].getGridPosX(), MIN_GRID, MAX_GRID);
		states[7] = normalise(otherHunters[2].getGridPosY(), MIN_GRID, MAX_GRID);

		// prey adjacent x and y positions TODO: provided the preys location if known

		// if (knowPreyLocation/canSeePrey)

		// right
		states[8] = normalise(preyX + 1, MIN_GRID, MAX_GRID);
		states[9] = normalise(preyY, MIN_GRID, MAX_GRID);

		// left
		states[10] = normalise(preyX - 1, MIN_GRID, MAX_GRID);
		states[11] = normalise(preyY, MIN_GRID, MAX_GRID);

		// down
		states[12] = normalise(preyX, MIN_GRID, MAX_GRID);
		states[13] = normalise(preyY + 1, MIN_GRID, MAX_GRID);

		// up
		states[14] = normalise(preyX, MIN_GRID, MAX_GRID);
		states[15] = normalise(preyY - 1, MIN_GRID, MAX_GRID);

		// manhattan distances from the adjacent prey spaces
		states[16] = getNormalisedManhattenDistance(x, y, preyX + 1, preyY);
		states[17] = getNormalisedManhattenDistance(x, y, preyX - 1, preyY);
		states[18] = getNormalisedManhattenDistance(x, y, preyX, preyY + 1);
		states[19] = getNormalisedManhattenDistance(x, y, preyX, preyY - 1);

		shuffle(states);

		return states;
	}

	// private int[] getGridState() {
	// final int x = getGridPosX();
	// final int y = getGridPosY();

	// final Direction up = Direction.UP;
	// final Direction down = Direction.DOWN;
	// final Direction right = Direction.RIGHT;
	// final Direction left = Direction.LEFT;

	// final int[] is = new int[] {
	// // self attributes
	// // next to preys
	// isAdjacentToPrey() ? 1 : 0, otherHunters[0].isAdjacentToPrey() ? 1 : 0,
	// otherHunters[1].isAdjacentToPrey() ? 1 : 0,
	// otherHunters[2].isAdjacentToPrey() ? 1 : 0, isAdjacentToHunter() ? 1 : 0,

	// // next to hunter
	// // isAdjacentToHunter() ? 1 : 0,
	// // next to wall
	// // isAdjacentToObstacle() ? 1 : 0,
	// // up
	// isAdjacentToPrey(up.x(x), up.y(y)) ? 1 : 0,
	// // isAdjacentToPrey(up.x(up.x(x)), up.y(up.y(y))) ? 1 : 0,
	// // down
	// isAdjacentToPrey(down.x(x), down.y(y)) ? 1 : 0,
	// // isAdjacentToPrey(down.x(down.x(x)), down.y(down.y(y))) ? 1 : 0,
	// // left
	// isAdjacentToPrey(left.x(x), left.y(y)) ? 1 : 0,
	// // isAdjacentToPrey(left.x(left.x(x)), left.y(left.y(y))) ? 1 : 0,
	// // right
	// isAdjacentToPrey(right.x(x), right.y(y)) ? 1 : 0,
	// // isAdjacentToPrey(right.x(right.x(x)), right.y(right.y(y))) ? 1 : 0,
	// };

	// shuffle(is);

	// // System.out.println(Arrays.toString(is));
	// return is;
	// }

	/**
	 * Fisher–Yates shuffle Algorithm
	 * https://www.geeksforgeeks.org/shuffle-a-given-array-using-fisher-yates-shuffle-algorithm/
	 *
	 * @param arr
	 */
	// private static void shuffle(final int[] states) {
	// // Start from the last element and swap one by one. We don't
	// // need to run for the first element that's why i > 0
	// for (int i = states.length - 1; i > 0; i--) {

	// // Pick a random index from 0 to i
	// final int j = RANDOM.nextInt(i);

	// // Swap states[i] with the element at random index
	// final int temp = states[i];
	// states[i] = states[j];
	// states[j] = temp;
	// }
	// }
	private static void shuffle(final float[] states) {
		// Start from the last element and swap one by one. We don't
		// need to run for the first element that's why i > 0
		for (int i = states.length - 1; i > 0; i--) {

			// Pick a random index from 0 to i
			final int j = RANDOM.nextInt(i);

			// Swap states[i] with the element at random index
			final float temp = states[i];
			states[i] = states[j];
			states[j] = temp;
		}
	}

	@Override
	public void run() {
		deepLearningRunning();

		// final String endLog = "Hunter " + number + " Stopped";
		// logger.info(endLog);
	}

	@Override
	public void stopRobot() {
		super.stopRobot();
		resumeRobot();
		resetHunterCount();
	}

	@Override
	final void updateGrid(final int x, final int y) {
		env.updateGridHunter(x, y);
	}


}
