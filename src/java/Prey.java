import java.util.Random;
import java.util.logging.Logger;

import comp329robosim.MyGridCell;
import comp329robosim.OccupancyType;
import comp329robosim.SimulatedRobot;

/**
 * @author rob
 *
 */
public class Prey extends RobotRunner {
	private static final Random RANDOM = new Random();

	protected final Logger logger = Logger.getLogger("final_year_project." + Prey.class.getName());

	public Prey(SimulatedRobot r, int d, SimulationEnv env) {
		super(r, d, env);

		int x = getEnvPosX();
		int y = getEnvPosY();

		// right
		env.updateEnv(x + 1, y, OccupancyType.GOAL);
		// left
		env.updateEnv(x - 1, y, OccupancyType.GOAL);
		// down
		env.updateEnv(x, y + 1, OccupancyType.GOAL);
		// up
		env.updateEnv(x, y - 1, OccupancyType.GOAL);
		// actual position
		env.updateEnv(x, y, OccupancyType.PREY);

	}

	private boolean canMove(int x, int y, MyGridCell[][] grid) {
		return grid[x][y].getCellType() == OccupancyType.EMPTY || grid[x][y].getCellType() == OccupancyType.GOAL;

//		return grid[x][y].getCellType() != OccupancyType.HUNTER
//				&& grid[x][y].getCellType() != OccupancyType.OBSTACLE;
	}

	private int getRandomNumber() {
		return RANDOM.nextInt((4 - 1) + 1) + 1;
	}

	private void moveDown() {
		if (a == 0 || a == 360 || a == -360) {

			// move forward
//			travel(350);

//			env.setPreviousPosition(x, y);
//
//			setNewDownPosition();
			env.setPreviousPositionDown(x, y);

			travel(350);

		} else if (a == 90 || a == -270) {

			rotate(-90);
//			travel(350);

//			env.setPreviousPosition(x, y);
//
//			setNewDownPosition();
			env.setPreviousPositionDown(x, y);

			travel(350);
		} else if (a == 180 || a == -180) {

			rotate(180);
//			travel(350);

//			env.setPreviousPosition(x, y);
//
//			setNewDownPosition();
			env.setPreviousPositionDown(x, y);

			travel(350);
		} else if (a == 270 || a == -90) {

			rotate(90);
//			travel(350);

//			env.setPreviousPosition(x, y);
//
//			setNewDownPosition();
			env.setPreviousPositionDown(x, y);

			travel(350);
		} else {
			logger.info(Integer.toString(a));
		}

	}

	private void moveLeft() {
		if (a == -360) {
			rotate(90);
//			travel(350);

//			env.setPreviousPosition(x, y);
//
//			setNewLeftPosition();
			env.setPreviousPositionLeft(x, y);

			travel(350);
		} else if (a == 0 || a == 360) {

			rotate(-90);
//			travel(350);

//			env.setPreviousPosition(x, y);
//
//			setNewLeftPosition();
			env.setPreviousPositionLeft(x, y);

			travel(350);
		} else if (a == 90 || a == -270) {

			rotate(180);
//			travel(350);

//			env.setPreviousPosition(x, y);
//
//			setNewLeftPosition();
			env.setPreviousPositionLeft(x, y);

			travel(350);
		} else if (a == 180 || a == -180) {

			rotate(90);
//			travel(350);

//			env.setPreviousPosition(x, y);
//
//			setNewLeftPosition();
			env.setPreviousPositionLeft(x, y);

			travel(350);
		} else if (a == 270 || a == -90) {

			// move forward
//			travel(350);

//			env.setPreviousPosition(x, y);
//
//			setNewLeftPosition();
			env.setPreviousPositionLeft(x, y);

			travel(350);

		} else {
			logger.info(Integer.toString(a));
		}
	}

	private void moveRight() {
		if (a == 360) {
			rotate(-90);
//			travel(350);

//			env.setPreviousPosition(x, y);
//
//			setNewRightPosition();
			env.setPreviousPositionRight(x, y);

			travel(350);
		} else if (a == 0 || a == -360) {

			rotate(90);
//			travel(350);

//			env.setPreviousPosition(x, y);
//
//			setNewRightPosition();
			env.setPreviousPositionRight(x, y);

			travel(350);
		} else if (a == 90 || a == -270) {

			// move forward
//			travel(350);

//			env.setPreviousPosition(x, y);
//
//			setNewRightPosition();
			env.setPreviousPositionRight(x, y);

			travel(350);
		} else if (a == 180 || a == -180) {

			rotate(-90);
//			travel(350);

//			env.setPreviousPosition(x, y);
//
//			setNewRightPosition();
			env.setPreviousPositionRight(x, y);

			travel(350);
		} else if (a == 270) {

			rotate(-180);
//			travel(350);

//			env.setPreviousPosition(x, y);
//
//			setNewRightPosition();
			env.setPreviousPositionRight(x, y);

			travel(350);
		} else if (a == -90) {

			rotate(180);
//			travel(350);

//			env.setPreviousPosition(x, y);
//
//			setNewRightPosition();
			env.setPreviousPositionRight(x, y);

			travel(350);
		} else {
			logger.info(Integer.toString(a));
		}
	}

