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
//		env.updateEnv(x + 1, y, OccupancyType.GOAL);
//		// left
//		env.updateEnv(x - 1, y, OccupancyType.GOAL);
//		// down
//		env.updateEnv(x, y + 1, OccupancyType.GOAL);
//		// up
//		env.updateEnv(x, y - 1, OccupancyType.GOAL);
		// actual position
		env.updateEnv(x, y, OccupancyType.PREY);

	}

	private boolean canMove(int dx, int dy) {
		MyGridCell[][] grid = env.getGrid();
		return grid[dx][dy].getCellType() != OccupancyType.HUNTER
				&& grid[dx][dy].getCellType() != OccupancyType.OBSTACLE;// || grid[dx][dy].getCellType() ==
		// OccupancyType.GOAL;
	}

	private int getRandomNumber() {
		return RANDOM.nextInt((4 - 1) + 1) + 1;
	}

	private void moveDown() {
		if (a == 0 || a == 360 || a == -360) {

			// move forward

			if (canMove(x, y + 1)) {
				env.setPreviousPositionDown(x, y);
				travel(350);
			}

		} else if (a == 90 || a == -270) {

			rotate(-90);

			if (canMove(x, y + 1)) {
				env.setPreviousPositionDown(x, y);
				travel(350);
			}
		} else if (a == 180 || a == -180) {

			rotate(180);

			if (canMove(x, y + 1)) {
				env.setPreviousPositionDown(x, y);
				travel(350);
			}
		} else if (a == 270 || a == -90) {

			rotate(90);

			if (canMove(x, y + 1)) {
				env.setPreviousPositionDown(x, y);
				travel(350);
			}
		} else {
			logger.info(Integer.toString(a));
		}

	}

	private void moveLeft() {
		if (a == -360) {
			rotate(90);

			if (canMove(x - 1, y)) {
				env.setPreviousPositionLeft(x, y);
				travel(350);
			}
		} else if (a == 0 || a == 360) {

			rotate(-90);

			if (canMove(x - 1, y)) {
				env.setPreviousPositionLeft(x, y);
				travel(350);
			}
		} else if (a == 90 || a == -270) {

			rotate(180);

			if (canMove(x - 1, y)) {
				env.setPreviousPositionLeft(x, y);
				travel(350);
			}
		} else if (a == 180 || a == -180) {

			rotate(90);

			if (canMove(x - 1, y)) {
				env.setPreviousPositionLeft(x, y);
				travel(350);
			}
		} else if (a == 270 || a == -90) {

			// move forward

			if (canMove(x - 1, y)) {
				env.setPreviousPositionLeft(x, y);
				travel(350);
			}

		} else {
			logger.info(Integer.toString(a));
		}
	}

	private void moveRight() {
		if (a == 360) {
			rotate(-90);

			if (canMove(x + 1, y)) {
				env.setPreviousPositionRight(x, y);
				travel(350);
			}
		} else if (a == 0 || a == -360) {

			rotate(90);

			if (canMove(x + 1, y)) {
				env.setPreviousPositionRight(x, y);
				travel(350);
			}
		} else if (a == 90 || a == -270) {

			// move forward

			if (canMove(x + 1, y)) {
				env.setPreviousPositionRight(x, y);
				travel(350);
			}
		} else if (a == 180 || a == -180) {

			rotate(-90);

			if (canMove(x + 1, y)) {
				env.setPreviousPositionRight(x, y);
				travel(350);
			}
		} else if (a == 270) {

			rotate(-180);

			if (canMove(x + 1, y)) {
				env.setPreviousPositionRight(x, y);
				travel(350);
			}
		} else if (a == -90) {

			rotate(180);

			if (canMove(x + 1, y)) {
				env.setPreviousPositionRight(x, y);
				travel(350);
			}
		} else {
			logger.info(Integer.toString(a));
		}
	}

	private void moveUp() {
		if (a == 360) {
			rotate(-180);

			if (canMove(x, y - 1)) {
				env.setPreviousPositionUp(x, y);
				travel(350);
			}
		} else if (a == 0 || a == -360) {

			rotate(180);

			if (canMove(x, y - 1)) {
				env.setPreviousPositionUp(x, y);
				travel(350);
			}

		} else if (a == 90 || a == -270) {

			rotate(90);

			if (canMove(x, y - 1)) {
				env.setPreviousPositionUp(x, y);
				travel(350);
			}
		} else if (a == 180 || a == -180) {

			// move forward

			if (canMove(x, y - 1)) {
				env.setPreviousPositionUp(x, y);
				travel(350);
			}

		} else if (a == 270 || a == -90) {

			rotate(-90);

			if (canMove(x, y - 1)) {
				env.setPreviousPositionUp(x, y);
				travel(350);
			}
		} else {
			logger.info(Integer.toString(a));
		}
	}

	@Override
	public void run() {
		while (true) {
			int randomMove = getRandomNumber();

			x = getEnvPosX();
			y = getEnvPosY();

			a = getHeading();

			// check if in a goal state

			boolean right = canMove(x + 1, y);
			boolean down = canMove(x, y + 1);
			boolean left = canMove(x - 1, y);
			boolean up = canMove(x, y - 1);

			///
			if (!right && !left && !up && !down) {
				// Do nothing while in goal state

				logger.info("trapped");

				// break;
				pauseRobot();

			}

			checkWaitingStatus();

			///

			switch (randomMove) {
			case 1:
				// right
				if (right) {
					moveRight();
					env.resumeHunters();
				}
				break;
			case 2:
				// down
				if (down) {
					moveDown();
					env.resumeHunters();
				}
				break;
			case 3:
				// left
				if (left) {
					moveLeft();
					env.resumeHunters();
				}
				break;
			case 4:
				// up
				if (up) {
					moveUp();
					env.resumeHunters();
				}
				break;
			default:
				break;
			}

			env.printGrid(logger);

		}

		// super.run();
	}

}
