/**
 * @author rob
 *
 */
public class QLearning {
	private final double gamma = 0.9; // Eagerness - 0 looks in the near future, 1 looks in the distant future

	private final int mazeWidth = 3;
	private final int mazeHeight = 3;
	private final int statesCount = mazeHeight * mazeWidth;

	private final int reward = 100;
	private final int penalty = -10;

}
