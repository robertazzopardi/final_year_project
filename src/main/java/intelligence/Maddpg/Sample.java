package intelligence.Maddpg;

import java.util.List;
import robots.Action;

/**
 * Sample of experiences in the simulation
 */
public class Sample {
    final List<List<Boolean[]>> obsBatch;
    final List<List<Action>> indivActionBatch;
    final List<List<Float>> indivRewardBatch;
    final List<List<Boolean[]>> nextObsBatch;
    final List<Boolean[]> globalStateBatch;
    final List<Boolean[]> globalNextStateBatch;
    final List<Action[]> globalActionsBatch;
    final List<Integer> doneBatch;

    public Sample(final List<List<Boolean[]>> obsBatch, final List<List<Action>> indivActionBatch,
            final List<List<Float>> indivRewardBatch, final List<List<Boolean[]>> nextObsBatch,
            final List<Boolean[]> globalStateBatch, final List<Boolean[]> globalNextStateBatch,
            final List<Action[]> globalActionsBatch, final List<Integer> doneBatch) {
        this.obsBatch = obsBatch;
        this.indivActionBatch = indivActionBatch;
        this.indivRewardBatch = indivRewardBatch;
        this.nextObsBatch = nextObsBatch;
        this.globalStateBatch = globalStateBatch;
        this.globalNextStateBatch = globalNextStateBatch;
        this.globalActionsBatch = globalActionsBatch;
        this.doneBatch = doneBatch;
    }
}
