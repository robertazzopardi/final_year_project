package intelligence.Maddpg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.NDArrayIndex;
import robots.Action;

/**
 * Collection of the previous experience in the simulation
 */
public class ReplayBuffer {
    // private static final Random RAND = new Random();

    private static final int NUM_AGENTS = 4;

    private final List<Experience> buffer;

    public ReplayBuffer(final int maxSize) {
        this.buffer = new ArrayList<>(maxSize);
    }

    /**
     * Add new experiences to the memory
     *
     * @param state
     * @param action
     * @param reward
     * @param nextState
     * @param dones
     */
    public void push(final Boolean[][] state, final Action[] action, final Double[] rewards,
            final Boolean[][] nextState, final Integer[] dones) {
        buffer.add(new Experience(state, action, rewards, nextState, dones));
    }

    /**
     * Get a sample from the memory of size batchSize
     *
     * @param batchSize
     * @return
     */
    public Sample sample(final int batchSize) {
        final List<List<Boolean[]>> obsBatch = new ArrayList<>(Arrays.asList(new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>()));
        final List<List<Action>> indivActionBatch = new ArrayList<>(Arrays.asList(new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>()));
        final List<List<Double>> indivRewardBatch = new ArrayList<>(Arrays.asList(new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>()));
        final List<List<Boolean[]>> nextObsBatch = new ArrayList<>(Arrays.asList(new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>()));

        final List<Boolean[]> globalStateBatch = new ArrayList<>();
        final List<Boolean[]> globalNextStateBatch = new ArrayList<>();
        final List<Action[]> globalActionsBatch = new ArrayList<>();
        final List<Integer> doneBatch = new ArrayList<>();

        final List<Experience> batch = randomSample(batchSize);

        for (final Experience experience : batch) {
            final Boolean[][] state = experience.state;
            final Action[] action = experience.action;
            final Double[] reward = experience.reward;
            final Boolean[][] nextState = experience.nextState;
            final Integer[] done = experience.dones;

            for (int i = 0; i < NUM_AGENTS; i++) {
                final Boolean[] obsI = state[i];
                final Action actionI = action[i];
                final Double rewardI = reward[i];
                final Boolean[] nextObsI = nextState[i];

                // System.out.println(obsI.length);

                obsBatch.get(i).add(obsI);
                indivActionBatch.get(i).add(actionI);
                indivRewardBatch.get(i).add(rewardI);
                nextObsBatch.get(i).add(nextObsI);
            }

            // globalStateBatch.addAll(
            // Arrays.asList(Stream.of(state).flatMap(Stream::of).toArray(Boolean[]::new)));
            // globalActionsBatch.addAll(Arrays.asList(action));
            // globalNextStateBatch.addAll(Arrays
            // .asList(Stream.of(nextState).flatMap(Stream::of).toArray(Boolean[]::new)));
            // doneBatch.addAll(Arrays.asList(done));

            globalStateBatch.add(Stream.of(state).flatMap(Stream::of).toArray(Boolean[]::new));
            globalActionsBatch.add(action);
            globalNextStateBatch
                    .add(Stream.of(nextState).flatMap(Stream::of).toArray(Boolean[]::new));
            doneBatch.addAll(Arrays.asList(done));

        }

        return new Sample(obsBatch, indivActionBatch, indivRewardBatch, nextObsBatch,
                globalStateBatch, globalNextStateBatch, globalActionsBatch, doneBatch);
    }

    public int getLength() {
        return buffer.size();
    }

    /**
     * Generate a random sample from a list
     *
     * Based on
     * https://stackoverflow.com/questions/8378752/pick-multiple-random-elements-from-a-list-in-java
     *
     * @param buff
     * @param n
     * @return
     */
    public List<Experience> randomSample(final int n) {
        final List<Experience> copy = new ArrayList<>(buffer);
        Collections.shuffle(copy);
        return n > copy.size() ? copy.subList(0, copy.size()) : copy.subList(0, n);
    }

    /**
     * https://www.javamex.com/tutorials/random_numbers/random_sample.shtml
     *
     * @param population
     * @param nSamplesNeeded
     * @return
     */
    // public static Experience[] pickSample(final Experience[] population, int nSamplesNeeded) {
    // final Experience[] ret = (Experience[]) Array
    // .newInstance(population.getClass().getComponentType(), nSamplesNeeded);
    // int nPicked = 0, i = 0, nLeft = population.length;
    // while (nSamplesNeeded > 0) {
    // final int rand = RAND.nextInt(nLeft);
    // if (rand < nSamplesNeeded) {
    // ret[nPicked++] = population[i];
    // nSamplesNeeded--;
    // }
    // nLeft--;
    // i++;
    // }
    // return ret;
    // }

}
