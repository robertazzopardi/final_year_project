import java.util.logging.Logger;

import comp329robosim.MyGridCell;
import comp329robosim.RobotMonitor;
import comp329robosim.SimulatedRobot;

/**
 * @author rob
 *
 */
public abstract class RobotRunner extends RobotMonitor {

	final RobotController controller;

	final SimulationEnv env;

	final MyGridCell[][] grid;

	Logger logger;

	boolean paused = false;

	final Object pauseLock = new Object();

	RobotRunner(final SimulatedRobot r, final int d, final SimulationEnv env, final RobotController controller) {
		super(r, d);

		monitorRobotStatus(false);

		setTravelSpeed(100);

		this.env = env;

		this.grid = env.getGrid();

		this.controller = controller;
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

	public final boolean isPaused() {
		return paused;
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

	final void pauseRobot() {
		paused = true;
	}

	final void resumeRobot() {
		synchronized (pauseLock) {
			paused = false;
			pauseLock.notifyAll();
		}
	}
}
