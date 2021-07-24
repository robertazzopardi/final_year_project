package intelligence.Maddpg;

import org.nd4j.common.primitives.Pair;
import org.nd4j.linalg.activations.IActivation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.lossfunctions.ILossFunction;
import org.nd4j.linalg.lossfunctions.LossUtil;
import org.nd4j.linalg.lossfunctions.impl.LossMCXENT;
import org.nd4j.linalg.ops.transforms.Transforms;
import org.nd4j.shade.jackson.annotation.JsonInclude;
import lombok.EqualsAndHashCode;

/**
 *
 * Custom loss function required for Actor-Critic methods:
 *
 * <pre>
 * L = sum_i advantage_i * log( probability_i ) + entropy( probability )
 * </pre>
 *
 * It is very similar to the Multi-Class Cross Entropy loss function.
 *
 * @author saudet
 * @see LossMCXENT
 */
@EqualsAndHashCode
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Loss implements ILossFunction {

    public static final double BETA = 0.01;

    private INDArray scoreArray(final INDArray labels, final INDArray preOutput,
            final IActivation activationFn, final INDArray mask) {
        final INDArray output = activationFn.getActivation(preOutput.dup(), true).addi(1e-5);
        final INDArray logOutput = Transforms.log(output, true);
        final INDArray entropy = output.muli(logOutput);
        final INDArray scoreArr = logOutput.muli(labels).subi(entropy.muli(BETA));

        if (mask != null) {
            LossUtil.applyMask(scoreArr, mask);
        }
        return scoreArr;
    }

    @Override
    public double computeScore(final INDArray labels, final INDArray preOutput,
            final IActivation activationFn, final INDArray mask, final boolean average) {
        final INDArray scoreArr = scoreArray(labels, preOutput, activationFn, mask);
        final double score = -scoreArr.sumNumber().doubleValue();
        return average ? score / scoreArr.size(0) : score;
    }

    @Override
    public INDArray computeScoreArray(final INDArray labels, final INDArray preOutput,
            final IActivation activationFn, final INDArray mask) {
        final INDArray scoreArr = scoreArray(labels, preOutput, activationFn, mask);
        return scoreArr.sum(1).muli(-1);
    }

    @Override
    public INDArray computeGradient(final INDArray labels, final INDArray preOutput,
            final IActivation activationFn, final INDArray mask) {
        final INDArray output = activationFn.getActivation(preOutput.dup(), true).addi(1e-5);
        final INDArray logOutput = Transforms.log(output, true);
        final INDArray entropyDev = logOutput.addi(1);
        final INDArray dLda = output.rdivi(labels).subi(entropyDev.muli(BETA)).negi();
        final INDArray grad = activationFn.backprop(preOutput, dLda).getFirst();

        if (mask != null) {
            LossUtil.applyMask(grad, mask);
        }
        return grad;
    }

    @Override
    public Pair<Double, INDArray> computeGradientAndScore(final INDArray labels,
            final INDArray preOutput, final IActivation activationFn, final INDArray mask,
            final boolean average) {
        return new Pair<>(computeScore(labels, preOutput, activationFn, mask, average),
                computeGradient(labels, preOutput, activationFn, mask));
    }

    @Override
    public String toString() {
        return "ActorCriticLoss()";
    }

    @Override
    public String name() {
        return toString();
    }
}
