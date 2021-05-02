package intelligence.Maddpg;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import robots.Action;
import simulation.Env;

/**
 * Collection of the previous experience in the simulation
 */
public class ReplayBuffer implements Serializable {
    private static final Logger LOG = LoggerFactory.getLogger(ReplayBuffer.class.getName());
    private static final String SERIALISED_NAME = Env.OUTPUT_FOLDER + "/replayBuffer.ser";
    // private final Set<Experience> buffer;
    private final List<Experience> buffer;

    public ReplayBuffer() {
        // this.buffer = new HashSet<>();
        this.buffer = new ArrayList<>();
    }

    /**
     * Save the state of the buffer
     *
     * @param replayBuffer
     */
    // public static void serialiseBuffer(final ReplayBuffer replayBuffer) {
    // try (BufferedOutputStream fileOut = new BufferedOutputStream(new
    // FileOutputStream(SERIALISED_NAME));
    // ObjectOutputStream out = new ObjectOutputStream(fileOut);) {
    // out.writeObject(replayBuffer);
    // LOG.info("Serialized data saved");
    // } catch (final IOException i) {
    // i.printStackTrace();
    // }
    // }

    /**
     * Load the saved state of the replay buffer from previous runs
     *
     * @return
     */
    // public static ReplayBuffer deserialiseBuffer() {
    // try (BufferedInputStream fileIn = new BufferedInputStream(new
    // FileInputStream(SERIALISED_NAME));
    // ObjectInputStream in = new ObjectInputStream(fileIn);) {
    // final ReplayBuffer replayBuffer = (ReplayBuffer) in.readObject();
    // final String logInfo = "Loaded Saved ReplayBuffer of length " +
    // replayBuffer.getLength();
    // LOG.info(logInfo);
    // return replayBuffer;
    // } catch (final IOException i) {
    // // i.printStackTrace();
    // LOG.error("IOException");
    // } catch (final ClassNotFoundException c) {
    // LOG.error("ReplayBuffer class not found");
    // // c.printStackTrace();
    // }
    // LOG.info("Loading new replay buffer");
    // return new ReplayBuffer();
    // }

    /**
     * Add new experiences to the memory
     *
     * @param state
     * @param action
     * @param reward
     * @param nextState
     * @param dones
     */
    public void push(final INDArray[] state, final Action[] action, final Float[] rewards, final INDArray[] nextState) {
        if (!buffer.add(new Experience(state, action, rewards, nextState)))
            ;
        // LOG.info("Did not add experience to replay");
    }

    /**
     * Get a sample from the memory of size batchSize
     *
     * @param batchSize
     * @return Sample
     */
    public Sample sample(final int batchSize) {
        final List<List<INDArray>> obsBatch = new ArrayList<>(
                Arrays.asList(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>()));
        final List<List<Action>> indivActionBatch = new ArrayList<>(
                Arrays.asList(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>()));
        final List<List<Float>> indivRewardBatch = new ArrayList<>(
                Arrays.asList(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>()));
        final List<List<INDArray>> nextObsBatch = new ArrayList<>(
                Arrays.asList(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>()));

        final List<INDArray[]> globalStateBatch = new ArrayList<>();
        final List<INDArray[]> globalNextStateBatch = new ArrayList<>();
        final List<INDArray> globalActionsBatch = new ArrayList<>();

        final List<Experience> batch = randomSample(batchSize);

        for (final Experience experience : batch) {
            final INDArray[] state = experience.state;
            final Action[] action = experience.action;
            final Float[] reward = experience.reward;
            final INDArray[] nextState = experience.nextState;

            for (int i = 0; i < Env.AGENT_COUNT - 1; i++) {
                obsBatch.get(i).add(state[i]);
                indivActionBatch.get(i).add(action[i]);
                indivRewardBatch.get(i).add(reward[i]);
                nextObsBatch.get(i).add(nextState[i]);
            }

            globalStateBatch.add(state);
            globalActionsBatch.add(
                    Nd4j.createFromArray(Arrays.stream(action).map(Action::getActionIndexFloat).toArray(Float[]::new)));
            globalNextStateBatch.add(nextState);
        }

        final INDArray[] tmp = obsBatch.stream().map(Nd4j::vstack).toArray(INDArray[]::new);
        final INDArray[] tmp2 = nextObsBatch.stream().map(Nd4j::vstack).toArray(INDArray[]::new);

        return new Sample(tmp, indivActionBatch, indivRewardBatch, tmp2, globalStateBatch, globalNextStateBatch,
                globalActionsBatch);
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
     * @param n
     * @return
     */
    public List<Experience> randomSample(final int n) {
        final List<Experience> copy = new ArrayList<>(buffer);
        Collections.shuffle(copy);
        final int size = copy.size();
        return n > size ? copy.subList(0, size) : copy.subList(0, n);
    }

}
