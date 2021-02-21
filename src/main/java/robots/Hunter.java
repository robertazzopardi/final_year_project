package robots;

import java.util.logging.Level;
import java.util.logging.Logger;

import comp329robosim.OccupancyType;
import comp329robosim.SimulatedRobot;
import intelligence.Intelligence;
import simulation.SimulationEnv;

/**
 * @author rob
 *
 */
final class Hunter extends RobotRunner {

	private static int hunterCount = 1;

	public static int getHunterCount() {
		return hunterCount - 1;
	}

	private static synchronized void resetHunterCount() {
		hunterCount = 1;
	}

	private final Intelligence learning;

	private final int number;

	private volatile boolean paused = false;

	private final Object pauseLock = new Object();

	private final Hunter[] otherHunters = new Hunter[3];

	public void setOthers(final Hunter[] hunters) {
		int index = 0;
		for (int i = 0; i < hunters.length; i++) {
			// logger.info(Boolean.toString(hunters[i].equals(this)));
			if (!hunters[i].equals(this)) {
				otherHunters[index++] = hunters[i];
			}
		}
	}

	public Hunter(final SimulatedRobot r, final int d, final SimulationEnv env, final RobotController controller,
			final Intelligence learning) {
		super(r, d, env, controller);

		this.number = hunterCount++;

		this.logger = Logger.getLogger("Hunter " + number);

		env.updateGridHunter(getGridPosX(), getGridPosY());

		this.learning = learning;

	}

	@Override
	boolean canMove(final int x, final int y) {
		return grid[y][x].getCellType() != OccupancyType.OBSTACLE;
	}

	public Intelligence getLearning() {
		return learning;
	}

	private boolean isAdjacentToPrey(final int x, final int y) {
		return grid[y][x - 1].getCellType() == OccupancyType.PREY || grid[y][x + 1].getCellType() == OccupancyType.PREY
				|| grid[y - 1][x].getCellType() == OccupancyType.PREY
				|| grid[y + 1][x].getCellType() == OccupancyType.PREY;
	}

	public boolean isPaused() {
		return paused;
	}

