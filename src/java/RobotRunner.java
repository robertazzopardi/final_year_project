import java.util.logging.Logger;

import comp329robosim.MyGridCell;
import comp329robosim.RobotMonitor;
import comp329robosim.SimulatedRobot;

/**
 * @author rob
 *
 */
public abstract class RobotRunner extends RobotMonitor {

	int a; // heading

	final RobotController controller;

	SimulationEnv env;

	final MyGridCell[][] grid;

	Logger logger;

	volatile boolean paused = false;

	final Object pauseLock = new Object();

	int x; // column

	int y; // row

	RobotRunner(final SimulatedRobot r, final int d, final SimulationEnv env, final RobotController controller) {
		super(r, d);

		monitorRobotStatus(false);

		setTravelSpeed(100);

		this.env = env;
		this.grid = env.getGrid();

		this.controller = controller;
	}

	abstract boolean canMove(int dx, int dy);

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

	final void logAdjacent() {
		String adjacentString = String.format("%s %s %s %s", grid[x + 1][y], grid[x][y + 1], grid[x - 1][y],
				grid[x][y - 1]);
		logger.info(adjacentString);
	}

	/**
	 * handle moving down a row
	 */
	abstract void moveDown();

	/**
	 * handle moving left a column
	 */
	abstract void moveLeft();

	/**
	 * handle moving right a column
	 */
	abstract void moveRight();

	/**
	 * handle moving up a row
	 */
	abstract void moveUp();

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

// sets some rules for the robots movements
// private void handleRobotMovement() {
// thisHunter.setTravelSpeed(100); // 10cm per sec myRobot.setDirection(0);
//
//// thisHunter.rotate(-90);
//
// System.out.println(thisHunter.getUSenseRange());
//
//// System.out.println(thisHunter.getCSenseColor() == Color.WHITE);
//
// while (true) {
//
//// Color colourColor = thisHunter.getCSenseColor();
//// System.out.println(colourColor);
//
// // range of next object
// int sensedRange = thisHunter.getUSenseRange();
// System.out.println(sensedRange);
////
//// thisHunter.rotate(getRandomRotation());
////
//// if (sensedRange < 350) {
//// thisHunter.rotate(getRandomRotation());
//// } else {
//// thisHunter.travel(350);
//// }
//
// if (sensedRange > 180) {
// thisHunter.travel(350);
// } else {
// thisHunter.rotate(getRandomRotationSingle());
// }
//
// }
//
//// while (!robot.isBumperPressed()) {
//// robot.travel(350); // travel 35 cm
//// if (robot.getCSenseColor().equals(Color.GREEN)) {
//// System.out.println("found Green cell at location (" + robot.getX() + ","
//// + robot.getY() + ") with heading " + robot.getHeading());
//// }
//// }
// }
