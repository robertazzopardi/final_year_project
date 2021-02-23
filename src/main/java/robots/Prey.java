package robots;

import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

import comp329robosim.OccupancyType;
import comp329robosim.SimulatedRobot;
import simulation.SimulationEnv;

/**
 * @author rob
 *
 */
final class Prey extends RobotRunner {
	private static final int[] ALL = new int[] { 1, 2, 3, 4 };

	private static final int[] NO_DOWN = new int[] { 1, 3, 4 };

	private static final int[] NO_LEFT = new int[] { 1, 2, 4 };

	private static final int[] NO_RIGHT = new int[] { 2, 3, 4 };

	private static final int[] NO_UP = new int[] { 1, 2, 3 };

	int randomMove;

	private final RobotController controller;

	private int getRandomDirection(final int[] array) {
		final int rnd = ThreadLocalRandom.current().nextInt(0, array.length);
		return array[rnd];
	}

	public Prey(final SimulatedRobot r, final int d, final SimulationEnv env, final RobotController controller) {
		super(r, d, env);

		logger = Logger.getLogger(Prey.class.getName());

		randomMove = getRandomDirection(ALL);

		setPositionNew(getGridPosX(), getGridPosY());

		this.controller = controller;
	}

	@Override
	boolean canMove(final int x, final int y) {
		return grid[y][x].getCellType() != OccupancyType.OBSTACLE && grid[y][x].getCellType() != OccupancyType.HUNTER;
	}

	@Override
	void moveDown(final int x, final int y, final int a) {
		switch (a) {
			case 0:
			case 360:
			case -360:
				travelAction(x, y, x, y + 1);
				break;
			case 90:
			case -270:
				rotate(-90);
				break;
			case 180:
			case -180:
				// rotate(90);
				// break;
			case 270:
			case -90:
				rotate(90);
				break;
			default:
				break;
		}
	}

	@Override
	void moveLeft(final int x, final int y, final int a) {
		switch (a) {
			// case -360:
			// rotate(90);
			// break;
			case 0:
			case 360:
				rotate(-90);
				break;
			case 90:
			case -270:
				// rotate(90);
				// break;
			case 180:
			case -180:
			case -360:
				rotate(90);
				break;
			case 270:
			case -90:
				travelAction(x, y, x - 1, y);
				break;
			default:
				break;
		}
	}

	@Override
	void moveRight(final int x, final int y, final int a) {
		switch (a) {
			// case 360:
			// rotate(-90);
			// break;
			case 0:
			case -360:
			case -90:
				rotate(90);
				break;
			case 90:
			case -270:
				travelAction(x, y, x + 1, y);
				break;
			case 180:
			case -180:
				// rotate(-90);
				// break;
			case 270:
			case 360:
				rotate(-90);
				break;
			// case -90:
			// rotate(90);
			// break;
			default:
				break;
		}
	}

	@Override
	void moveUp(final int x, final int y, final int a) {
		switch (a) {
			// case 360:
			// rotate(-90);
			// break;
			case 0:
			case -360:
				// rotate(90);
				// break;
			case 90:
			case -270:
				rotate(90);
				break;
			case 180:
			case -180:
				travelAction(x, y, x, y - 1);
				break;
			case 270:
			case -90:
			case 360:
				rotate(-90);
				break;
			default:
				break;
		}
	}

	@Override
	public void run() {
		while (!exit) {
			// update current position
			final int x = getGridPosX();
			final int y = getGridPosY();
			final int a = getHeading();

			final boolean right = canMove(x + 1, y);
			final boolean down = canMove(x, y + 1);
			final boolean left = canMove(x - 1, y);
			final boolean up = canMove(x, y - 1);

			// check if surrounded by the hunters
			if (!right && !left && !up && !down) {
				// Do nothing while in goal state
				logger.info("trapped");

				controller.stopRobots();

				// Done with current epoch, now we can restart the simulation
				try {
					Thread.sleep(2000);
				} catch (final InterruptedException e) {
					Thread.currentThread().interrupt();
				}

				controller.restartRobots();

				env.updateTitle(env.getEpisode());

				break;
			}

			doAction(x, y, a, right, down, left, up);
		}
		logger.info("Prey Stopped");
	}

	private void doAction(final int x, final int y, final int a, final boolean right, final boolean down,
			final boolean left, final boolean up) {
		// Handle movement
		switch (randomMove) {
			case 1:
				// right
				if (right) {
					moveRight(x, y, a);
				} else {
					randomMove = getRandomDirection(NO_RIGHT);
				}
				break;
			case 2:
				// down
				if (down) {
					moveDown(x, y, a);
				} else {
					randomMove = getRandomDirection(NO_DOWN);
				}
				break;
			case 3:
				// left
				if (left) {
					moveLeft(x, y, a);
				} else {
					randomMove = getRandomDirection(NO_LEFT);
				}
				break;
			case 4:
				// up
				if (up) {
					moveUp(x, y, a);
				} else {
					randomMove = getRandomDirection(NO_UP);
				}
				break;
			default:
				break;
		}
	}

	private void setPositionNew(final int x, final int y) {
		// set position
		env.updateGridPrey(x, y);

		if (grid[y][x + 1].getCellType() != OccupancyType.OBSTACLE
				&& grid[y][x + 1].getCellType() != OccupancyType.HUNTER) {
			env.updateGridGoal(x + 1, y);
		}
		if (grid[y][x - 1].getCellType() != OccupancyType.OBSTACLE
				&& grid[y][x - 1].getCellType() != OccupancyType.HUNTER) {
			env.updateGridGoal(x - 1, y);
		}
		if (grid[y + 1][x].getCellType() != OccupancyType.OBSTACLE
				&& grid[y + 1][x].getCellType() != OccupancyType.HUNTER) {
			env.updateGridGoal(x, y + 1);
		}
		if (grid[y - 1][x].getCellType() != OccupancyType.OBSTACLE
				&& grid[y - 1][x].getCellType() != OccupancyType.HUNTER) {
			env.updateGridGoal(x, y - 1);
		}
	}

	private void setPositionOld(final int x, final int y) {
		// set previous position
		env.updateGridEmpty(x, y);

		if (grid[y][x + 1].getCellType() != OccupancyType.OBSTACLE
				&& grid[y][x + 1].getCellType() != OccupancyType.HUNTER) {
			env.updateGridEmpty(x + 1, y);
		}
		if (grid[y][x - 1].getCellType() != OccupancyType.OBSTACLE
				&& grid[y][x - 1].getCellType() != OccupancyType.HUNTER) {
			env.updateGridEmpty(x - 1, y);
		}
		if (grid[y + 1][x].getCellType() != OccupancyType.OBSTACLE
				&& grid[y + 1][x].getCellType() != OccupancyType.HUNTER) {
			env.updateGridEmpty(x, y + 1);
		}
		if (grid[y - 1][x].getCellType() != OccupancyType.OBSTACLE
				&& grid[y - 1][x].getCellType() != OccupancyType.HUNTER) {
			env.updateGridEmpty(x, y - 1);
		}
	}

	private void travelAction(final int x, final int y, final int dx, final int dy) {
		if (canMove(dx, dy)) {
			controller.resumeHunters();

			setPositionOld(x, y);
			setPositionNew(dx, dy);

			travel(350);
			randomMove = getRandomDirection(ALL);
		}
	}
}
