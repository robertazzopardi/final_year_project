import java.util.Random;
import java.util.logging.Logger;

import comp329robosim.OccupancyType;
import comp329robosim.SimulatedRobot;

/**
 * @author rob
 *
 */
public class Prey extends RobotRunner {
	private static final int[] ALL = new int[] { 1, 2, 3, 4 };

	private static final int[] NO_DOWN = new int[] { 1, 3, 4 };

	private static final int[] NO_LEFT = new int[] { 1, 2, 4 };

	private static final int[] NO_RIGHT = new int[] { 2, 3, 4 };

	private static final int[] NO_UP = new int[] { 1, 2, 3 };

	private static final Random RANDOM = new Random();

	public static int getRandom(final int[] array) {
		final int rnd = RANDOM.nextInt(array.length);
		return array[rnd];
	}

	int randomMove = getRandom(ALL);

	public Prey(final SimulatedRobot r, final int d, final SimulationEnv env, final RobotController controller) {
		super(r, d, env, controller);

		logger = Logger.getLogger("final_year_project." + Prey.class.getName());

		setPositionFilled(getEnvPosX(), getEnvPosY());
	}

	private void setPositionFilled(int dx, int dy) {

		// set position
		env.updateEnv(dx, dy, OccupancyType.PREY);

		if (grid[dx + 1][dy].getCellType() != OccupancyType.OBSTACLE)
			env.updateEnv(dx + 1, dy, OccupancyType.GOAL);
		if (grid[dx - 1][dy].getCellType() != OccupancyType.OBSTACLE)
			env.updateEnv(dx - 1, dy, OccupancyType.GOAL);
		if (grid[dx][dy + 1].getCellType() != OccupancyType.OBSTACLE)
			env.updateEnv(dx, dy + 1, OccupancyType.GOAL);
		if (grid[dx][dy - 1].getCellType() != OccupancyType.OBSTACLE)
			env.updateEnv(dx, dy - 1, OccupancyType.GOAL);

	}

	private void setPositionEmpty(int dx, int dy) {

		// set position
		env.updateEnv(dx, dy, OccupancyType.EMPTY);

		if (grid[dx + 1][dy].getCellType() != OccupancyType.OBSTACLE)
			env.updateEnv(dx + 1, dy, OccupancyType.EMPTY);
		if (grid[dx - 1][dy].getCellType() != OccupancyType.OBSTACLE)
			env.updateEnv(dx - 1, dy, OccupancyType.EMPTY);
		if (grid[dx][dy + 1].getCellType() != OccupancyType.OBSTACLE)
			env.updateEnv(dx, dy + 1, OccupancyType.EMPTY);
		if (grid[dx][dy - 1].getCellType() != OccupancyType.OBSTACLE)
			env.updateEnv(dx, dy - 1, OccupancyType.EMPTY);
	}

	@Override
	protected boolean canMove(final int dx, final int dy) {
		return grid[dx][dy].getCellType() != OccupancyType.OBSTACLE;
	}

	private void moveDown() {
		switch (a) {
		case 0:
		case 360:
		case -360:
			if (canMove(x, y + 1)) {
//				env.updateEnvOldNew(x, y + 1, x, y);

				setPositionEmpty(x, y);
				setPositionFilled(x, y + 1);

				travel(350);
				randomMove = getRandom(ALL);
				controller.resumeHunters();
			}
			break;
		case 90:
		case -270:
			rotate(-90);
			break;
		case 180:
		case -180:
			// rotate(180);
			rotate(90);
			break;
		case 270:
		case -90:
			rotate(90);
			break;
		default:
			logger.info(Integer.toString(a));
			break;
		}
	}

	private void moveLeft() {
		switch (a) {
		case -360:
			rotate(90);
			break;
		case 0:
		case 360:
			rotate(-90);
			break;
		case 90:
		case -270:
			// rotate(180);
			rotate(90);
			break;
		case 180:
		case -180:
			rotate(90);
			break;
		case 270:
		case -90:
			if (canMove(x - 1, y)) {
//				env.updateEnvOldNew(x - 1, y, x, y);

				setPositionEmpty(x, y);
				setPositionFilled(x - 1, y);

				travel(350);
				randomMove = getRandom(ALL);
				controller.resumeHunters();
			}
			break;
		default:
			logger.info(Integer.toString(a));
			break;
		}
	}

