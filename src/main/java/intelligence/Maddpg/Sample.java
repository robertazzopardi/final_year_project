package intelligence.Maddpg;

import java.util.List;
import org.nd4j.linalg.api.ndarray.INDArray;
import robots.Action;

/**
 * Sample of experiences in the simulation
 */
public class Sample {
    final List<List<INDArray>> obsBatch;
    final List<List<Action>> indivActionBatch;
    final List<List<Float>> indivRewardBatch;
    final List<List<INDArray>> nextObsBatch;
    final List<INDArray[]> globalStateBatch;
    final List<INDArray[]> globalNextStateBatch;
    final List<INDArray> globalActionsBatch;

    public Sample(final List<List<INDArray>> obsBatch, final List<List<Action>> indivActionBatch,
            final List<List<Float>> indivRewardBatch, final List<List<INDArray>> nextObsBatch,
            final List<INDArray[]> globalStateBatch, final List<INDArray[]> globalNextStateBatch,
            final List<INDArray> globalActionsBatch) {
        this.obsBatch = obsBatch;
        this.indivActionBatch = indivActionBatch;
        this.indivRewardBatch = indivRewardBatch;
        this.nextObsBatch = nextObsBatch;
        this.globalStateBatch = globalStateBatch;
        this.globalNextStateBatch = globalNextStateBatch;
        this.globalActionsBatch = globalActionsBatch;
    }
}
