package simulationtest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import comp329robosim.MyGridCell;
import comp329robosim.OccupancyType;
import simulation.Mode;
import simulation.SimulationEnv;

class SimulationEnvTest {

    private final SimulationEnv simulationEnv =
            new SimulationEnv("", SimulationEnv.GRID_SIZE, SimulationEnv.GRID_SIZE, Mode.EVAL);

    @Test
    void checkModeTest() {
        assertEquals(Mode.EVAL, simulationEnv.getMode());
    }

    @Test
    void checkEpisodeTest() {
        assertEquals(1, simulationEnv.getEpisode());
    }

    @Test
    void incrementEpisodeTest() {
        simulationEnv.incrementEpisode();
        assertEquals(2, simulationEnv.getEpisode());

        simulationEnv.incrementEpisode();
        assertEquals(3, simulationEnv.getEpisode());
    }

    @Test
    void updateGridEmptyTest() {
        MyGridCell[][] grid = simulationEnv.getGrid();

        grid[1][1].setCellType(OccupancyType.UNKNOWN);

        assertEquals(OccupancyType.UNKNOWN, grid[1][1].getCellType());

        simulationEnv.updateGridEmpty(1, 1);

        assertTrue(grid[1][1].isEmpty());
    }

    @Test
    void updateGridGoalTest() {
        MyGridCell[][] grid = simulationEnv.getGrid();

        grid[1][1].setCellType(OccupancyType.UNKNOWN);

        assertEquals(OccupancyType.UNKNOWN, grid[1][1].getCellType());

        simulationEnv.updateGridGoal(1, 1);

        assertEquals(OccupancyType.GOAL, grid[1][1].getCellType(),
                "Cell is:" + grid[1][1].getCellType());
    }

    @Test
    void updateGridHunterTest() {
        MyGridCell[][] grid = simulationEnv.getGrid();

        grid[1][1].setCellType(OccupancyType.UNKNOWN);

        assertEquals(OccupancyType.UNKNOWN, grid[1][1].getCellType());

        simulationEnv.updateGridHunter(1, 1);

        assertEquals(OccupancyType.HUNTER, grid[1][1].getCellType(),
                "Cell is:" + grid[1][1].getCellType());
    }

    @Test
    void updateGridPreyTest() {
        MyGridCell[][] grid = simulationEnv.getGrid();

        grid[1][1].setCellType(OccupancyType.UNKNOWN);

        assertEquals(OccupancyType.UNKNOWN, grid[1][1].getCellType());

        simulationEnv.updateGridPrey(1, 1);

        assertEquals(OccupancyType.PREY, grid[1][1].getCellType(),
                "Cell is:" + grid[1][1].getCellType());
    }

}
