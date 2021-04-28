package intelligence.qlearning;

import java.util.ArrayList;
import java.util.Random;
import comp329robosim.MyGridCell;
import comp329robosim.OccupancyType;
import simulation.Env;

/**
 * @author rob
 *
 */
final class QLearning {
	private static final double ALPHA = 0.1; // Learning rate
	private static final int EPOCH = 1000;

	private static final double GAMMA = 0.9; // Eagerness - 0 looks in the near future, 1 looks in
												// the distant future

	private static final int PENALTY = -100;

	private static final Random rand = new Random();

	private static final int REWARD = 100;
	private static final int STATESCOUNT = Env.GRID_SIZE * Env.GRID_SIZE;

	private final MyGridCell[][] maze;

	private double[][] qValues;

	private int[][] rValues;

	public QLearning(final MyGridCell[][] grid) {
		this.maze = grid;
	}

	private void calculateQ() {
		for (int i = 0; i < EPOCH; i++) {
			// Select random initial state
			int crtObservation = rand.nextInt(STATESCOUNT);

			while (!isFinalObservation(crtObservation)) {
				final int[] actionsFromCurrentObservation =
						possibleActionsFromObservation(crtObservation);

				if (actionsFromCurrentObservation.length == 0) {
					return;
				}

				// Pick a random action from the ones possible
				final int index = rand.nextInt(actionsFromCurrentObservation.length);
				final int newObservations = actionsFromCurrentObservation[index];

				// Q(state,action)= Q(state,action) + alpha * (R(state,action) + gamma *
				// Max(next state, all actions) - Q(state,action))
				final double q = qValues[crtObservation][newObservations];
				final double maxQ = maxQ(newObservations);
				final int r = rValues[crtObservation][newObservations];
				final double value = q + ALPHA * (r + GAMMA * maxQ - q);

				qValues[crtObservation][newObservations] = value;
				crtObservation = newObservations;
			}
		}
	}

	// public int getPolicyFromObservation(final int state) {
	// final int[] actionsFromObservation = possibleActionsFromObservation(state);
	// double maxValue = Double.MIN_VALUE;
	// int policyGotoObservation = state;

	// // Pick to move to the state that has the maximum Q value
	// for (final int newObservations : actionsFromObservation) {
	// final double value = Q[state][newObservations];
	// if (value > maxValue) {
	// maxValue = value;
	// policyGotoObservation = newObservations;
	// }
	// }
	// return policyGotoObservation;
	// }

	public int getActionFromObservation(final int state) {
		train();

		final int[] actionsFromObservation = possibleActionsFromObservation(state);
		double maxValue = Double.MIN_VALUE;
		int policyGotoObservation = state;

		// Pick to move to the state that has the maximum Q value
		for (final int newObservations : actionsFromObservation) {
			final double value = qValues[state][newObservations];
			if (value > maxValue) {
				maxValue = value;
				policyGotoObservation = newObservations;
			}
		}
		return policyGotoObservation;
	}

	private void init() {
		rValues = new int[STATESCOUNT][STATESCOUNT];
		qValues = new double[STATESCOUNT][STATESCOUNT];

		int i = 0;
		int j = 0;

		// We will navigate through the reward matrix R using k index
		for (int k = 0; k < STATESCOUNT; k++) {
			// We will navigate with i and j through the maze, so we need
			// to translate k into i and j
			i = k / Env.GRID_SIZE;
			j = k - i * Env.GRID_SIZE;

			// Fill in the reward matrix with -1
			for (int s = 0; s < STATESCOUNT; s++) {
				rValues[k][s] = -1;
			}
			// If not in final state or a wall try moving in all directions in the maze

			if (maze[i][j].getCellType() != OccupancyType.GOAL) {
				// Try to move left in the maze
				tryMoveLeft(i, j, k);

				// Try to move right in the maze
				tryMoveRight(i, j, k);

				// Try to move up in the maze
				tryMoveUp(i, j, k);

				// Try to move down in the maze
				tryMoveDown(i, j, k);
			}
		}
		initializeQ();
		// printR(R);
	}

	// Set Q values to R values
	private void initializeQ() {
		for (int i = 0; i < STATESCOUNT; i++) {
			// for (int j = 0; j < STATESCOUNT; j++) {
			// qValues[i][j] = rValues[i][j];
			// }
			final int[] aMatrix = rValues[i];
			final int aLength = aMatrix.length;
			qValues[i] = new double[aLength];
			System.arraycopy(aMatrix, 0, qValues[i], 0, aLength);
		}
	}

