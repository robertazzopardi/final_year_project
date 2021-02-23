package robots;

import java.util.logging.Logger;

import comp329robosim.MyGridCell;
import comp329robosim.RobotMonitor;
import comp329robosim.SimulatedRobot;
import simulation.SimulationEnv;

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

	/**
	 * handle moving down a row
	 */
	abstract void moveDown(int x, int y, int a);

	/**
	 * handle moving left a column
	 */
	abstract void moveLeft(int x, int y, int a);

	/**
	 * handle moving right a column
	 */
	abstract void moveRight(int x, int y, int a);

	/**
	 * handle moving up a row
	 */
	abstract void moveUp(int x, int y, int a);

	public void stopRobot() {
		exit = true;
	}

}
