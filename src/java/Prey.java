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
	protected final Logger logger = Logger.getLogger("final_year_project." + Prey.class.getName());

	private static final Random RANDOM = new Random();

	public Prey(SimulatedRobot r, int d, SimulationEnv env) {
		super(r, d, env);

		int x = getEnvPosX();
		int y = getEnvPosY();

		// right
		env.updateEnvP(x + 1, y, OccupancyType.GOAL);
		// left
		env.updateEnvP(x - 1, y, OccupancyType.GOAL);
		// up
		env.updateEnvP(x, y + 1, OccupancyType.GOAL);
		// down
		env.updateEnvP(x, y - 1, OccupancyType.GOAL);
		// actual position
		env.updateEnvP(x, y, OccupancyType.PREY);

	}

	private boolean canMove(int currX, int currY, MyGridCell[][] grid) {
		return grid[currX][currY].getCellType() == OccupancyType.EMPTY
				|| grid[currX][currY].getCellType() == OccupancyType.GOAL;
	}

	private int getRandomNumber() {
		return RANDOM.nextInt((4 - 1) + 1) + 1;
	}

	private void setPreviousPosition(int currX, int currY) {
		// set previous pos to empty
		env.updateEnvP(currX, currY, OccupancyType.EMPTY);
		// right
		env.updateEnvP(currX + 1, currY, OccupancyType.EMPTY);
		// left
		env.updateEnvP(currX - 1, currY, OccupancyType.EMPTY);
		// up
		env.updateEnvP(currX, currY + 1, OccupancyType.EMPTY);
		// down
		env.updateEnvP(currX, currY - 1, OccupancyType.EMPTY);
	}

	private void moveDown(int theta, int currX, int currY) {
		if (theta == 360) {
			rotate(-180);
			travel(350);

			setPreviousPosition(currX, currY);

			setNewDownPosition(currX, currY);
		} else if (theta == 0 || theta == -360) {

			rotate(180);
			travel(350);

			setPreviousPosition(currX, currY);

			setNewDownPosition(currX, currY);

		} else if (theta == 90 || theta == -270) {

			rotate(90);
			travel(350);

			setPreviousPosition(currX, currY);

			setNewDownPosition(currX, currY);
		} else if (theta == 180 || theta == -180) {

			// move forward
			travel(350);

			setPreviousPosition(currX, currY);

			setNewDownPosition(currX, currY);

		} else if (theta == 270 || theta == -90) {

			rotate(-90);
			travel(350);

			setPreviousPosition(currX, currY);

			setNewDownPosition(currX, currY);
		} else {
			logger.info(Integer.toString(theta));
		}
	}

	private void moveLeft(int theta, int currX, int currY) {
		if (theta == -360) {
			rotate(90);
			travel(350);

			setPreviousPosition(currX, currY);

			setNewLeftPosition(currX, currY);
		} else if (theta == 0 || theta == 360) {

			rotate(-90);
			travel(350);

			setPreviousPosition(currX, currY);

			setNewLeftPosition(currX, currY);

		} else if (theta == 90 || theta == -270) {

			rotate(180);
			travel(350);

			setPreviousPosition(currX, currY);

			setNewLeftPosition(currX, currY);
		} else if (theta == 180 || theta == -180) {

			rotate(90);
			travel(350);

			setPreviousPosition(currX, currY);

			setNewLeftPosition(currX, currY);
		} else if (theta == 270 || theta == -90) {

			// move forward
			travel(350);

			setPreviousPosition(currX, currY);

			setNewLeftPosition(currX, currY);

		} else {
			logger.info(Integer.toString(theta));
		}
	}

	private void moveRight(int theta, int currX, int currY) {
		if (theta == 360) {
			rotate(-90);
			travel(350);

			setPreviousPosition(currX, currY);

			setNewRightPosition(currX, currY);
		} else if (theta == 0 || theta == -360) {

			rotate(90);
			travel(350);

			setPreviousPosition(currX, currY);

			setNewRightPosition(currX, currY);
		} else if (theta == 90 || theta == -270) {

			// move forward
			travel(350);

			setPreviousPosition(currX, currY);

			setNewRightPosition(currX, currY);

		} else if (theta == 180 || theta == -180) {

			rotate(-90);
			travel(350);

			setPreviousPosition(currX, currY);

			setNewRightPosition(currX, currY);
		} else if (theta == 270) {

			rotate(-180);
			travel(350);

			setPreviousPosition(currX, currY);

			setNewRightPosition(currX, currY);
		} else if (theta == -90) {

			rotate(180);
			travel(350);

			setPreviousPosition(currX, currY);

			setNewRightPosition(currX, currY);
		} else {
			logger.info(Integer.toString(theta));
		}
	}

	private void moveUp(int theta, int currX, int currY) {
		if (theta == 0 || theta == 360 || theta == -360) {

			// move forward
			travel(350);

			setPreviousPosition(currX, currY);

			setNewUpPosition(currX, currY);

		} else if (theta == 90 || theta == -270) {

			rotate(-90);
			travel(350);

			setPreviousPosition(currX, currY);

			setNewUpPosition(currX, currY);
		} else if (theta == 180 || theta == -180) {

			rotate(180);
			travel(350);

			setPreviousPosition(currX, currY);

			setNewUpPosition(currX, currY);
		} else if (theta == 270 || theta == -90) {

			rotate(90);
			travel(350);

			setPreviousPosition(currX, currY);

			setNewUpPosition(currX, currY);
		} else {
			logger.info(Integer.toString(theta));
		}

	}

	@Override
	public void run() {
		while (true) {

			// System.out.println(getRandomNumber());
			// travel(350);

			int randomMove = getRandomNumber();

			int currX = getEnvPosX();
			int currY = getEnvPosY();

			MyGridCell[][] grid = env.getGrid();

			int theta = getHeading();

//			System.out.println(theta);
//			logger.info(Integer.toString(theta));

			switch (randomMove) {
			case 1:
				// right
				if (canMove(currX + 1, currY, grid)) {
					// System.out.println("right");
					moveRight(theta, currX, currY);
				}

				break;

			case 2:
				// up
				if (canMove(currX, currY + 1, grid)) {
					// System.out.println("up");
					moveUp(theta, currX, currY);
				}

				break;

			case 3:
				// left
				if (canMove(currX - 1, currY, grid)) {
					// System.out.println("left");
					moveLeft(theta, currX, currY);
				}

				break;

			case 4:
				// down
				if (canMove(currX, currY - 1, grid)) {
					// System.out.println("down");
					moveDown(theta, currX, currY);
				}

				break;

			default:
				break;
			}

			// break;
			env.printGrid();
		}

		// super.run();
	}

	private void setNewDownPosition(int currX, int currY) {
		// set new position
		env.updateEnvP(currX, currY - 1, OccupancyType.PREY);

		env.updateEnvP(currX, currY - 2, OccupancyType.GOAL);
		env.updateEnvP(currX, currY, OccupancyType.GOAL);
		env.updateEnvP(currX + 1, currY - 1, OccupancyType.GOAL);
		env.updateEnvP(currX - 1, currY - 1, OccupancyType.GOAL);
	}

	private void setNewLeftPosition(int currX, int currY) {
		// set new position
		env.updateEnvP(currX - 1, currY, OccupancyType.PREY);

		env.updateEnvP(currX - 2, currY, OccupancyType.GOAL);
		env.updateEnvP(currX, currY, OccupancyType.GOAL);
		env.updateEnvP(currX - 1, currY + 1, OccupancyType.GOAL);
		env.updateEnvP(currX - 1, currY - 1, OccupancyType.GOAL);
	}

	private void setNewRightPosition(int currX, int currY) {
		// set new position
		env.updateEnvP(currX + 1, currY, OccupancyType.PREY);

		env.updateEnvP(currX + 2, currY, OccupancyType.GOAL);
		env.updateEnvP(currX, currY, OccupancyType.GOAL);
		env.updateEnvP(currX + 1, currY + 1, OccupancyType.GOAL);
		env.updateEnvP(currX + 1, currY - 1, OccupancyType.GOAL);
	}

	private void setNewUpPosition(int currX, int currY) {
		// set new position
		env.updateEnvP(currX, currY + 1, OccupancyType.PREY);

		env.updateEnvP(currX, currY + 2, OccupancyType.GOAL);
		env.updateEnvP(currX, currY, OccupancyType.GOAL);
		env.updateEnvP(currX + 1, currY + 1, OccupancyType.GOAL);
		env.updateEnvP(currX - 1, currY + 1, OccupancyType.GOAL);
	}

}
