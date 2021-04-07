package intelligence.Maddpg;

import java.util.List;
import robots.Action;

public class Sample {
    public final List<List<Boolean>> obsBatch;
    public final List<List<Action>> indivActionBatch;
    public final List<List<Double>> indivRewardBatch;
    public final List<List<Boolean>> nextObsBatch;
    public final List<Boolean[]> globalStateBatch;
    public final List<Boolean[]> globalNextStateBatch;
    public final List<Action> globalActionsBatch;

    public Sample(final List<List<Boolean>> obsBatch, final List<List<Action>> indivActionBatch,
            final List<List<Double>> indivRewardBatch, final List<List<Boolean>> nextObsBatch,
            final List<Boolean[]> globalStateBatch, final List<Boolean[]> globalNextStateBatch,
            final List<Action> globalActionsBatch) {
        this.obsBatch = obsBatch;
        this.indivActionBatch = indivActionBatch;
        this.indivRewardBatch = indivRewardBatch;
        this.nextObsBatch = nextObsBatch;
        this.globalStateBatch = globalStateBatch;
        this.globalNextStateBatch = globalNextStateBatch;
        this.globalActionsBatch = globalActionsBatch;
    }

}