	private boolean isFinalObservation(final int state) {
		final int i = state / Env.GRID_SIZE;
		final int j = state - i * Env.GRID_SIZE;

		return maze[i][j].getCellType() == OccupancyType.GOAL;
	}

	private double maxQ(final int newObservations) {
		final int[] actionsFromObservation = possibleActionsFromObservation(newObservations);
		// the learning rate and eagerness will keep the W value above the lowest reward
		double maxValue = -10;
		for (final int nextAction : actionsFromObservation) {
			final double value = qValues[newObservations][nextAction];
			if (value > maxValue) {
				maxValue = value;
			}
		}
		return maxValue;
	}

	public int[] possibleActionsFromObservation(final int state) {
		final ArrayList<Integer> result = new ArrayList<>();
		for (int i = 0; i < STATESCOUNT; i++) {
			if (rValues[state][i] != -1) {
				result.add(i);
			}
		}

		return result.stream().mapToInt(i -> i).toArray();
	}

	/**
	 * @param i
	 * @param j
	 * @param k
	 */
	private void tryMoveDown(final int i, final int j, final int k) {
		final int goDown = i + 1;
		if (goDown < Env.GRID_SIZE) {
			final int target = goDown * Env.GRID_SIZE + j;

			switch (maze[goDown][j].getCellType()) {
				case EMPTY:
					rValues[k][target] = 0;
					break;
				case GOAL:
					rValues[k][target] = REWARD;
					break;
				default:
					rValues[k][target] = PENALTY;
					break;
			}
		}
	}

	/**
	 * @param i
	 * @param j
	 * @param k
	 */
	private void tryMoveLeft(final int i, final int j, final int k) {
		final int goLeft = j - 1;
		if (goLeft >= 0) {
			final int target = i * Env.GRID_SIZE + goLeft;

			switch (maze[i][goLeft].getCellType()) {
				case EMPTY:
					rValues[k][target] = 0;
					break;
				case GOAL:
					rValues[k][target] = REWARD;
					break;
				default:
					rValues[k][target] = PENALTY;
					break;
			}
		}
	}

	/**
	 * @param i
	 * @param j
	 * @param k
	 */
	private void tryMoveRight(final int i, final int j, final int k) {
		final int goRight = j + 1;
		if (goRight < Env.GRID_SIZE) {
			final int target = i * Env.GRID_SIZE + goRight;

			switch (maze[i][goRight].getCellType()) {
				case EMPTY:
					rValues[k][target] = 0;
					break;
				case GOAL:
					rValues[k][target] = REWARD;
					break;
				default:
					rValues[k][target] = PENALTY;
					break;
			}
		}
	}

	// public void printPolicy() {
	// System.out.println("\nPrint policy");
	// for (int i = 0; i < STATESCOUNT; i++) {
	// System.out.println("From state " + i + " goto state " +
	// getPolicyFromObservation(i));
	// }
	// }
	//
	// public void printQ() {
	// System.out.println("Q matrix");
	// for (int i = 0; i < Q.length; i++) {
	// System.out.print("From state " + i + ": ");
	// for (int j = 0; j < Q[i].length; j++) {
	// System.out.printf("%6.2f ", (Q[i][j]));
	// }
	// System.out.println();
	// }
	// }
	//
	// // Used for debug
	// public void printR(final int[][] matrix) {
	// System.out.printf("%25s", "Observations: ");
	// for (int i = 0; i <= 8; i++) {
	// System.out.printf("%4s", i);
	// }
	// System.out.println();
	// for (int i = 0; i < STATESCOUNT; i++) {
	// System.out.print("Possible states from " + i + " :[");
	// for (int j = 0; j < STATESCOUNT; j++) {
	// System.out.printf("%4s", matrix[i][j]);
	// }
	// System.out.println("]");
	// }
	// }

	/**
	 * @param i
	 * @param j
	 * @param k
	 */
	private void tryMoveUp(final int i, final int j, final int k) {
		final int goUp = i - 1;
		if (goUp >= 0) {
			final int target = goUp * Env.GRID_SIZE + j;

			switch (maze[goUp][j].getCellType()) {
				case EMPTY:
					rValues[k][target] = 0;
					break;
				case GOAL:
					rValues[k][target] = REWARD;
					break;
				default:
					rValues[k][target] = PENALTY;
					break;
			}
		}
	}

	void train() {
		init();
		calculateQ();
	}

}
