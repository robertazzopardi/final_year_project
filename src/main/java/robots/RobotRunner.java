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

	static final int CELL_DISTANCE = 350;

	static int moveCount = RobotController.STEP_COUNT;

	final SimulationEnv env;

	volatile boolean exit = false;

	final MyGridCell[][] grid;

	Logger logger;

	final RobotController controller;

	RobotRunner(final SimulatedRobot r, final int d, final SimulationEnv env, final RobotController controller) {
		super(r, d);

		monitorRobotStatus(false);

		setTravelSpeed(100);

		this.env = env;

		this.controller = controller;

		this.grid = env.getGrid();
	}

	abstract boolean canMove(int x, int y);

	/**
	 * get x position on the grid from the robots location
	 *
	 * @return
	 */
	final int getGridPosX() {
		return (int) ((((double) getX() / CELL_DISTANCE) * 2) - 1) / 2;
	}

	/**
	 * get y position on the grid from the robots location
	 *
	 * @return
	 */
	final int getGridPosY() {
		return (int) ((((double) getY() / CELL_DISTANCE) * 2) - 1) / 2;
	}

	public void stopRobot() {
		exit = true;
	}

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

	abstract void left(final Direction left);

	abstract void down(final Direction down);

	abstract void right(final Direction right);

	abstract void up(final Direction up);

	final void forward() {
		switch (Direction.fromDegree(getHeading())) {
		case DOWN:
			down(Direction.DOWN);
			break;
		case RIGHT:
			right(Direction.RIGHT);
			break;
		case UP:
			up(Direction.UP);
			break;
		case LEFT:
			left(Direction.LEFT);
			break;
		default:
			break;
		}
	}

	void doAction(final Action direction) {
		switch (direction) {
		case FORWARD:
			forward();
			break;

		case LEFT:
			if (env.getMode() == Mode.EVAL) {
				rotate(-90);
				forward();
			} else {
				setPose(getX(), getY(), getHeading() + -90);
			}
			break;

		case RIGHT:
			if (env.getMode() == Mode.EVAL) {
				rotate(90);
				forward();
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
