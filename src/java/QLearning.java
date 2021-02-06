import java.util.ArrayList;
import java.util.Random;

import comp329robosim.MyGridCell;
import comp329robosim.OccupancyType;

/**
 * @author rob
 *
 */
public class QLearning {
	private static final double ALPHA = 0.1; // Learning rate
	private static final double GAMMA = 0.9; // Eagerness - 0 looks in the near future, 1 looks in the distant future

	private static final int PENALTY = -100;

	private static final Random rand = new Random();

	private static final int REWARD = 100;
	private static final int STATESCOUNT = SimulationEnv.WIDTH * SimulationEnv.HEIGHT;

	private SimulationEnv env;

	private MyGridCell[][] maze;

	private double[][] Q; // Q learning

	// public static void qlearn() {
	// QLearning ql = new QLearning();
	// ql.init();
	// ql.calculateQ();
	// ql.printQ();
	// ql.printPolicy();
	// }

	private int[][] R; // Reward lookup

	public QLearning(SimulationEnv env) {
		this.env = env;

		init();
		calculateQ();
		// printQ();
		// printPolicy();
	}

	private void calculateQ() {

		for (int i = 0; i < 1000; i++) { // Train cycles
			// Select random initial state
			int crtState = rand.nextInt(STATESCOUNT);

			while (!isFinalState(crtState)) {
				int[] actionsFromCurrentState = possibleActionsFromState(crtState);

				// Pick a random action from the ones possible
				int index = rand.nextInt(actionsFromCurrentState.length);
				int nextState = actionsFromCurrentState[index];

				// Q(state,action)= Q(state,action) + alpha * (R(state,action) + gamma *
				// Max(next state, all actions) - Q(state,action))
				double q = Q[crtState][nextState];
				double maxQ = maxQ(nextState);
				int r = R[crtState][nextState];
				double value = q + ALPHA * (r + GAMMA * maxQ - q);

				Q[crtState][nextState] = value;
				crtState = nextState;
			}
		}
	}

	public int getPolicyFromState(int state) {
		int[] actionsFromState = possibleActionsFromState(state);
		double maxValue = Double.MIN_VALUE;
		int policyGotoState = state;

		// Pick to move to the state that has the maximum Q value
		for (int nextState : actionsFromState) {
			double value = Q[state][nextState];
			if (value > maxValue) {
				maxValue = value;
				policyGotoState = nextState;
			}
		}
		return policyGotoState;
	}

	private void init() {
		maze = env.getGrid();

		env.printGrid();

		R = new int[STATESCOUNT][STATESCOUNT];
		Q = new double[STATESCOUNT][STATESCOUNT];

		int i = 0;
		int j = 0;

		// We will navigate through the reward matrix R using k index
		for (int k = 0; k < STATESCOUNT; k++) {
			// We will navigate with i and j through the maze, so we need
			// to translate k into i and j
			i = k / SimulationEnv.WIDTH;
			j = k - i * SimulationEnv.WIDTH;
			// Fill in the reward matrix with -1
			for (int s = 0; s < STATESCOUNT; s++) {
				R[k][s] = -1;
			}
			// If not in final state or a wall try moving in all directions in the maze

			if (maze[i][j].getCellType() != OccupancyType.GOAL) {
				// Try to move left in the maze
				int goLeft = j - 1;
				if (goLeft >= 0) {
					int target = i * SimulationEnv.WIDTH + goLeft;

					if (maze[i][goLeft].getCellType() == OccupancyType.EMPTY) {
						R[k][target] = 0;
					} else if (maze[i][goLeft].getCellType() == OccupancyType.GOAL) {
						R[k][target] = REWARD;
					} else {
						R[k][target] = PENALTY;
					}
				}

				// Try to move right in the maze
				int goRight = j + 1;
				if (goRight < SimulationEnv.WIDTH) {
					int target = i * SimulationEnv.WIDTH + goRight;

					if (maze[i][goRight].getCellType() == OccupancyType.EMPTY) {
						R[k][target] = 0;
					} else if (maze[i][goRight].getCellType() == OccupancyType.GOAL) {
						R[k][target] = REWARD;
					} else {
						R[k][target] = PENALTY;
					}
				}
				// Try to move up in the maze
				int goUp = i - 1;
				if (goUp >= 0) {
					int target = goUp * SimulationEnv.WIDTH + j;

					if (maze[goUp][j].getCellType() == OccupancyType.EMPTY) {
						R[k][target] = 0;
					} else if (maze[goUp][j].getCellType() == OccupancyType.GOAL) {
						R[k][target] = REWARD;
					} else {
						R[k][target] = PENALTY;
					}
				}
				// Try to move down in the maze
				int goDown = i + 1;
				if (goDown < SimulationEnv.WIDTH) {
					int target = goDown * SimulationEnv.WIDTH + j;

					if (maze[goDown][j].getCellType() == OccupancyType.EMPTY) {
						R[k][target] = 0;
					} else if (maze[goDown][j].getCellType() == OccupancyType.GOAL) {
						R[k][target] = REWARD;
					} else {
						R[k][target] = PENALTY;
					}
				}
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

	private boolean isFinalState(int state) {
		int i = state / SimulationEnv.WIDTH;
		int j = state - i * SimulationEnv.WIDTH;

		return maze[i][j].getCellType() == OccupancyType.GOAL;
	}

	private double maxQ(int nextState) {
		int[] actionsFromState = possibleActionsFromState(nextState);
		// the learning rate and eagerness will keep the W value above the lowest reward
		double maxValue = -10;
		for (int nextAction : actionsFromState) {
			double value = Q[nextState][nextAction];
			if (value > maxValue)
				maxValue = value;
		}
		return maxValue;
	}

	public int[] possibleActionsFromState(int state) {
		ArrayList<Integer> result = new ArrayList<>();
		for (int i = 0; i < STATESCOUNT; i++) {
			if (R[state][i] != -1) {
				result.add(i);
			}
		}
		return result.stream().mapToInt(i -> i).toArray();
	}

	public void printPolicy() {
		System.out.println("\nPrint policy");
		for (int i = 0; i < STATESCOUNT; i++) {
			System.out.println("From state " + i + " goto state " + getPolicyFromState(i));
		}
	}

	public void printQ() {
		System.out.println("Q matrix");
		for (int i = 0; i < Q.length; i++) {
			System.out.print("From state " + i + ":  ");
			for (int j = 0; j < Q[i].length; j++) {
				System.out.printf("%6.2f ", (Q[i][j]));
			}
			System.out.println();
		}
	}

	// Used for debug
	public void printR(int[][] matrix) {
		System.out.printf("%25s", "States: ");
		for (int i = 0; i <= 8; i++) {
			System.out.printf("%4s", i);
		}
		System.out.println();
		for (int i = 0; i < STATESCOUNT; i++) {
			System.out.print("Possible states from " + i + " :[");
			for (int j = 0; j < STATESCOUNT; j++) {
				System.out.printf("%4s", matrix[i][j]);
			}
			System.out.println("]");
		}
	}

	public synchronized void retrain() {
		init();
		calculateQ();
	}

}
