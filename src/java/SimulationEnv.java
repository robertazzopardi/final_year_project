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

	final Logger logger = Logger.getLogger("final_year_project." + SimulationEnv.class.getName());

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

	public MyGridCell[][] getGrid() {
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
		for (final MyGridCell[] myGridCells : grid) {
			final String row = Arrays.toString(myGridCells);
			inLogger.info(row);
		}
		inLogger.info("");
	}

	public void updateGridEmpty(final int x, final int y) {
		synchronized (grid[y][x]) {
			grid[y][x].setEmpty();
		}
	}

	public void updateGridGoal(final int x, final int y) {
		synchronized (grid[y][x]) {
			grid[y][x].setCellType(OccupancyType.GOAL);
		}
	}

	public void updateGridHunter(final int x, final int y) {
		synchronized (grid[y][x]) {
			grid[y][x].setCellType(OccupancyType.HUNTER);
		}
	}

	public void updateGridPrey(final int x, final int y) {
		synchronized (grid[y][x]) {
			grid[y][x].setCellType(OccupancyType.PREY);
		}
	}

}
