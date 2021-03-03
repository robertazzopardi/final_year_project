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
	static final int CELL_DISTANCE = 350;

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

	public void stopRobot() {
		exit = true;
	}

	static final float normalise(final int x, final int min, final int max) {
		return (2 * ((float) (x - min) / (max - min))) - 1;
	}

	abstract void left(final int x, final int y);

	abstract void down(final int x, final int y);

	abstract void right(final int x, final int y);

	abstract void up(final int x, final int y);

	final void travel() {
		final int degrees = getHeading() % 360;
		switch (degrees) {
			case 0:
				down(getGridPosX(), getGridPosY());
				break;

			case 90:
			case -270:
				right(getGridPosX(), getGridPosY());
				break;

			case 180:
			case -180:
				up(getGridPosX(), getGridPosY());
				break;

			case 270:
			case -90:
				left(getGridPosX(), getGridPosY());
				break;

			default:
				System.out.println(degrees);
				break;
		}
	}

}