	@Override
	void moveDown(final int x, final int y, final int a) {
		switch (a) {
			case 0:
			case 360:
			case -360:
				if (canMove(x, y + 1)) {
					env.updateGridEmpty(x, y);
					env.updateGridHunter(x, y + 1);
					travel(350);
				}
				break;
			case 90:
			case -270:
				rotate(-90);
				break;
			case 180:
			case -180:
				rotate(180);
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
	void moveLeft(final int x, final int y, final int a) {
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
				rotate(180);
				break;
			case 180:
			case -180:
				rotate(90);
				break;
			case 270:
			case -90:
				if (canMove(x - 1, y)) {
					env.updateGridEmpty(x, y);
					env.updateGridHunter(x - 1, y);
					travel(350);
				}
				break;
			default:
				break;
		}
	}

	@Override
	void moveRight(final int x, final int y, final int a) {
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
					env.updateGridEmpty(x, y);
					env.updateGridHunter(x + 1, y);
					travel(350);
				}
				break;
			case 180:
			case -180:
				rotate(-90);
				break;
			case 270:
				rotate(-180);
				break;
			case -90:
				rotate(180);
				break;
			default:
				break;
		}
	}

	@Override
	void moveUp(final int x, final int y, final int a) {
		switch (a) {
			case 360:
				rotate(-180);
				break;
			case 0:
			case -360:
				rotate(180);
				break;
			case 90:
			case -270:
				rotate(90);
				break;
			case 180:
			case -180:
				if (canMove(x, y - 1)) {
					env.updateGridEmpty(x, y);
					env.updateGridHunter(x, y - 1);
					travel(350);
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

	private void pauseRobot() {
		paused = true;
	}

	public void resumeRobot() {
		synchronized (pauseLock) {
			paused = false;
			pauseLock.notifyAll();
		}
	}

	private void deepLearningRunning() {
		while (!exit) {
			final int x = getGridPosX();
			final int y = getGridPosY();
			final int a = getHeading();

			// check if in a goal state
			if (isAdjacentToPrey(x, y)) {
				// Do nothing while in goal state
				logger.info("in a goal state");
				env.updateGridHunter(x, y);
				pauseRobot();
			}

			// check if paused and should be waiting
			synchronized (pauseLock) {
				if (paused) {
					try {
						pauseLock.wait();
					} catch (final InterruptedException ex) {
						ex.printStackTrace();
						Thread.currentThread().interrupt();
					}
				}
			}

			// compare the current state to the next state produced from qlearning
			// final int nextState = learning.getAction(currState);
			final int[] states = new int[4];
			states[0] = getCurentState(x, y);
			states[1] = otherHunters[0].getCurentState(otherHunters[0].getGridPosX(), otherHunters[0].getGridPosY());
			states[2] = otherHunters[1].getCurentState(otherHunters[1].getGridPosX(), otherHunters[1].getGridPosY());
			states[3] = otherHunters[2].getCurentState(otherHunters[2].getGridPosX(), otherHunters[2].getGridPosY());

			final int direction = learning.getActionFromStates(states);

			learning.updateEpsilon();

			switch (direction) {
				case 1:
					// right
					moveRight(x, y, a);
					break;
				case 2:
					// down
					moveDown(x, y, a);
					break;
				case 3:
					// left
					moveLeft(x, y, a);
					break;
				case 4:
					// up
					moveUp(x, y, a);
					break;
				default:
					break;
			}

			final double score = isAdjacentToPrey(getGridPosX(), getGridPosY()) ? 100 : -1;

			if (score == 100) {
				logger.log(Level.OFF, "Good Score");
			}

			// compare the current state to the next state produced from qlearning
			// final int nextState = learning.getAction(currState);
			final int[] newStates = new int[4];
			states[0] = getCurentState(x, y);
			states[1] = otherHunters[0].getCurentState(otherHunters[0].getGridPosX(), otherHunters[0].getGridPosY());
			states[2] = otherHunters[1].getCurentState(otherHunters[1].getGridPosX(), otherHunters[1].getGridPosY());
			states[3] = otherHunters[2].getCurentState(otherHunters[2].getGridPosX(), otherHunters[2].getGridPosY());

			learning.update(states, direction, score, newStates);
		}
	}

	@Override
	public void run() {
		// qlearningRunning();
		deepLearningRunning();

		final String endLog = "Hunter " + number + " Stopped";
		logger.info(endLog);
	}

	// private void qlearningRunning() {
	// while (!exit) {
	// // train
	// // learning.train();

	// final int x = getGridPosX();
	// final int y = getGridPosY();
	// final int a = getHeading();

	// // check if in a goal state
	// if (isAdjacentToPrey(x, y)) {
	// // Do nothing while in goal state
	// logger.info("in a goal state");
	// env.updateGridHunter(x, y);
	// pauseRobot();
	// }

	// // check if paused and should be waiting
	// synchronized (pauseLock) {
	// if (paused) {
	// try {
	// pauseLock.wait();
	// } catch (final InterruptedException ex) {
	// ex.printStackTrace();
	// Thread.currentThread().interrupt();
	// }
	// }
	// }

	// // compare the current state to the next state produced from qlearning
	// final int currState = getCurentState(x, y);

	// final int nextState = learning.getActionFromState(currState);

	// if (currState + 1 == nextState) {
	// // right
	// moveRight(x, y, a);
	// } else if (currState - 1 == nextState) {
	// // left
	// moveLeft(x, y, a);
	// } else if (currState + 10 == nextState) {
	// // up
	// moveDown(x, y, a);
	// } else if (currState - 10 == nextState) {
	// // down
	// moveUp(x, y, a);
	// }
	// }
	// }

	@Override
	public void stopRobot() {
		super.stopRobot();
		resumeRobot();
		resetHunterCount();
	}

}
