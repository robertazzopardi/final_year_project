// Environment code for project final_year_project

import java.util.Arrays;
import java.util.logging.Logger;

import comp329robosim.EnvController;
import comp329robosim.MyGridCell;
import comp329robosim.OccupancyType;
import jason.asSyntax.Structure;
//import jason.asSyntax.parser.*;
import jason.environment.Environment;

public class SimulationEnv extends Environment {

	private static final String CONFIG_FILE = "/Users/rob/_CODE/Java/final-project/defaultConfig.txt";

	public static final int HEIGHT = 10;

	public static final int WIDTH = 10;

	private EnvController controller;

	private MyGridCell[][] grid;

	private final Logger logger = Logger.getLogger("final_year_project." + SimulationEnv.class.getName());

	@Override
	public boolean executeAction(final String agName, final Structure action) {
		final String executionInfo = String.format("executing: %s, but not implemented!", action);
		logger.info(executionInfo);
		if (true) { // you may improve this condition
			informAgsEnvironmentChanged();
		}
		return true; // the action was executed with success
	}

	public EnvController getController() {
		return controller;
	}

	public synchronized MyGridCell[][] getGrid() {
		return grid;
	}

	/** Called before the MAS execution with the args informed in .mas2j */
	@Override
	public void init(final String[] args) {
		super.init(args);

		// get the controller
		controller = new EnvController(CONFIG_FILE, WIDTH, HEIGHT);

		grid = controller.getGrid();

		new RobotController(this);

	}

	public void printGrid(final Logger inLogger) {

		synchronized (grid) {
			for (int i = 0; i < grid.length; i++) {
				final String row = Arrays.toString(grid[i]);
				inLogger.info(row);
			}

			inLogger.info("");
		}
	}

	public synchronized void setPreviousPositionDown(final int x, final int y) {

		if (grid[y][x].getCellType() != OccupancyType.OBSTACLE && grid[y][x].getCellType() != OccupancyType.HUNTER) {
			grid[y][x].setEmpty();
		}

		if (grid[y][x].getCellType() != OccupancyType.PREY) {
			grid[y + 1][x].setCellType(OccupancyType.PREY);
		}

	}

	public synchronized void setPreviousPositionLeft(final int x, final int y) {

		if (grid[y][x].getCellType() != OccupancyType.OBSTACLE && grid[y][x].getCellType() != OccupancyType.HUNTER) {
			grid[y][x].setEmpty();
		}

		if (grid[y][x].getCellType() != OccupancyType.PREY) {
			grid[y][x - 1].setCellType(OccupancyType.PREY);
		}

	}

	public synchronized void setPreviousPositionRight(final int x, final int y) {

		if (grid[y][x].getCellType() != OccupancyType.OBSTACLE && grid[y][x].getCellType() != OccupancyType.HUNTER) {
			grid[y][x].setEmpty();
		}

		if (grid[y][x].getCellType() != OccupancyType.PREY) {
			grid[y][x + 1].setCellType(OccupancyType.PREY);
		}

	}

	public void setPreviousPositionUp(final int x, final int y) {

		if (grid[y][x].getCellType() != OccupancyType.OBSTACLE && grid[y][x].getCellType() != OccupancyType.HUNTER) {
			grid[y][x].setEmpty();
		}

		if (grid[y][x].getCellType() != OccupancyType.PREY) {
			grid[y - 1][x].setCellType(OccupancyType.PREY);
		}

	}

	public void updateEnv(final int x, final int y, final OccupancyType occupancyType) {
		grid[y][x].setCellType(occupancyType);
	}

	public synchronized void updateEnvOldNew(final int nx, final int ny, final int ox, final int oy) {

		if (grid[ny][nx] != grid[oy][ox]) {
			grid[oy][ox].setEmpty();
		}

		if (grid[ny][nx].getCellType() != OccupancyType.PREY) {
			grid[ny][nx].setCellType(OccupancyType.HUNTER);
		}

	}

}
