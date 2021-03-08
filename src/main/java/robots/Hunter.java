package robots;

import java.util.Arrays;
import java.util.logging.Logger;

import comp329robosim.MyGridCell;
import comp329robosim.OccupancyType;
import comp329robosim.SimulatedRobot;
import intelligence.DeepQLearning;
import simulation.SimulationEnv;
import simulation.Mode;

/**
 *
 */
final class Hunter extends RobotRunner {

	private static int hunterCount = 1;

	private static void resetHunterCount() {
		hunterCount = 1;
	}

	private DeepQLearning learning;

	private final int number;

	private volatile boolean paused = false;

	private final Object pauseLock = new Object();

	private final Hunter[] otherHunters = new Hunter[3];

	private final Prey prey;

	// Grid min max
	private static final int MIN_GRID = 1;
	private static final int MAX_GRID = SimulationEnv.GRID_SIZE;

	// Range scanners
	// private static final int SENSOR_SCAN_MIN = 0;
	private static final int SENSOR_SCAN_MAX = 2550;

	public void setOthers(final Hunter[] hunters) {
		int index = 0;

		for (final Hunter hunter : hunters) {
			if (!hunter.equals(this)) {
				otherHunters[index++] = hunter;
			}
		}
	}

	public Hunter(final SimulatedRobot r, final int d, final SimulationEnv env, final DeepQLearning learning,
			final RobotController controller, final Prey prey) {
		super(r, d, env, controller);

		this.number = hunterCount++;

		this.logger = Logger.getLogger("Hunter " + number);

		env.updateGridHunter(getGridPosX(), getGridPosY());

		this.learning = learning;

		this.prey = prey;
	}

	// @Override
	// boolean canMove(final int x, final int y) {
	// return grid[y][x].isEmpty() || grid[y][x].getCellType() ==
	// OccupancyType.GOAL;
	// }

	@Override
	boolean canMove(final int x, final int y) {
		// return grid[y][x].isEmpty() || grid[y][x].getCellType() ==
		// OccupancyType.GOAL;
		return grid[y][x].getCellType() != OccupancyType.OBSTACLE && grid[y][x].getCellType() != OccupancyType.HUNTER
				&& grid[y][x].getCellType() != OccupancyType.PREY;
	}

	public DeepQLearning getLearning() {
		return learning;
	}

	public void setLearning(final DeepQLearning learning) {
		this.learning = learning;
	}