	private void moveRight() {
		switch (a) {
		case 360:
			rotate(-90);
			break;
		case 0:
		case -360:
			rotate(90);
			break;
		case 90:
		case -270:
			if (canMove(x + 1, y)) {
//				env.updateEnvOldNew(x + 1, y, x, y);

				setPositionEmpty(x, y);
				setPositionFilled(x + 1, y);

				travel(350);
				randomMove = getRandom(ALL);
				controller.resumeHunters();
			}
			break;
		case 180:
		case -180:
			rotate(-90);
			break;
		case 270:
			// rotate(-180);
			rotate(-90);
			break;
		case -90:
			// rotate(180);
			rotate(90);
			break;
		default:
			logger.info(Integer.toString(a));
			break;
		}
	}

	private void moveUp() {
		switch (a) {
		case 360:
			// rotate(-180);
			rotate(-90);
			break;
		case 0:
		case -360:
			// rotate(180);
			rotate(90);
			break;
		case 90:
		case -270:
			rotate(90);
			break;
		case 180:
		case -180:
			if (canMove(x, y - 1)) {
//				env.updateEnvOldNew(x, y - 1, x, y);

				setPositionEmpty(x, y);
				setPositionFilled(x, y - 1);

				travel(350);
				randomMove = getRandom(ALL);
				controller.resumeHunters();
			}
			break;
		case 270:
		case -90:
			rotate(-90);
			break;
		default:
			logger.info(Integer.toString(a));
			break;
		}
	}

	@Override
	public void run() {
		while (true) {

			x = getEnvPosX();
			y = getEnvPosY();
			a = getHeading();

			// check if in a goal state

			// boolean right = canMove(x + 1, y);
			// boolean down = canMove(x, y + 1);
			// boolean left = canMove(x - 1, y);
			// boolean up = canMove(x, y - 1);

//			final MyGridCell[][] grid = env.getGrid();

			final boolean right = grid[x + 1][y].getCellType() == OccupancyType.GOAL
					&& grid[x + 1][y].getCellType() != OccupancyType.HUNTER;
			final boolean down = grid[x][y + 1].getCellType() == OccupancyType.GOAL
					&& grid[x + 1][y].getCellType() != OccupancyType.HUNTER;
			final boolean left = grid[x - 1][y].getCellType() == OccupancyType.GOAL
					&& grid[x + 1][y].getCellType() != OccupancyType.HUNTER;
			final boolean up = grid[x][y - 1].getCellType() == OccupancyType.GOAL
					&& grid[x + 1][y].getCellType() != OccupancyType.HUNTER;

			logger.info(grid[x + 1][y] + " " + grid[x][y + 1] + " " + grid[x - 1][y] + " " + grid[x][y - 1]);

			///
			if (!right && !left && !up && !down) {
				// Do nothing while in goal state

				logger.info("trapped");

				// break;
				pauseRobot();

			}

			/////
			// checkWaitingStatus();

			synchronized (pauseLock) {
				if (paused) {
					try {
						// synchronized (pauseLock) {
						pauseLock.wait();
						// }
					} catch (final InterruptedException ex) {
						// break;
						ex.printStackTrace();
					}
				}
			}

			/////

			switch (randomMove) {
			case 1:
				// right
				if (right) {
					moveRight();
					controller.resumeHunters();
				} else {
					randomMove = getRandom(NO_RIGHT);
				}
				break;
			case 2:
				// down
				if (down) {
					moveDown();
					controller.resumeHunters();
				} else {
					randomMove = getRandom(NO_DOWN);
				}
				break;
			case 3:
				// left
				if (left) {
					moveLeft();
					controller.resumeHunters();
				} else {
					randomMove = getRandom(NO_LEFT);
				}
				break;
			case 4:
				// up
				if (up) {
					moveUp();
					controller.resumeHunters();
				} else {
					randomMove = getRandom(NO_UP);
				}
				break;
			default:
				break;
			}

//			env.printGrid(logger);

		}

		// super.run();
	}

}
