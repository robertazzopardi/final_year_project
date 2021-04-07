package robots;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;
import comp329robosim.SimulatedRobot;
import intelligence.Inteligence;
import intelligence.DeepQLearning.DeepQLearning;
import simulation.Env;
import simulation.Mode;

/**
 *
 */
public class RobotController {

	private final int agents = 4;

	private static final double GAMMA = 0.95;
	private static final double TAU = 1e-3;
	private static final int BATCH_SIZE = 1024;

	private static final int EPISODES_BEFORE_TRAIN = 20;

	private static final int EPISODES_LENGTH = 50;

	private static final int MAX_REPLAY_BUFFER_LEN = BATCH_SIZE * EPISODES_LENGTH;

	private int[] replaySampleIndex;



	// private void experience(final List<Boolean> state, final List<Action> action,
	// final List<Double> reward, final List<Boolean> nextObservation) {
	// memory.add(state, action, reward, nextObservation);
	// }

	// private void preUpdate() {
	// replaySampleIndex = null;
	// }

	// private T update(final int t) {
	// if (memory.getMemoryLength() < MAX_REPLAY_BUFFER_LEN) {
	// return null;
	// }
	// if (t % 100 != 0) {
	// return null;
	// }

	// replaySampleIndex = memory.makeIndex(BATCH_SIZE);

	// List<Boolean> obs = new ArrayList<>();
	// List<Boolean> obsNext = new ArrayList<>();
	// List<Action> act = new ArrayList<>();

	// for (int i = 0; i < hunters.length; i++) {
	// Experience exp = memory.sampleIndex(replaySampleIndex);
	// obs.addAll(exp.obs);
	// obsNext.addAll(exp.nextObs);
	// act.addAll(exp.action);
	// }

	// final Experience exp = memory.sampleIndex(replaySampleIndex);

	// int numSample = 1;
	// double targetQ = 0.0;

	// for (int i = 0; i < numSample; i++) {
	// final List<Action> targetActionNext = IntStream.range(0, hunters.length)
	// .forEach(i -> hunters[i].pDebug.get("targetAction")(obsNextN[i]));
	// final List<Double> targetQNext = qDebug.get("targetQValues");
	// targetQ += rew + GAMMA * (1 - done) * targetQNext;
	// }

	// targetQ /=


	// return null;
	// }


	public static final int STEP_COUNT = 5000;

	// public static final int STATE_COUNT = 20;
	// public static final int STATE_COUNT = 28;
	public static final int STATE_COUNT = 32;

	// public static final int STATE_COUNT = 14;

	public static final int DELAY = 1000;

	private int captures = 0;

	public final Hunter[] hunters = new Hunter[4];

	private Prey prey;

	private final Env env;

	// private final Maddpg maddpg;

	// public final CapturesChart capturesChart;

	private static final int capacity = 1000000;
	private static final int maxEpisode = 500;
	private static final int maxStep = 100;
	private static final int batchSize = 1024;

	private ExecutorService executor = Executors.newFixedThreadPool(4);

	public final Maddpg maddpg;

	class Tmp implements Callable<String> {
		@Override
		public String call() throws Exception {
			System.out.println("3");
			return "egg";
		}
	}

	public RobotController(final Env env) {
		this.env = env;

		// this.capturesChart = new CapturesChart("Average Moves");
		// CapturesChart.startChart(this.capturesChart);

		initRobots();

		// prey.start();
		// startRobots();



		// this.maddpg = new Maddpg(capacity, hunters, this, 500, 100, 32);
		// this.maddpg.run();

		maddpg = new Maddpg(capacity, hunters, this, 500, 10000, 3200);
		new Thread(maddpg).start();
	}

	// public StepObs step(final Action[] actions) {

	// final Double[] rewards = new Double[hunters.length];
	// for (int i = 0; i < actions.length; i++) {
	// hunters[i].resumeRobot();
	// hunters[i].setAction(actions[i]);
	// rewards[i] = hunters[i].getScoreForAction(actions[i]);
	// }

	// final Boolean[][] nextStates = new Boolean[hunters.length][];
	// for (int i = 0; i < hunters.length; i++) {
	// nextStates[i] = hunters[i].createGameObservation();
	// }

