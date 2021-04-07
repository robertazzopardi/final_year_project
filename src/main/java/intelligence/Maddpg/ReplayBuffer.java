package intelligence.Maddpg;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import robots.Action;

public class ReplayBuffer {
    private static final Random RAND = new Random();

    private static final int NUM_AGENTS = 4;

    private final int maxSize;

    private final Experience[] buffer;

    private int index = 0;

    public ReplayBuffer(final int maxSize) {
        this.maxSize = maxSize;
        this.buffer = new Experience[maxSize];
    }



    public void push(final Boolean[][] state, final Action[] action, final Double[] reward,
            final Boolean[][] nextState) {
        buffer[index++] = new Experience(state, action, reward, nextState);
    }

    public Sample sample(final int batchSize) {
        final List<List<Boolean>> obsBatch = new ArrayList<>(NUM_AGENTS);
        final List<List<Action>> indivActionBatch = new ArrayList<>(NUM_AGENTS);
        final List<List<Double>> indivRewardBatch = new ArrayList<>(NUM_AGENTS);
        final List<List<Boolean>> nextObsBatch = new ArrayList<>(NUM_AGENTS);

        final List<Boolean[]> globalStateBatch = new ArrayList<>();
        final List<Boolean[]> globalNextStateBatch = new ArrayList<>();
        final List<Action> globalActionsBatch = new ArrayList<>();

        final Experience[] batch = pickSample(this.buffer, batchSize);

        for (Experience experience : batch) {
            for (int i = 0; i < NUM_AGENTS; i++) {
                final Boolean[] obsI = experience.state[i];
                final Action actionI = experience.action[i];
                final Double rewardI = experience.reward[i];
                final Boolean[] nextObsI = experience.nextState[i];

                obsBatch.get(i).addAll(Arrays.asList(obsI));
                indivActionBatch.get(i).add(actionI);
                indivRewardBatch.get(i).add(rewardI);
                nextObsBatch.get(i).addAll(Arrays.asList(nextObsI));
            }

            // globalStateBatch.add(Arrays.stream(experience.state)
            // .map(j -> Arrays.stream(j).map(x -> x)).toArray(Boolean[]::new));

            // TODO: add globalStates
        }

        return new Sample(obsBatch, indivActionBatch, indivRewardBatch, nextObsBatch,
                globalStateBatch, globalNextStateBatch, globalActionsBatch);
    }

    public int getLength() {
        return index;
    }

    /**
     * https://www.javamex.com/tutorials/random_numbers/random_sample.shtml
     *
     * @param population
     * @param nSamplesNeeded
     * @return
     */
    public static Experience[] pickSample(final Experience[] population, int nSamplesNeeded) {
        final Experience[] ret = (Experience[]) Array
                .newInstance(population.getClass().getComponentType(), nSamplesNeeded);
        int nPicked = 0, i = 0, nLeft = population.length;
        while (nSamplesNeeded > 0) {
            final int rand = RAND.nextInt(nLeft);
            if (rand < nSamplesNeeded) {
                ret[nPicked++] = population[i];
                nSamplesNeeded--;
            }
            nLeft--;
            i++;
        }
        return ret;
    }

    // private final List<Experience> memory;

    // private int index;

    // private final int capacity;

    // public ReplayBuffer(final int capacity) {
    // memory = new ArrayList<>();
    // this.capacity = capacity;
    // index = 0;
    // }

    // public int getMemoryLength() {
    // return memory.size();
    // }

    // public void clear() {
    // memory.clear();
    // index = 0;
    // }

    // public void add(final List<Boolean> state, final List<Action> action,
    // final List<Double> reward, final List<Boolean> nextObservation) {
    // final Experience data = new Experience(state, action, reward, nextObservation);

    // if (index >= memory.size()) {
    // memory.add(data);
    // } else {
    // memory.set(index, data);
    // }
    // index = (index + 1) % capacity;
    // }

    // public Experience encodeSample(final int[] idxes) {
    // final List<Boolean> obs = new ArrayList<>();
    // final List<Action> action = new ArrayList<>();
    // final List<Double> reward = new ArrayList<>();
    // final List<Boolean> nextObs = new ArrayList<>();

    // for (final int i : idxes) {
    // final Experience data = memory.get(i);
    // obs.addAll(data.obs);
    // action.addAll(data.action);
    // reward.addAll(data.reward);
    // nextObs.addAll(data.nextObs);
    // }

    // return new Experience(obs, action, reward, nextObs);
    // }

    // public int[] makeIndex(final int batchSize) {
    // final int[] tmp = new int[batchSize];
    // for (int i = 0; i < batchSize; i++)
    // tmp[i] = ThreadLocalRandom.current().nextInt(0, memory.size() - 1);
    // return tmp;
    // }

    // public int[] makeLatestIndex(final int batchSize) {
    // final List<Integer> tmp = new ArrayList<>();
    // for (int i = 0; i < batchSize; i++)
    // tmp.add((index - 1 - i) % capacity);
    // Collections.shuffle(tmp);
    // return tmp.stream().mapToInt(i -> i).toArray();
    // }

    // public Experience sampleIndex(final int[] idxes) {
    // return encodeSample(idxes);
    // }

    // public Experience sample(final int batchSize) {
    // int[] idxes;
    // if (batchSize > 0) {
    // idxes = makeIndex(batchSize);
    // } else {
    // idxes = IntStream.range(0, memory.size()).toArray();
    // }
    // return encodeSample(idxes);
    // }

    // public Experience collect() {
    // return sample(-1);
    // }

    // // public T sample() {
    // // final List mem = memory;
    // // Collections.shuffle(mem);
    // // return mem.get(0);
    // // }

}
