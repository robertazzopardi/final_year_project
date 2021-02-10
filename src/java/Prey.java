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

	public static int getRandomDirection(final int[] array) {
		final int rnd = RANDOM.nextInt(array.length);
		return array[rnd];
	}

	int randomMove = getRandomDirection(ALL);

	public Prey(final SimulatedRobot r, final int d, final SimulationEnv env, final RobotController controller) {
		super(r, d, env, controller);

		logger = Logger.getLogger("final_year_project." + Prey.class.getName());

		setPositionNew(getGridPosX(), getGridPosY());

		logger.info(getGridPosX() + " " + getGridPosY());
	}

	@Override
	boolean canMove(final int dx, final int dy) {
		return grid[dx][dy].getCellType() != OccupancyType.OBSTACLE;
	}

	@Override
	void moveDown() {
		switch (a) {
		case 0:
		case 360:
		case -360:
			if (canMove(x, y + 1)) {
				setPositionOld(x, y);
				setPositionNew(x, y + 1);

				travel(350);
				randomMove = getRandomDirection(ALL);
				controller.resumeHunters();
			}
			break;
		case 90:
		case -270:
			rotate(-90);
			break;
		case 180:
		case -180:
			rotate(90);
			break;
		case 270:
		case -90:
			rotate(90);
			break;
		default:
			break;
		}
	}

	@Override
	void moveLeft() {
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
			rotate(90);
			break;
		case 180:
		case -180:
			rotate(90);
			break;
		case 270:
		case -90:
			if (canMove(x - 1, y)) {
				setPositionOld(x, y);
				setPositionNew(x - 1, y);

				travel(350);
				randomMove = getRandomDirection(ALL);
				controller.resumeHunters();
			}
			break;
		default:
			break;
		}
	}

	@Override
	void moveRight() {
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
				setPositionOld(x, y);
				setPositionNew(x + 1, y);

				travel(350);
				randomMove = getRandomDirection(ALL);
				controller.resumeHunters();
			}
			break;
		case 180:
		case -180:
			rotate(-90);
			break;
		case 270:
			rotate(-90);
			break;
		case -90:
			rotate(90);
			break;
		default:
			break;
		}
	}

	@Override
	void moveUp() {
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
			rotate(90);
			break;
		case 180:
		case -180:
			if (canMove(x, y - 1)) {
				setPositionOld(x, y);
				setPositionNew(x, y - 1);

				travel(350);
				randomMove = getRandomDirection(ALL);
				controller.resumeHunters();
			}
			break;
		case 270:
		case -90:
			rotate(-90);
			break;
		default:
			break;
		}
	}

	@Override
	public void run() {
		while (true) {

			// update current position
			x = getGridPosX();
			y = getGridPosY();
			a = getHeading();

			// check if can move into adjacent spaces
			final boolean right = grid[x + 1][y].getCellType() == OccupancyType.GOAL
					&& grid[x + 1][y].getCellType() != OccupancyType.HUNTER;

			final boolean down = grid[x][y + 1].getCellType() == OccupancyType.GOAL
					&& grid[x][y + 1].getCellType() != OccupancyType.HUNTER;

			final boolean left = grid[x - 1][y].getCellType() == OccupancyType.GOAL
					&& grid[x - 1][y].getCellType() != OccupancyType.HUNTER;

			final boolean up = grid[x][y - 1].getCellType() == OccupancyType.GOAL
					&& grid[x][y - 1].getCellType() != OccupancyType.HUNTER;

			logAdjacent();

			// check if surrounded by the hunters
			if (!right && !left && !up && !down) {
				// Do nothing while in goal state
				logger.info("trapped");
				pauseRobot();
			}

			// check if paused
			synchronized (pauseLock) {
				if (paused) {
					try {
						pauseLock.wait();
					} catch (final InterruptedException ex) {
						ex.printStackTrace();
					}
				}
			}

			// Handle movement
			switch (randomMove) {
			case 1:
				// right
				if (right) {
					moveRight();
					controller.resumeHunters();
				} else {
					randomMove = getRandomDirection(NO_RIGHT);
				}
				break;
			case 2:
				// down
				if (down) {
					moveDown();
					controller.resumeHunters();
				} else {
					randomMove = getRandomDirection(NO_DOWN);
				}
				break;
			case 3:
				// left
				if (left) {
					moveLeft();
					controller.resumeHunters();
				} else {
					randomMove = getRandomDirection(NO_LEFT);
				}
				break;
			case 4:
				// up
				if (up) {
					moveUp();
					controller.resumeHunters();
				} else {
					randomMove = getRandomDirection(NO_UP);
				}
				break;
			default:
				break;
			}
		}
	}

	private void setPositionOld(int dx, int dy) {
		// set position
		env.updateEnv(dx, dy, OccupancyType.EMPTY);

		if (grid[dx + 1][dy].getCellType() != OccupancyType.OBSTACLE) {
			env.updateEnv(dx + 1, dy, OccupancyType.EMPTY);
		}
		if (grid[dx - 1][dy].getCellType() != OccupancyType.OBSTACLE) {
			env.updateEnv(dx - 1, dy, OccupancyType.EMPTY);
		}
		if (grid[dx][dy + 1].getCellType() != OccupancyType.OBSTACLE) {
			env.updateEnv(dx, dy + 1, OccupancyType.EMPTY);
		}
		if (grid[dx][dy - 1].getCellType() != OccupancyType.OBSTACLE) {
			env.updateEnv(dx, dy - 1, OccupancyType.EMPTY);
		}
	}

	private void setPositionNew(int dx, int dy) {
		// set position
		env.updateEnv(dx, dy, OccupancyType.PREY);

		if (grid[dx + 1][dy].getCellType() != OccupancyType.OBSTACLE) {
			env.updateEnv(dx + 1, dy, OccupancyType.GOAL);
		}
		if (grid[dx - 1][dy].getCellType() != OccupancyType.OBSTACLE) {
			env.updateEnv(dx - 1, dy, OccupancyType.GOAL);
		}
		if (grid[dx][dy + 1].getCellType() != OccupancyType.OBSTACLE) {
			env.updateEnv(dx, dy + 1, OccupancyType.GOAL);
		}
		if (grid[dx][dy - 1].getCellType() != OccupancyType.OBSTACLE) {
			env.updateEnv(dx, dy - 1, OccupancyType.GOAL);
		}

	}

}
