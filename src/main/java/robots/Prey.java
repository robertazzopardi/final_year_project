package robots;

import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

import comp329robosim.OccupancyType;
import comp329robosim.SimulatedRobot;
import simulation.GridPrinter;
import simulation.SimulationEnv;
import simulation.SimulationEnv.Mode;

/**
 * @author rob
 *
 */
final class Prey extends RobotRunner {
	private static final Action[] ALL = new Action[] { Action.RIGHT_TURN, Action.NOTHING, Action.LEFT_TURN,
			Action.TRAVEL };

	// private static final Action[] NO_DOWN = new Action[] { Action.RIGHT,
	// Action.LEFT, Action.UP };

	// private static final Action[] NO_LEFT = new Action[] { Action.RIGHT,
	// Action.DOWN, Action.UP };

	// private static final Action[] NO_RIGHT = new Action[] { Action.DOWN,
	// Action.LEFT, Action.UP };

	// private static final Action[] NO_UP = new Action[] { Action.RIGHT,
	// Action.DOWN, Action.LEFT };

	private Action randomMove;

	private final RobotController controller;

	private Action getRandomDirection(final Action[] array) {
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
		// return grid[y][x].getCellType() != OccupancyType.OBSTACLE &&
		// grid[y][x].getCellType() != OccupancyType.HUNTER;
		return grid[y][x].getCellType() == OccupancyType.GOAL || grid[y][x].getCellType() == OccupancyType.EMPTY;
	}

	@Override
	public void run() {
		while (!exit) {
			final int x = getGridPosX();
			final int y = getGridPosY();
			// final int a = getHeading();

			final boolean right = canMove(x + 1, y);
			final boolean down = canMove(x, y + 1);
			final boolean left = canMove(x - 1, y);
			final boolean up = canMove(x, y - 1);

			// check if surrounded by the hunters
			if (!right && !left && !up && !down) {

				// Do nothing while in goal state
				logger.info("trapped");

				controller.stopRobots();

				GridPrinter.printGrid(grid);

				// Done with current epoch, now we can restart the simulation
				try {
					Thread.sleep(2000);
				} catch (final InterruptedException e) {
					Thread.currentThread().interrupt();
				}

				env.resetGrid();

				controller.restartRobots();

				env.updateTitle(env.getEpisode());

				break;
			}

			randomMove = Action.getRandomAction();

			// doAction(x, y, a, right, down, left, up);

			doAction(randomMove);
		}
		logger.info("Prey Stopped");
	}

	private void doAction(final Action direction) {
		switch (direction) {
			case TRAVEL:
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
				break;

			case LEFT_TURN:
				// rotate(-90);
				if (SimulationEnv.MODE == Mode.EVAL) {
					rotate(-90);
				} else {
					setPose(getX(), getY(), getHeading() + -90);
				}
				break;

			case RIGHT_TURN:
				// rotate(90);
				if (SimulationEnv.MODE == Mode.EVAL) {
					rotate(90);
				} else {
					setPose(getX(), getY(), getHeading() + 90);
				}
				break;

			case NOTHING:
				break;

			default:
				break;
		}

	}

	private void left(final int x, final int y) {
		if (canMove(x - 1, y)) {
			controller.resumeHunters();

			setPositionOld(x, y);
			setPositionNew(x - 1, y);

			if (SimulationEnv.MODE == Mode.EVAL) {
				travel(CELL_DISTANCE);
			} else {
				setPose(getX() - CELL_DISTANCE, getY(), getHeading());
			}

			randomMove = getRandomDirection(ALL);
		}
	}

	private void up(final int x, final int y) {
		if (canMove(x, y - 1)) {
			controller.resumeHunters();

			setPositionOld(x, y);
			setPositionNew(x, y - 1);

			if (SimulationEnv.MODE == Mode.EVAL) {
				travel(CELL_DISTANCE);
			} else {
				setPose(getX(), getY() - CELL_DISTANCE, getHeading());
			}

			randomMove = getRandomDirection(ALL);
		}
	}

	private void right(final int x, final int y) {
		if (canMove(x + 1, y)) {
			controller.resumeHunters();

			setPositionOld(x, y);
			setPositionNew(x + 1, y);

			if (SimulationEnv.MODE == Mode.EVAL) {
				travel(CELL_DISTANCE);
			} else {
				setPose(getX() + CELL_DISTANCE, getY(), getHeading());
			}

			randomMove = getRandomDirection(ALL);
		}
	}

	private void down(final int x, final int y) {
		if (canMove(x, y + 1)) {
			controller.resumeHunters();

			setPositionOld(x, y);
			setPositionNew(x, y + 1);

			if (SimulationEnv.MODE == Mode.EVAL) {
				travel(CELL_DISTANCE);
			} else {
				setPose(getX(), getY() + CELL_DISTANCE, getHeading());
			}

			randomMove = getRandomDirection(ALL);
		}
	}

	// private void doAction(final int x, final int y, final int a, final boolean
	// right, final boolean down,
	// final boolean left, final boolean up) {
	// // Handle movement
	// switch (randomMove) {
	// case RIGHT_TURN:
	// // right
	// if (right) {
	// moveRight(x, y, a);
	// } else {
	// randomMove = getRandomDirection(NO_RIGHT);
	// }
	// break;
	// case DOWN:
	// // down
	// if (down) {
	// moveDown(x, y, a);
	// } else {
	// randomMove = getRandomDirection(NO_DOWN);
	// }
	// break;
	// case LEFT:
	// // left
	// if (left) {
	// moveLeft(x, y, a);
	// } else {
	// randomMove = getRandomDirection(NO_LEFT);
	// }
	// break;
	// case UP:
	// // up
	// if (up) {
	// moveUp(x, y, a);
	// } else {
	// randomMove = getRandomDirection(NO_UP);
	// }
	// break;
	// default:
	// break;
	// }
	// }

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

}
