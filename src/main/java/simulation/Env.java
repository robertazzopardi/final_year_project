package simulation;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.nd4j.linalg.api.ndarray.INDArray;
import comp329robosim.EnvController;
import comp329robosim.MyGridCell;
import comp329robosim.OccupancyType;
import intelligence.Maddpg.Maddpg;
import robots.Action;
import robots.Agent;
import robots.StepObs;

public class Env extends EnvController {
    public static final int TOTAL_EPISODES = 100;
    public static final int CELL_WIDTH = 350;
    public static final int CELL_RADIUS = CELL_WIDTH / 2;
    public static final int GRID_SIZE = 8;
    public static final int ENV_SIZE = GRID_SIZE * CELL_WIDTH;
    public static final int AGENT_COUNT = 5;
    private static final int DELAY = 1000;

    private static final int CAPACITY = 1000000;// Should calculate actual capacity
    public static final int BATCH_SIZE = 64;
    // private static final int MAX_EPISODE = 1001;
    private static final int MAX_EPISODE = 101;
    public static final int MAX_STEP = 100 * GRID_SIZE;

    private static final ExecutorService executor = Executors.newFixedThreadPool(AGENT_COUNT + 1);

    public static final String OUTPUT_FOLDER = "src/main/resources/";

    private final File[] actors;
    private final File[] critics;

    public final int trainedEpisodes;

    private final List<Agent> agents;

    private final Mode mode;

    private final MyGridCell[][] grid;

    public Env(final String confFileName, final int cols, final int rows, final Mode mode) {
        super(confFileName, cols, rows);

        this.mode = mode;

        grid = getGrid();

        addBoundaries();

        final File[] files = new File(OUTPUT_FOLDER).listFiles((dir1, filename) -> filename.endsWith(".zip"));
        actors = Arrays.stream(files).filter(i -> i.getName().contains("actor")).toArray(File[]::new);
        critics = Arrays.stream(files).filter(i -> i.getName().contains("critic")).toArray(File[]::new);

        if (actors.length > 0) {
            final String name = actors[0].getName();
            trainedEpisodes = Integer.parseInt((String) name.subSequence(name.indexOf("_") + 1, name.lastIndexOf("_")))
                    + MAX_EPISODE;
        } else
            trainedEpisodes = MAX_EPISODE;

        agents = new ArrayList<>();

        new Maddpg(CAPACITY, this, MAX_EPISODE, MAX_STEP, BATCH_SIZE).run();
    }

    // /**
    // * Initialise the robot controller
    // */
    // public void startController() {
    // new RobotController(this);
    // }

    public void removeOldFilesFiles() {
        for (int i = 0; i < actors.length; i++) {
            try {
                Files.delete(actors[i].toPath());
                Files.delete(critics[i].toPath());
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }
    }

    public List<Agent> getAgents() {
        return agents;
    }

    /**
     * Reset environment and get hunter observations
     *
     * @return
     */
    public INDArray[] reset() {
        initRobots();
        return agents.subList(0, 4).stream().map(Agent::getObservation).toArray(INDArray[]::new);
    }

    /**
     * Step through the simulation environment and return the a new observation
     *
     * @param actions
     * @return
     */
    public StepObs step(final Action[] actions, final int step) {
        final Float[] rewards = new Float[4];

        final INDArray[] nextStates = new INDArray[4];

        // Set next action
        for (int i = 0; i < 4; i++) {
            agents.get(i).setAction(actions[i]);
        }
        agents.get(4).setAction(Action.getRandomAction());

        // step agent through environment
        executeAction();

        // Collect the states after the agents have moved
        for (int i = 0; i < 4; i++) {
            nextStates[i] = agents.get(i).getObservation();
            rewards[i] = agents.get(i).getReward(actions[i]);
        }

        final boolean trapped = agents.get(4).isTrapped();

        return new StepObs(nextStates, rewards, trapped);
    }

    private void executeAction() {
        // Step each agent through the world
        try {
            executor.invokeAll(agents);
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    private void initRobots() {
        // initialise the robots from the environment
        for (int i = 0; i < AGENT_COUNT; i++) {
            if (agents.size() < AGENT_COUNT - 1) {
                agents.add(Agent.makeAgent(Agent.HUNTER_STRING, getAndSetHunter(i), DELAY, this,
                        (getMode() == Mode.EVAL || getMode() == Mode.TRAIN_ON) && actors.length > 0 ? actors[i] : null,
                        (getMode() == Mode.EVAL || getMode() == Mode.TRAIN_ON) && critics.length > 0 ? critics[i]
                                : null));
            } else if (agents.size() < AGENT_COUNT) {
                agents.add(Agent.makeAgent(Agent.PREY_STRING, getAndSetPrey(), DELAY, this, null, null));
            }
            // check if agents is on another agent
            do {
                final int randomPosX = agents.get(i).getSimulatedRobot().getRandomPos();
                final int randomPosY = agents.get(i).getSimulatedRobot().getRandomPos();
                agents.get(i).setPose(randomPosX, randomPosY, 0);
            } while (isSamePosition(agents.get(i)));
        }
    }

    private boolean isSamePosition(final Agent agent) {
        return agents.stream().anyMatch(i -> i != agent && agent.getX() == i.getX() && agent.getY() == i.getY());
    }

    /**
     * Get the game mode
     *
     * @return
     */
    public Mode getMode() {
        return mode;
    }

    private void addBoundaries() {
        for (int x = 0; x < grid.length; x++) {
            for (int y = 0; y < grid.length; y++) {
                if (x == 0 || y == 0 || x == GRID_SIZE - 1 || y == GRID_SIZE - 1) {
                    grid[x][y].setCellType(OccupancyType.OBSTACLE);
                }
            }
        }
    }

    /**
     * Set all agent cells to empty
     */
    public void resetGridToEmpty() {
        for (final MyGridCell[] myGridCells : grid) {
            for (final MyGridCell myGridCell : myGridCells) {
                if (myGridCell.getCellType() == OccupancyType.HUNTER
                        || myGridCell.getCellType() == OccupancyType.PREY) {
                    myGridCell.setEmpty();
                }
            }
        }
    }
}
