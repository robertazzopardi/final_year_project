package robots;

import java.util.logging.Logger;

import comp329robosim.MyGridCell;
import comp329robosim.RobotMonitor;
import comp329robosim.SimulatedRobot;
import simulation.Mode;
import simulation.SimulationEnv;

/**
 * @author rob
 *
 */
abstract class RobotRunner extends RobotMonitor {

	static int moveCount = RobotController.STEP_COUNT;

	final SimulationEnv env;

	volatile boolean exit = false;

	final MyGridCell[][] grid;

	Logger logger;

	final RobotController controller;

	private final boolean mode;

	RobotRunner(final SimulatedRobot r, final int d, final SimulationEnv env,
			final RobotController controller) {
		super(r, d);

		monitorRobotStatus(false);

		setTravelSpeed(100);

		this.env = env;

		this.controller = controller;

		this.grid = env.getGrid();

		mode = env.getMode() == Mode.EVAL;
	}

	/**
	 * Get whether the robot can move into the x and y position
	 *
	 * @param x
	 * @param y
	 * @return
	 */
	abstract boolean canMove(int x, int y);

	/**
	 * Set the cell at the x and y position with the value of the robot
	 *
	 * @param x
	 * @param y
	 */
	abstract void updateGrid(final int x, final int y);

	/**
	 * get x position on the grid from the robots location
	 *
	 * @return
	 */
	final int getGridPosX() {
		return (int) ((((double) getX() / SimulationEnv.CELL_DISTANCE) * 2) - 1) / 2;
	}

	/**
	 * get y position on the grid from the robots location
	 *
	 * @return
	 */
	final int getGridPosY() {
		return (int) ((((double) getY() / SimulationEnv.CELL_DISTANCE) * 2) - 1) / 2;
	}

	final int getDistanceAhead() {
		return (int) ((((double) getUSenseRange() / SimulationEnv.CELL_DISTANCE) * 2) - 1) / 2;
	}

	public void stopRobot() {
		exit = true;
	}

	/**
	 * Normalise value between -1 and 1
	 *
	 * @param x
	 * @param min
	 * @param max
	 * @return
	 */
	static final float normalise(final int x, final int min, final int max) {
		return (2 * ((float) (x - min) / (max - min))) - 1;
		// return (float) (x - min) / (max - min);
	}

	static synchronized void incrementMoves() {
		moveCount--;
	}

	static void resetMoves() {
		moveCount = RobotController.STEP_COUNT;
	}

	final void moveDirection(final Direction direction) {
		final int x = getGridPosX();
		final int y = getGridPosY();

		if (canMove(direction.x(x), direction.y(y))) {
			env.updateGridEmpty(x, y);
			updateGrid(direction.x(x), direction.y(y));

			if (env.getMode() == Mode.EVAL) {
				travel(SimulationEnv.CELL_DISTANCE);
			} else {
				setPose(direction.px(getX()), direction.py(getY()), getHeading());
			}
		}
	}

	/**
	 * Perform set in the environment based on chosen action
	 *
	 * @param action
	 */
	void doAction(final Action action) {
		switch (action) {
			case FORWARD:
				moveDirection(Direction.fromDegree(getHeading()));
				break;

			case LEFT:
				if (mode) {
					rotate(-90);
				} else {
					setPose(getX(), getY(), getHeading() - 90);
				}
				break;

			case RIGHT:
				if (mode) {
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

}
