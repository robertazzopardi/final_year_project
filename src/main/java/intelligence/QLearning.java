package intelligence;

import java.util.ArrayList;
import java.util.Random;

import comp329robosim.MyGridCell;
import comp329robosim.OccupancyType;

import simulation.SimulationEnv;

/**
 * @author rob
 *
 */
final class QLearning extends Intelligence {
	private static final double ALPHA = 0.1; // Learning rate
	private static final int EPOCH = 1000;

	private static final double GAMMA = 0.9; // Eagerness - 0 looks in the near future, 1 looks in the distant future

	private static final int PENALTY = -100;

	private static final Random rand = new Random();

	private static final int REWARD = 100;
	private static final int STATESCOUNT = SimulationEnv.SIZE * SimulationEnv.SIZE;

	private final MyGridCell[][] maze;

	private double[][] Q;

	private int[][] R;

	public QLearning(final MyGridCell[][] grid) {
		this.maze = grid;
	}

	private void calculateQ() {
		for (int i = 0; i < EPOCH; i++) {
			// Select random initial state
			int crtState = rand.nextInt(STATESCOUNT);

			while (!isFinalState(crtState)) {
				final int[] actionsFromCurrentState = possibleActionsFromState(crtState);

				if (actionsFromCurrentState.length == 0) {
					return;
				}

				// Pick a random action from the ones possible
				final int index = rand.nextInt(actionsFromCurrentState.length);
				final int nextState = actionsFromCurrentState[index];

				// Q(state,action)= Q(state,action) + alpha * (R(state,action) + gamma *
				// Max(next state, all actions) - Q(state,action))
				final double q = Q[crtState][nextState];
				final double maxQ = maxQ(nextState);
				final int r = R[crtState][nextState];
				final double value = q + ALPHA * (r + GAMMA * maxQ - q);

				Q[crtState][nextState] = value;
				crtState = nextState;
			}
		}
	}

	// public int getPolicyFromState(final int state) {
	// final int[] actionsFromState = possibleActionsFromState(state);
	// double maxValue = Double.MIN_VALUE;
	// int policyGotoState = state;

	// // Pick to move to the state that has the maximum Q value
	// for (final int nextState : actionsFromState) {
	// final double value = Q[state][nextState];
	// if (value > maxValue) {
	// maxValue = value;
	// policyGotoState = nextState;
	// }
	// }
	// return policyGotoState;
	// }

	@Override
	public int predict(final int state) {
		final int[] actionsFromState = possibleActionsFromState(state);
		double maxValue = Double.MIN_VALUE;
		int policyGotoState = state;

		// Pick to move to the state that has the maximum Q value
		for (final int nextState : actionsFromState) {
			final double value = Q[state][nextState];
			if (value > maxValue) {
				maxValue = value;
				policyGotoState = nextState;
			}
		}
		return policyGotoState;

	}

	private void init() {
		R = new int[STATESCOUNT][STATESCOUNT];
		Q = new double[STATESCOUNT][STATESCOUNT];

		int i = 0;
		int j = 0;

		// We will navigate through the reward matrix R using k index
		for (int k = 0; k < STATESCOUNT; k++) {
			// We will navigate with i and j through the maze, so we need
			// to translate k into i and j
			i = k / SimulationEnv.SIZE;
			j = k - i * SimulationEnv.SIZE;

			// Fill in the reward matrix with -1
			for (int s = 0; s < STATESCOUNT; s++) {
				R[k][s] = -1;
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
			for (int j = 0; j < STATESCOUNT; j++) {
				Q[i][j] = R[i][j];
			}
		}
	}

	private boolean isFinalState(final int state) {
		final int i = state / SimulationEnv.SIZE;
		final int j = state - i * SimulationEnv.SIZE;

		return maze[i][j].getCellType() == OccupancyType.GOAL;
	}

	private double maxQ(final int nextState) {
		final int[] actionsFromState = possibleActionsFromState(nextState);
		// the learning rate and eagerness will keep the W value above the lowest reward
		double maxValue = -10;
		for (final int nextAction : actionsFromState) {
			final double value = Q[nextState][nextAction];
			if (value > maxValue) {
				maxValue = value;
			}
		}
		return maxValue;
	}

	public int[] possibleActionsFromState(final int state) {
		final ArrayList<Integer> result = new ArrayList<>();
		for (int i = 0; i < STATESCOUNT; i++) {
			if (R[state][i] != -1) {
				result.add(i);
			}
		}

		return result.stream().mapToInt(i -> i).toArray();
	}

	@Override
	public void train() {
		init();
		calculateQ();
	}

	/**
	 * @param i
	 * @param j
	 * @param k
	 */
	private void tryMoveDown(final int i, final int j, final int k) {
		final int goDown = i + 1;
		if (goDown < SimulationEnv.SIZE) {
			final int target = goDown * SimulationEnv.SIZE + j;

			switch (maze[goDown][j].getCellType()) {
				case EMPTY:
					R[k][target] = 0;
					break;
				case GOAL:
					R[k][target] = REWARD;
					break;
				default:
					R[k][target] = PENALTY;
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
			final int target = i * SimulationEnv.SIZE + goLeft;

			switch (maze[i][goLeft].getCellType()) {
				case EMPTY:
					R[k][target] = 0;
					break;
				case GOAL:
					R[k][target] = REWARD;
					break;
				default:
					R[k][target] = PENALTY;
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
		if (goRight < SimulationEnv.SIZE) {
			final int target = i * SimulationEnv.SIZE + goRight;

			switch (maze[i][goRight].getCellType()) {
				case EMPTY:
					R[k][target] = 0;
					break;
				case GOAL:
					R[k][target] = REWARD;
					break;
				default:
					R[k][target] = PENALTY;
					break;
			}
		}
	}

	// public void printPolicy() {
	// System.out.println("\nPrint policy");
	// for (int i = 0; i < STATESCOUNT; i++) {
	// System.out.println("From state " + i + " goto state " +
	// getPolicyFromState(i));
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
	// System.out.printf("%25s", "States: ");
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
			final int target = goUp * SimulationEnv.SIZE + j;

			switch (maze[goUp][j].getCellType()) {
				case EMPTY:
					R[k][target] = 0;
					break;
				case GOAL:
					R[k][target] = REWARD;
					break;
				default:
					R[k][target] = PENALTY;
					break;
			}
		}
	}

}
