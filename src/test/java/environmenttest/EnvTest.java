package environmenttest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.nd4j.linalg.api.ndarray.INDArray;
import comp329robosim.MyGridCell;
import comp329robosim.OccupancyType;
import environment.Env;
import intelligence.Mode;
import io.netty.util.internal.ThreadLocalRandom;
import robots.Action;
import robots.Agent;
import robots.StepObs;

class EnvTest {

    private final Env env;
    final List<Agent> agents;

    public EnvTest() {
        env = new Env("", Env.GRID_SIZE, Env.GRID_SIZE, Mode.TRAIN_ON);
        agents = env.getAgents();
    }

    @Test
    void testGetAgents() {
        assertNotNull(agents);
        assertEquals("Prey", agents.get(agents.size() - 1).getClass().getSimpleName());
    }

    @Test
    void testEnvReset() {
        final INDArray[] observations = env.reset();

        assertEquals(Env.HUNTER_COUNT, observations.length);
    }


    @Test
    void testEnvStep() {
        final Action[] actions = new Action[Env.HUNTER_COUNT];
        for (int i = 0; i < Env.HUNTER_COUNT; i++) {
            actions[i] = Action.getRandomAction();
        }
        final StepObs stepObs = env.step(actions);
        assertNotNull(stepObs);
        assertNotNull(stepObs.getNextStates());
        assertNotNull(stepObs.getRewards());


    }

    @Test
    void testAgentSamePosition() {
        final Agent agent = agents.get(ThreadLocalRandom.current().nextInt(Env.HUNTER_COUNT));
        assertFalse(env.isSamePosition(agent));
    }

    @Test
    void checkModeTest() {
        assertEquals(Mode.EVAL, env.getMode());
    }

    @Test
    void updateGridEmptyTest() {
        MyGridCell[][] grid = env.getGrid();

        grid[1][1].setCellType(OccupancyType.UNKNOWN);

        assertEquals(OccupancyType.UNKNOWN, grid[1][1].getCellType());

        assertTrue(grid[1][1].isEmpty());
    }

    @Test
    void updateGridTest() {
        MyGridCell[][] grid = env.getGrid();

        grid[1][1].setCellType(OccupancyType.UNKNOWN);

        assertEquals(OccupancyType.UNKNOWN, grid[1][1].getCellType());

        assertEquals(OccupancyType.GOAL, grid[1][1].getCellType(),
                "Cell is:" + grid[1][1].getCellType());

        assertEquals(OccupancyType.HUNTER, grid[1][1].getCellType(),
                "Cell is:" + grid[1][1].getCellType());

        assertEquals(OccupancyType.PREY, grid[1][1].getCellType(),
                "Cell is:" + grid[1][1].getCellType());
    }

}