	public boolean isAdjacentToPrey() {
		final int x = getGridPosX();
		final int y = getGridPosY();
		return grid[y][x - 1].getCellType() == OccupancyType.PREY || grid[y][x + 1].getCellType() == OccupancyType.PREY
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

	public boolean isAdjacentToObstacle() {
		final int x = getGridPosX();
		final int y = getGridPosY();
		return grid[y][x - 1].getCellType() == OccupancyType.OBSTACLE
				|| grid[y][x + 1].getCellType() == OccupancyType.OBSTACLE
				|| grid[y - 1][x].getCellType() == OccupancyType.OBSTACLE
				|| grid[y + 1][x].getCellType() == OccupancyType.OBSTACLE;
	}

	public boolean isAdjacentToPrey(final int x, final int y) {
		if (x == SimulationEnv.GRID_SIZE - 1 || y == SimulationEnv.GRID_SIZE - 1 || x == 0 || y == 0) {
			return false;
		}
		return grid[y][x - 1].getCellType() == OccupancyType.PREY || grid[y][x + 1].getCellType() == OccupancyType.PREY
				|| grid[y - 1][x].getCellType() == OccupancyType.PREY
				|| grid[y + 1][x].getCellType() == OccupancyType.PREY;
	}

	public boolean isPaused() {
		return paused;
	}

	private void pauseRobot() {
		paused = true;
	}

	public void resumeRobot() {
		synchronized (pauseLock) {
			paused = false;
			pauseLock.notifyAll();
		}
	}

	private void deepLearningRunning() {
		float[] currState = getStates();
		float[] newState = null;

		Action action = Action.getRandomAction();

		double score = 0;
		final boolean gameMode = env.getMode() != Mode.EVAL;

		final Hunter[] hunters = new Hunter[] { otherHunters[0], otherHunters[1], otherHunters[2], this };

		final int previousTurnCount = 0;

		while (!exit) {
			// double score = -.1;

			// // check if in a goal state
			// if (isAdjacentToPrey()) {
			// // Do nothing while in goal state
			// // logger.info("in a goal state");
			// env.updateGridHunter(getGridPosX(), getGridPosY());
			// pauseRobot();
			// }

			// // check if paused and should be waiting
			// synchronized (pauseLock) {
			// if (paused) {
			// try {
			// pauseLock.wait();
			// } catch (final InterruptedException ex) {
			// ex.printStackTrace();
			// Thread.currentThread().interrupt();
			// }
			// }
			// }

			// compare the current state to the next state produced from qlearning

			// Action lastAction = action;

			action = learning.getActionFromStates(currState);
			final int x = getGridPosX();
			final int y = getGridPosY();

			final MyGridCell down = grid[y + 1][x];
			final MyGridCell up = grid[y - 1][x];
			final MyGridCell right = grid[y][x + 1];
			final MyGridCell left = grid[y][x - 1];
			logger.info(action.toString() + " " + up + " " + down + " " + left + " " + right);

			if (gameMode) {
				learning.updateEpsilon();

				// score = getScore(action);
				// if (action == Action.TRAVEL) {
				// score = canMoveScore();
				// }

				score = getScoreForAction(action);
			}

			// int px = prey.getGridPosX();
			// int py = prey.getGridPosX();
			// System.out.println(Arrays.toString(
			// Arrays.stream(prey.getFreeAdjacentSquares()).map(i ->
			// getManhattenDistance(px, py, i)).toArray()));

			// int oldDist = getManhattenDistance(getGridPosX(), getGridPosY(),
			// prey.getGridPosX(), prey.getGridPosY());

			// boolean oldGState = isAdjacentToPrey();

			// int oldcount = (int)
			// Arrays.stream(hunters).filter(Hunter::isAdjacentToPrey).count();

			doAction(action);

			// int newcount = (int)
			// Arrays.stream(hunters).filter(Hunter::isAdjacentToPrey).count();

			// int newDist = getManhattenDistance(getGridPosX(), getGridPosY(),
			// prey.getGridPosX(), prey.getGridPosY());

			// boolean newGState = isAdjacentToPrey();

			newState = getStates();

			if (gameMode) {

				// int count = (int)
				// Arrays.stream(hunters).filter(Hunter::isAdjacentToPrey).count();

				// System.out.println(Arrays.stream(prey.getFreeAdjacentSquares()).map(mapper));

				// logger.info(Double.toString(score));
				learning.update(currState, action, score, newState);

			}

			currState = newState;

			incrementMoves();

		}

		if (moveCount >= RobotController.STEP_COUNT) {
			learning.update(currState, action, -.1, newState);
		} else {
			learning.update(currState, action, Arrays.stream(hunters).filter(Hunter::isAdjacentToPrey).count() / 10.0,
					newState);
		}

		// controller.addCaptureScore(currState, action, this);
	}

	public double getScoreForAction(final Action action) {

		double score = 0;
		switch (action) {
		case TRAVEL:

			score += addTravelScore();
			score += canMoveScore();

			break;
		case NOTHING:
			if (isAdjacentToPrey()) {
				score += .1;
			}
			if (isAdjacentToHunter()) {
				score -= .1;
			}
			break;
		case LEFT_TURN:
			// score += addTurnScore();
			// score += canMoveScore();
			// break;
		case RIGHT_TURN:
			score += addTurnScore();
			score += canMoveScore();
			break;
		default:
			break;
		}

		return score;
	}

	// private double getScore(final Action action) {
	// // double score = -1;
	// double score = -0.1;

	// switch (action) {
	// case LEFT_TURN:
	// score = addTurnScore((getHeading() - 90) % 360);
	// // score = -0.01;
	// break;

	// case RIGHT_TURN:
	// score = addTurnScore((getHeading() + 90) % 360);
	// // score = -0.01;

	// break;

	// case TRAVEL:
	// score = addTravelScore();

	// // if (isAdjacentToPrey()) {
	// // score = -0.1;
	// // }
	// // score = 0.5;

	// break;

	// case NOTHING:
	// if (!isAdjacentToPrey()) {
	// score = -0.01;
	// }

	// if (isAdjacentToPrey()) {
	// score = 1;
	// }
	// // score = -0.1;

	// break;

	// default:
	// break;
	// }

	// // logger.info(Double.toString(score));
	// return score;
	// }

	private double addTravelScore() {
		final int x = getGridPosX();
		final int y = getGridPosY();
		double score = 0;

		switch (Direction.fromDegree(getHeading())) {
		case DOWN:
			// if (getManhattenDistance(x, y + 1, px, py) >= oldDist) {
			// score -= .1;
			// }

			if (!isAdjacentToPrey(x, y + 1)) {
				score -= .1;
			}

			if (!canMove(x, y + 1)) {
				score -= .1;
			}

			break;

		case RIGHT:
			// if (getManhattenDistance(x + 1, y, px, py) >= oldDist) {
			// score -= .1;
			// }

			if (!isAdjacentToPrey(x + 1, y)) {
				score -= .1;
			}

			if (!canMove(x + 1, y)) {
				score -= .1;
			}

			break;

		case UP:
			// if (getManhattenDistance(x, y - 1, px, py) >= oldDist) {
			// score -= .1;
			// }

			if (!isAdjacentToPrey(x, y - 1)) {
				score -= .1;
			}

			if (canMove(x, y - 1)) {
				score -= .1;
			}

			break;

		case LEFT:
			// if (getManhattenDistance(x - 1, y, px, py) >= oldDist) {
			// score -= .1;
			// }

			if (!isAdjacentToPrey(x - 1, y)) {
				score -= .1;
			}

			if (!canMove(x - 1, y)) {
				score -= .1;
			}

			break;

		default:
			break;
		}

		return score;
	}

	private double canMoveScore() {
		final int x = getGridPosX();
		final int y = getGridPosY();
		double score = 0;

		switch (Direction.fromDegree(getHeading())) {
		case DOWN:

			if (!canMove(x, y + 1)) {
				score = -.15;
			}

			break;

		case RIGHT:

			if (!canMove(x + 1, y)) {
				score = -.15;
			}

			break;

		case UP:

			if (canMove(x, y - 1)) {
				score = -.15;
			}

			break;

		case LEFT:

			if (!canMove(x - 1, y)) {
				score = -.15;
			}

			break;

		default:
			break;
		}

		return score;
	}

	private double addTurnScore() {
		final int x = getGridPosX();
		final int y = getGridPosY();
		// final int px = prey.getGridPosX();
		// final int py = prey.getGridPosY();

		double score = 0;

		switch (Direction.fromDegree(getHeading())) {
		case DOWN:
			if (!isAdjacentToPrey(x, y + 1)) {
				score = -.1;
			}
			break;

		case RIGHT:
			if (!isAdjacentToPrey(x + 1, y)) {
				score = -.1;
			}
			break;

		case UP:
			if (!isAdjacentToPrey(x, y - 1)) {
				score = -.1;
			}
			break;

		case LEFT:
			if (!isAdjacentToPrey(x - 1, y)) {
				score = -.1;
			}
			break;

		default:
			break;
		}

		return score;
	}

	private void doAction(final Action action) {
		switch (action) {
		case TRAVEL:
			forward();
			break;

		case LEFT_TURN:
			if (env.getMode() == Mode.EVAL) {
				rotate(-90);
			} else {
				setPose(getX(), getY(), getHeading() + -90);
			}
			break;

		case RIGHT_TURN:
			if (env.getMode() == Mode.EVAL) {
				rotate(90);
			} else {
				setPose(getX(), getY(), getHeading() + 90);
			}
			break;

		case NOTHING:
			break;

		default:
			break;
		}

	}

	private int getManhattenDistance(final int x1, final int y1, final int x2, final int y2) {
		final int i = Math.abs(x2 - x1) + Math.abs(y2 - y1);
		return i == 0 ? SimulationEnv.GRID_SIZE + 1 : i;
	}

	private int getManhattenDistance(final int x2, final int y2, final Direction direction) {
		// return Math.abs(x2 - getGridPosX()) + Math.abs(y2 - getGridPosY());
		switch (direction) {
		case UP:
			return Math.abs(x2 - getGridPosX()) + Math.abs(y2 - 1 - getGridPosY());
		case DOWN:
			return Math.abs(x2 - getGridPosX()) + Math.abs(y2 + 1 - getGridPosY());
		case LEFT:
			return Math.abs(x2 - 1 - getGridPosX()) + Math.abs(y2 - getGridPosY());
		case RIGHT:
			return Math.abs(x2 + 1 - getGridPosX()) + Math.abs(y2 - getGridPosY());
		default:
			break;
		}
		return SimulationEnv.GRID_SIZE + 1;
	}

	private static float getNormalisedManhattenDistance(final int x1, final int y1, final int x2, final int y2) {
		return normalise(Math.abs(x2 - x1) + Math.abs(y2 - y1), 1, SimulationEnv.GRID_SIZE);
	}

	public float[] getStates() {
		final float[] states = new float[RobotController.STATE_COUNT];

		// normalised x and y positions
		states[0] = normalise(getGridPosX(), MIN_GRID, MAX_GRID);
		states[1] = normalise(getGridPosY(), MIN_GRID, MAX_GRID);

		states[2] = normalise(otherHunters[0].getGridPosX(), MIN_GRID, MAX_GRID);
		states[3] = normalise(otherHunters[0].getGridPosY(), MIN_GRID, MAX_GRID);

		states[4] = normalise(otherHunters[1].getGridPosX(), MIN_GRID, MAX_GRID);
		states[5] = normalise(otherHunters[1].getGridPosY(), MIN_GRID, MAX_GRID);

		states[6] = normalise(otherHunters[2].getGridPosX(), MIN_GRID, MAX_GRID);
		states[7] = normalise(otherHunters[2].getGridPosY(), MIN_GRID, MAX_GRID);

		states[8] = normalise(prey.getGridPosX(), MIN_GRID, MAX_GRID);
		states[9] = normalise(prey.getGridPosY(), MIN_GRID, MAX_GRID);

		// manhattan distances from the prey
		// TODO: manhattan distance between all adjacent distances
		states[10] = getNormalisedManhattenDistance(getGridPosX(), getGridPosY(), prey.getGridPosX(),
				prey.getGridPosX());
		states[11] = getNormalisedManhattenDistance(otherHunters[0].getGridPosX(), otherHunters[0].getGridPosY(),
				prey.getGridPosX(), prey.getGridPosX());
		states[12] = getNormalisedManhattenDistance(otherHunters[1].getGridPosX(), otherHunters[1].getGridPosY(),
				prey.getGridPosX(), prey.getGridPosX());
		states[13] = getNormalisedManhattenDistance(otherHunters[2].getGridPosX(), otherHunters[2].getGridPosY(),
				prey.getGridPosX(), prey.getGridPosX());

		// headings
		// states[14] = normalise(getUSenseRange(), -360, 360);
		// states[15] = normalise(getUSenseRange(), -360, 360);

		// states[16] = normalise(otherHunters[0].getUSenseRange(), -360, 360);
		// states[17] = normalise(otherHunters[0].getUSenseRange(), -360, 360);

		// states[18] = normalise(otherHunters[1].getUSenseRange(), -360, 360);
		// states[19] = normalise(otherHunters[1].getUSenseRange(), -360, 360);

		// states[20] = normalise(otherHunters[2].getUSenseRange(), -360, 360);
		// states[21] = normalise(otherHunters[2].getUSenseRange(), -360, 360);

		// states[22] = normalise(prey.getUSenseRange(), -360, 360);
		// states[23] = normalise(prey.getUSenseRange(), -360, 360);

		// TODO: get the x and y of the prey
		// 0 otherwise
		// if a robot is adjacent to the prey get the co-ordinates
		// if the prey is scanned get the co-ordinates
		return states;
	}

	// private final float getNormSenseRange() {
	// return normalise(getUSenseRange(), SENSOR_SCAN_MIN, SENSOR_SCAN_MAX);
	// }

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
	final void left() {
		final int x = getGridPosX();
		final int y = getGridPosY();

		if (canMove(x - 1, y)) {
			env.updateGridEmpty(x, y);
			env.updateGridHunter(x - 1, y);

			if (env.getMode() == Mode.EVAL) {
				travel(CELL_DISTANCE);
			} else {
				setPose(getX() - CELL_DISTANCE, getY(), getHeading());
			}
		}
	}

	@Override
	final void up() {
		final int x = getGridPosX();
		final int y = getGridPosY();

		if (canMove(x, y - 1)) {
			env.updateGridEmpty(x, y);
			env.updateGridHunter(x, y - 1);

			if (env.getMode() == Mode.EVAL) {
				travel(CELL_DISTANCE);
			} else {
				setPose(getX(), getY() - CELL_DISTANCE, getHeading());
			}
		}
	}

	@Override
	final void right() {
		final int x = getGridPosX();
		final int y = getGridPosY();

		if (canMove(x + 1, y)) {
			env.updateGridEmpty(x, y);
			env.updateGridHunter(x + 1, y);

			if (env.getMode() == Mode.EVAL) {
				travel(CELL_DISTANCE);
			} else {
				setPose(getX() + CELL_DISTANCE, getY(), getHeading());
			}
		}
	}

	@Override
	final void down() {
		final int x = getGridPosX();
		final int y = getGridPosY();

		if (canMove(x, y + 1)) {
			env.updateGridEmpty(x, y);
			env.updateGridHunter(x, y + 1);

			if (env.getMode() == Mode.EVAL) {
				travel(CELL_DISTANCE);
			} else {
				setPose(getX(), getY() + CELL_DISTANCE, getHeading());
			}
		}
	}
}