	// return new StepObs(nextStates, rewards);
	// }
	public StepObs step(final Action[] actions) {
		final Double[] rewards = new Double[hunters.length];
		for (int i = 0; i < actions.length; i++) {
			hunters[i] = new Hunter(hunters[i], actions[i]);
			rewards[i] = hunters[i].getScoreForAction(actions[i]);
		}

		Boolean[][] nextStates = new Boolean[hunters.length][];
		try {
			List<Future<Boolean[]>> res = executor.invokeAll(Arrays.asList(hunters));
			for (int i = 0; i < res.size(); i++) {
				nextStates[i] = res.get(i).get();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}


		return new StepObs(nextStates, rewards);
	}

	private void initRobots() {
		// initialise the robots from the environment
		final SimulatedRobot preyRobot = env.getAndSetPrey();
		prey = new Prey(preyRobot, DELAY, env, this);

		final Mode mode = env.getMode();

		for (int i = 0; i < hunters.length; i++) {
			do {
				final SimulatedRobot simulatedRobot = env.getAndSetHunter(i);
				Inteligence learning = null;
				switch (mode) {
					case EVAL:
						if (env.getFiles().length == 0) {
							learning = new DeepQLearning(true);
						} else {
							learning = DeepQLearning.loadNetwork(env.getFiles()[i], false, true);
						}
						break;

					case TRAIN_ON:
						if (env.getFiles().length < 4) {
							learning = new DeepQLearning(false);

						} else {
							learning = DeepQLearning.loadNetwork(env.getFiles()[i], true, false);

						}
						break;

					case TRAIN:
						learning = new DeepQLearning(false);
						break;

					default:
						break;
				}
				hunters[i] = new Hunter(simulatedRobot, DELAY, env, learning, this, prey, i);
			} while (isSamePosition(i));
		}

		// for (final Hunter hunter : hunters) {
		// hunter.setOthers(hunters);
		// }
	}

	private boolean isSamePosition(final int i) {
		if (hunters[i].getX() == prey.getX() && hunters[i].getY() == prey.getY()) {
			return true;
		}

		for (int j = 0; j < i; j++) {
			if (hunters[i].getX() == hunters[j].getX() && hunters[i].getY() == hunters[j].getY()) {
				return true;
			}
		}

		return false;
	}

	public void restartRobots() {
		// final SimulatedRobot preyRobot = env.getAndSetPrey();
		// prey = new Prey(preyRobot, DELAY, env, this);

		for (int i = 0; i < 4; i++) {
			do {
				final SimulatedRobot simulatedHunter = env.getAndSetHunter(i);
				hunters[i] = new Hunter(simulatedHunter, DELAY, env, hunters[i].getLearning(), this,
						prey, i);
			} while (isSamePosition(i));
		}

		// for (final Hunter hunter : hunters) {
		// hunter.setOthers(hunters);
		// }

		startRobots();
	}

	public void resumeHunters() {
		for (final Hunter hunter : hunters) {
			hunter.resumeRobot();
		}
	}

	private void startRobots() {
		// prey.start();
		for (final Hunter hunter : hunters) {
			hunter.start();
		}
	}

	public void pauseRobots() {
		for (final Hunter hunter : hunters) {
			hunter.pauseRobot();
		}
	}

	public void stopRobots() {
		// prey.stopRobot();
		for (final Hunter hunter : hunters) {
			hunter.stopRobot();
		}
	}

	// public void saveNetwork() {
	// // pick the network with the highest score
	// final Optional<Hunter> maxHunter = Arrays.stream(hunters)
	// .max(Comparator.comparing(v -> v.getLearning().getNetwork().score()));

	// if (maxHunter.isPresent()) {
	// final DeepQLearning dqn = maxHunter.get().getLearning();
	// // System.out.println(Double.toString(dqn.getNetwork().score()));
	// // for (Hunter hunter : hunters) {
	// // if (!hunter.equals(h)) {
	// // hunter.setLearning(new DeepQLearning(dqn));
	// // }
	// // }
	// if (env.getMode() == Mode.TRAIN) {
	// // DeepQLearning.saveNetwork(hunters[i].getLearning().getNetwork(), i,
	// // Integer.toString(Env.EPISODES));
	// DeepQLearning.saveNetwork(dqn.getNetwork(), 0,
	// Integer.toString(Env.EPISODES));
	// } else if (env.getMode() == Mode.TRAIN_ON) {
	// if (env.getFiles().length != 0) {
	// // final String fileName = env.getFiles()[i].getName();
	// try {
	// Files.delete(env.getFiles()[0].toPath());
	// } catch (IOException e) {
	// e.printStackTrace();
	// }

	// // DeepQLearning.saveNetwork(hunters[i].getLearning().getNetwork(), i,
	// // Integer.toString(env.getEpisode() - 1));
	// DeepQLearning.saveNetwork(dqn.getNetwork(), 0,
	// Integer.toString(env.getEpisode() - 1));

	// } else {
	// // DeepQLearning.saveNetwork(hunters[i].getLearning().getNetwork(), i,
	// // Integer.toString(Env.EPISODES));
	// DeepQLearning.saveNetwork(dqn.getNetwork(), 0,
	// Integer.toString(Env.EPISODES));
	// }
	// }
	// }
	// }

	public void saveNetworks() {
		for (int i = 0; i < hunters.length; i++) {
			if (env.getMode() == Mode.TRAIN) {
				DeepQLearning.saveNetwork(hunters[i].getNetwork(), i,
						Integer.toString(Env.TOTAL_EPISODES));
			} else if (env.getMode() == Mode.TRAIN_ON) {
				if (env.getFiles().length != 0) {
					// final String fileName = env.getFiles()[i].getName();
					try {
						Files.delete(env.getFiles()[i].toPath());
					} catch (final IOException e) {
						e.printStackTrace();
					}

					DeepQLearning.saveNetwork(hunters[i].getNetwork(), i,
							Integer.toString(env.getEpisode() - 1));

				} else {
					DeepQLearning.saveNetwork(hunters[i].getNetwork(), i,
							Integer.toString(Env.TOTAL_EPISODES));
				}
			}
		}
	}

	public void handleCapture(final boolean capture) {
		stopRobots();

		// GridPrinter.printGrid(env.getGrid());

		// Done with current episode, now we can restart the simulation
		try {
			Thread.sleep(2000);
		} catch (final InterruptedException e) {
			Thread.currentThread().interrupt();
		}

		env.resetGrid();

		if (env.getEpisode() <= Env.TOTAL_EPISODES + env.getTrainedEpisodes()) {
			env.updateTitle(
					env.incrementEpisode() + " Captures " + (capture ? ++captures : captures));
			// TODO: add captures to the file name and retrieve
			restartRobots();
		} else if (env.getMode() == Mode.TRAIN || env.getMode() == Mode.TRAIN_ON) {
			// saveNetwork();
			saveNetworks();
			env.stopRunning();
			System.exit(0);
		}
	}
}
