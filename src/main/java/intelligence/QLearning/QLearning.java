package intelligence.qlearning;

import java.util.ArrayList;
import java.util.Random;
import comp329robosim.MyGridCell;
import comp329robosim.OccupancyType;
import environment.Env;

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

	private int[][] rewards;

	public QLearning(final MyGridCell[][] grid) {
		this.maze = grid;
	}

	private void calculateQ() {
		for (int i = 0; i < EPOCH; i++) {
			// Select random initial state
			int crtObservation = rand.nextInt(STATESCOUNT);

			while (!isTerminalState(crtObservation)) {
				final int[] actionsFromCurrentObservation = getPolicy(crtObservation);

				if (actionsFromCurrentObservation.length == 0) {
					return;
				}

				final int index = rand.nextInt(actionsFromCurrentObservation.length);
				final int newObservations = actionsFromCurrentObservation[index];

				final double qv = qValues[crtObservation][newObservations];
				final double maxQ = getMaxQValue(newObservations);
				final int reward = rewards[crtObservation][newObservations];

				final double value = qv + ALPHA * (reward + GAMMA * maxQ - qv);

				qValues[crtObservation][newObservations] = value;
				crtObservation = newObservations;
			}
		}
	}

	// public int getPolicyFromObservation(final int state) {
	// final int[] actionsFromObservation = getPolicy(state);
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

		final int[] actionsFromObservation = getPolicy(state);
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
		rewards = new int[STATESCOUNT][STATESCOUNT];
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
				rewards[k][s] = -1;
			}
			// If not in final state or a wall try moving in all directions in the maze

			if (maze[i][j].getCellType() != OccupancyType.GOAL) {
				tryMoveLeftRight(i, k, j + 1);
				tryMoveLeftRight(i, k, j - 1);

				tryMoveUpDown(j, k, i + 1);
				tryMoveUpDown(j, k, i - 1);
			}
		}
		initializeQ();
		// printR(R);
	}

	// Set Q values to R values
	private void initializeQ() {
		for (int i = 0; i < STATESCOUNT; i++) {
			final int[] aMatrix = rewards[i];
			final int aLength = aMatrix.length;
			qValues[i] = new double[aLength];
			System.arraycopy(aMatrix, 0, qValues[i], 0, aLength);
		}
	}

	private boolean isTerminalState(final int state) {
		return maze[state / Env.GRID_SIZE][state - (state / Env.GRID_SIZE) * Env.GRID_SIZE]
				.getCellType() == OccupancyType.GOAL;
	}

	private double getMaxQValue(final int newObservations) {
		final int[] actionsFromObservation = getPolicy(newObservations);
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

	public int[] getPolicy(final int state) {
		final ArrayList<Integer> result = new ArrayList<>();
		for (int i = 0; i < STATESCOUNT; i++) {
			if (rewards[state][i] != -1) {
				result.add(i);
			}
		}

		return result.stream().mapToInt(i -> i).toArray();
	}

	private void updateRewards(final OccupancyType type, final int row, final int col) {
		switch (type) {
			case EMPTY:
				rewards[row][col] = 0;
				break;
			case GOAL:
				rewards[row][col] = REWARD;
				break;
			default:
				rewards[row][col] = PENALTY;
				break;
		}
	}

	private void tryMoveUpDown(final int j, final int k, final int next) {
		if (next >= 0 || next < Env.GRID_SIZE) {
			updateRewards(maze[next][j].getCellType(), k, next * Env.GRID_SIZE + j);
		}
	}

	private void tryMoveLeftRight(final int i, final int k, final int next) {
		if (next >= 0 || next < Env.GRID_SIZE) {
			updateRewards(maze[i][next].getCellType(), k, i * Env.GRID_SIZE + next);
		}
	}

	void train() {
		init();
		calculateQ();
	}

}
