package simulationtest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import comp329robosim.MyGridCell;
import comp329robosim.OccupancyType;
import simulation.Mode;
import simulation.Env;

class EnvTest {

    private final Env simulationEnv = new Env("", Env.GRID_SIZE, Env.GRID_SIZE, Mode.EVAL);

    @Test
    void checkModeTest() {
        assertEquals(Mode.EVAL, simulationEnv.getMode());
    }

    // @Test
    // void incrementEpisodeTest() {
    // int episode = simulationEnv.getEpisode();

    // simulationEnv.incrementEpisode();
    // assertEquals(episode + 1, simulationEnv.getEpisode());

    // episode = simulationEnv.getEpisode();

    // simulationEnv.incrementEpisode();
    // assertEquals(episode + 1, simulationEnv.getEpisode());
    // }

    @Test
    void updateGridEmptyTest() {
        MyGridCell[][] grid = simulationEnv.getGrid();

        grid[1][1].setCellType(OccupancyType.UNKNOWN);

        assertEquals(OccupancyType.UNKNOWN, grid[1][1].getCellType());

        // simulationEnv.updateGridEmpty(1, 1);

        assertTrue(grid[1][1].isEmpty());
    }

    @Test
    void updateGridTest() {
        MyGridCell[][] grid = simulationEnv.getGrid();

        grid[1][1].setCellType(OccupancyType.UNKNOWN);

        assertEquals(OccupancyType.UNKNOWN, grid[1][1].getCellType());

        // test goal position
        // simulationEnv.updateGrid(1, 1, OccupancyType.GOAL);

        assertEquals(OccupancyType.GOAL, grid[1][1].getCellType(),
                "Cell is:" + grid[1][1].getCellType());

        // test hunter position
        // simulationEnv.updateGrid(1, 1, OccupancyType.HUNTER);

        assertEquals(OccupancyType.HUNTER, grid[1][1].getCellType(),
                "Cell is:" + grid[1][1].getCellType());

        // test prey Position
        // simulationEnv.updateGrid(1, 1, OccupancyType.PREY);

        assertEquals(OccupancyType.PREY, grid[1][1].getCellType(),
                "Cell is:" + grid[1][1].getCellType());
    }

    // @Test
    // void updateGridHunterTest() {
    // MyGridCell[][] grid = simulationEnv.getGrid();

    // grid[1][1].setCellType(OccupancyType.UNKNOWN);

    // assertEquals(OccupancyType.UNKNOWN, grid[1][1].getCellType());

    // simulationEnv.updateGridHunter(1, 1);

    // assertEquals(OccupancyType.HUNTER, grid[1][1].getCellType(),
    // "Cell is:" + grid[1][1].getCellType());
    // }

    // @Test
    // void updateGridPreyTest() {
    // MyGridCell[][] grid = simulationEnv.getGrid();

    // grid[1][1].setCellType(OccupancyType.UNKNOWN);

    // assertEquals(OccupancyType.UNKNOWN, grid[1][1].getCellType());

    // simulationEnv.updateGridPrey(1, 1);

    // assertEquals(OccupancyType.PREY, grid[1][1].getCellType(),
    // "Cell is:" + grid[1][1].getCellType());
    // }

}
