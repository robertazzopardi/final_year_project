package intelligence.Maddpg;

import java.util.List;
import robots.Action;

/**
 * Sample of experiences in the simulation
 */
public class Sample {
    final List<List<Boolean[]>> obsBatch;
    final List<List<Action>> indivActionBatch;
    final List<List<Double>> indivRewardBatch;
    final List<List<Boolean[]>> nextObsBatch;

    final List<Boolean[]> globalStateBatch;
    final List<Boolean[]> globalNextStateBatch;
    final List<Action[]> globalActionsBatch;
    final List<Integer> doneBatch;

    public Sample(final List<List<Boolean[]>> obsBatch, final List<List<Action>> indivActionBatch,
            final List<List<Double>> indivRewardBatch, final List<List<Boolean[]>> nextObsBatch,
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

    // public final INDArray obsBatch;
    // public final INDArray indivActionBatch;
    // public final INDArray indivRewardBatch;
    // public final INDArray nextObsBatch;
    // public final INDArray globalStateBatch;
    // public final INDArray globalNextStateBatch;
    // public final INDArray globalActionsBatch;
    // public final INDArray doneBatch;

    // public Sample(final INDArray obsBatch, final INDArray indivActionBatch,
    // final INDArray indivRewardBatch, final INDArray nextObsBatch,
    // final INDArray globalStateBatch, final INDArray globalNextStateBatch,
    // final INDArray globalActionsBatch, final INDArray doneBatch) {
    // this.obsBatch = obsBatch;
    // this.indivActionBatch = indivActionBatch;
    // this.indivRewardBatch = indivRewardBatch;
    // this.nextObsBatch = nextObsBatch;
    // this.globalStateBatch = globalStateBatch;
    // this.globalNextStateBatch = globalNextStateBatch;
    // this.globalActionsBatch = globalActionsBatch;
    // this.doneBatch = doneBatch;
    // }

}
