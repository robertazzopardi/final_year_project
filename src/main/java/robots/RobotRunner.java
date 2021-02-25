package robots;

import java.util.logging.Logger;

import comp329robosim.MyGridCell;
import comp329robosim.RobotMonitor;
import comp329robosim.SimulatedRobot;
import simulation.SimulationEnv;
import simulation.SimulationEnv.Mode;

/**
 * @author rob
 *
 */
abstract class RobotRunner extends RobotMonitor {

	final SimulationEnv env;

	volatile boolean exit = false;

	final MyGridCell[][] grid;

	Logger logger;

	RobotRunner(final SimulatedRobot r, final int d, final SimulationEnv env) {
		super(r, d);

		monitorRobotStatus(false);

		setTravelSpeed(100);

		this.env = env;

		this.grid = env.getGrid();
	}

	abstract boolean canMove(int x, int y);

	final int getCurentState(final int currX, final int currY) {
		return Integer.parseInt(Integer.toString(currY) + Integer.toString(currX));
	}

	/**
	 * get x position on the grid from the robots location
	 *
	 * @return
	 */
	final int getGridPosX() {
		return (int) ((((double) getX() / 350) * 2) - 1) / 2;
	}

	/**
	 * get y position on the grid from the robots location
	 *
	 * @return
	 */
	final int getGridPosY() {
		return (int) ((((double) getY() / 350) * 2) - 1) / 2;
	}

	// /**
	// * handle moving down a row
	// */
	// abstract void moveDown(int x, int y, int a);

	// /**
	// * handle moving left a column
	// */
	// abstract void moveLeft(int x, int y, int a);

	// /**
	// * handle moving right a column
	// */
	// abstract void moveRight(int x, int y, int a);

	// /**
	// * handle moving up a row
	// */
	// abstract void moveUp(int x, int y, int a);

	abstract void travelAction(final int x, final int y, final int dx, final int dy, final Action direction);

	void moveDown(final int x, final int y, final int a) {
		switch (a) {
			case 0:
			case 360:
			case -360:
				travelAction(x, y, x, y + 1, Action.DOWN);
				break;
			case 90:
			case -270:
				if (SimulationEnv.MODE == Mode.EVAL) {
					rotate(-90);
				} else {
					setPose(getX(), getY(), getHeading() + -90);
				}
				break;
			case 180:
			case -180:
			case 270:
			case -90:
				if (SimulationEnv.MODE == Mode.EVAL) {
					rotate(90);
				} else {
					setPose(getX(), getY(), getHeading() + 90);
				}
				break;
			default:
				break;
		}
	}

	void moveLeft(final int x, final int y, final int a) {
		switch (a) {
			case 0:
			case 360:
				if (SimulationEnv.MODE == Mode.EVAL) {
					rotate(-90);
				} else {
					setPose(getX(), getY(), getHeading() + -90);
				}
				break;
			case 90:
			case -270:
			case -360:
			case 180:
			case -180:
				if (SimulationEnv.MODE == Mode.EVAL) {
					rotate(90);
				} else {
					setPose(getX(), getY(), getHeading() + 90);
				}
				break;
			case 270:
			case -90:
				travelAction(x, y, x - 1, y, Action.LEFT);
				break;
			default:
				break;
		}
	}

	void moveRight(final int x, final int y, final int a) {
		switch (a) {
			case 0:
			case -360:
			case -90:
				if (SimulationEnv.MODE == Mode.EVAL) {
					rotate(90);
				} else {
					setPose(getX(), getY(), getHeading() + 90);
				}
				break;
			case 90:
			case -270:
				travelAction(x, y, x + 1, y, Action.RIGHT);
				break;
			case 180:
			case -180:
			case 270:
			case 360:
				if (SimulationEnv.MODE == Mode.EVAL) {
					rotate(-90);
				} else {
					setPose(getX(), getY(), getHeading() + -90);
				}
				break;
			default:
				break;
		}
	}

	void moveUp(final int x, final int y, final int a) {
		switch (a) {
			case 0:
			case -360:
			case 90:
			case -270:
				if (SimulationEnv.MODE == Mode.EVAL) {
					rotate(90);
				} else {
					setPose(getX(), getY(), getHeading() + 90);
				}
				break;
			case 180:
			case -180:
				travelAction(x, y, x, y - 1, Action.UP);
				break;
			case 270:
			case -90:
			case 360:
				if (SimulationEnv.MODE == Mode.EVAL) {
					rotate(-90);
				} else {
					setPose(getX(), getY(), getHeading() + -90);
				}
				break;
			default:
				break;
		}
	}

	public void stopRobot() {
		exit = true;
	}

}