	private void moveUp() {
		if (a == 360) {
			rotate(-180);
//			travel(350);

//			env.setPreviousPosition(x, y);
//
//			setNewUpPosition();
			env.setPreviousPositionUp(x, y);

			travel(350);
		} else if (a == 0 || a == -360) {

			rotate(180);
//			travel(350);

//			env.setPreviousPosition(x, y);
//
//			setNewUpPosition();
			env.setPreviousPositionUp(x, y);

			travel(350);

		} else if (a == 90 || a == -270) {

			rotate(90);
//			travel(350);

//			env.setPreviousPosition(x, y);
//
//			setNewUpPosition();
			env.setPreviousPositionUp(x, y);

			travel(350);
		} else if (a == 180 || a == -180) {

			// move forward
//			travel(350);

//			env.setPreviousPosition(x, y);
//
//			setNewUpPosition();
			env.setPreviousPositionUp(x, y);

			travel(350);

		} else if (a == 270 || a == -90) {

			rotate(-90);
//			travel(350);

//			env.setPreviousPosition(x, y);
//
//			setNewUpPosition();
			env.setPreviousPositionUp(x, y);

			travel(350);
		} else {
			logger.info(Integer.toString(a));
		}
	}

	@Override
	public void run() {
		while (true) {

			// travel(350);

			int randomMove = getRandomNumber();

			x = getEnvPosX();
			y = getEnvPosY();

			MyGridCell[][] grid = env.getGrid();

			a = getHeading();

//			logger.info(Integer.toString(a));

			switch (randomMove) {
			case 1:
				// right
				if (canMove(x + 1, y, grid)) {
					// System.out.println("right");
					moveRight();

					env.resumeHunters();
				}

				break;

			case 2:
				// down
				if (canMove(x, y + 1, grid)) {
					// System.out.println("up");
					moveDown();

					env.resumeHunters();
				}

				break;

			case 3:
				// left
				if (canMove(x - 1, y, grid)) {
					// System.out.println("left");
					moveLeft();

					env.resumeHunters();
				}

				break;

			case 4:
				// up
				if (canMove(x, y - 1, grid)) {
					// System.out.println("down");
					moveUp();

					env.resumeHunters();
				}

				break;

			default:
				break;
			}

			// break;
			env.printGrid(logger);

//			env.resumeHunters();
		}

		// super.run();
	}

//	private void setNewDownPosition() {
//		// set new position
//		env.updateEnv(x, y + 1, OccupancyType.PREY);
//
//		env.updateEnv(x, y + 2, OccupancyType.GOAL);
//		env.updateEnv(x, y, OccupancyType.GOAL);
//		env.updateEnv(x + 1, y + 1, OccupancyType.GOAL);
//		env.updateEnv(x - 1, y + 1, OccupancyType.GOAL);
//	}

//	private void setNewLeftPosition() {
//		// set new position
//		env.updateEnv(x - 1, y, OccupancyType.PREY);
//
//		env.updateEnv(x - 2, y, OccupancyType.GOAL);
//		env.updateEnv(x, y, OccupancyType.GOAL);
//		env.updateEnv(x - 1, y + 1, OccupancyType.GOAL);
//		env.updateEnv(x - 1, y - 1, OccupancyType.GOAL);
//	}

//	private void setNewRightPosition() {
//		// set new position
//		env.updateEnv(x + 1, y, OccupancyType.PREY);
//
//		env.updateEnv(x + 2, y, OccupancyType.GOAL);
//		env.updateEnv(x, y, OccupancyType.GOAL);
//		env.updateEnv(x + 1, y + 1, OccupancyType.GOAL);
//		env.updateEnv(x + 1, y - 1, OccupancyType.GOAL);
//	}

//	private void setNewUpPosition() {
//		// set new position
//		env.updateEnv(x, y - 1, OccupancyType.PREY);
//
//		env.updateEnv(x, y - 2, OccupancyType.GOAL);
//		env.updateEnv(x, y, OccupancyType.GOAL);
//		env.updateEnv(x + 1, y - 1, OccupancyType.GOAL);
//		env.updateEnv(x - 1, y - 1, OccupancyType.GOAL);
//	}

}
