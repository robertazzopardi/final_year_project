package intelligence.Maddpg;

import java.util.List;

import org.nd4j.linalg.api.ndarray.INDArray;

/**
 * Data to be trained on buy the Actor and Critic models
 */
public class Data {
    public final List<Float> indivRewardBatchI;
    public final INDArray obsBatchI;
    public final List<INDArray[]> globalStateBatch;
    public final List<INDArray> globalActionsBatch;
    public final List<INDArray[]> globalNextStateBatch;

    public Data(final List<Float> indivRewardBatchI, final INDArray obsBatchI, final List<INDArray[]> globalStateBatch,
            final List<INDArray> globalActionsBatch, final List<INDArray[]> globalNextStateBatch) {
        this.indivRewardBatchI = indivRewardBatchI;
        this.obsBatchI = obsBatchI;
        this.globalStateBatch = globalStateBatch;
        this.globalActionsBatch = globalActionsBatch;
        this.globalNextStateBatch = globalNextStateBatch;
    }
}
