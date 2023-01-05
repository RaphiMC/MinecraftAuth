package net.raphimc.mcauth.step;

import org.apache.http.client.HttpClient;

public abstract class OptionalMergeStep<I1 extends AbstractStep.StepResult<?>, I2 extends AbstractStep.StepResult<?>, O extends OptionalMergeStep.StepResult<?, ?>> extends AbstractStep<I1, O> {

    public final AbstractStep<?, I2> prevStep2;

    public OptionalMergeStep(final AbstractStep<?, I1> prevStep1, final AbstractStep<?, I2> prevStep2) {
        super(prevStep1);

        this.prevStep2 = prevStep2;
    }

    @Override
    public O applyStep(HttpClient httpClient, I1 prevResult) throws Exception {
        return this.applyStep(httpClient, prevResult, null);
    }

    public abstract O applyStep(final HttpClient httpClient, final I1 prevResult1, final I2 prevResult2) throws Exception;

    @Override
    public O refresh(final HttpClient httpClient, final O result) throws Exception {
        final I1 prevResult1 = this.prevStep.refresh(httpClient, result != null ? (I1) result.prevResult() : null);
        final I2 prevResult2 = this.prevStep2 != null ? this.prevStep2.refresh(httpClient, result != null ? (I2) result.prevResult2() : null) : null;
        return this.applyStep(httpClient, prevResult1, prevResult2);
    }

    public interface StepResult<P1 extends AbstractStep.StepResult<?>, P2 extends AbstractStep.StepResult<?>> extends AbstractStep.StepResult<P1> {
        P2 prevResult2();

        @Override
        default boolean isExpired() throws Exception {
            return AbstractStep.StepResult.super.isExpired() || (this.prevResult2() != null && this.prevResult2().isExpired());
        }
    }

}
