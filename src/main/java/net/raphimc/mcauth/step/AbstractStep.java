package net.raphimc.mcauth.step;

import com.google.gson.JsonObject;
import org.apache.http.client.HttpClient;

public abstract class AbstractStep<I extends AbstractStep.StepResult<?>, O extends AbstractStep.StepResult<?>> {

    public final AbstractStep<?, I> prevStep;

    public AbstractStep(final AbstractStep<?, I> prevStep) {
        this.prevStep = prevStep;
    }

    public abstract O applyStep(final HttpClient httpClient, final I prevResult) throws Exception;

    public O refresh(final HttpClient httpClient, final O result) throws Exception {
        return this.applyStep(httpClient, this.prevStep != null ? this.prevStep.refresh(httpClient, result != null ? (I) result.prevResult() : null) : null);
    }

    public abstract O fromJson(final JsonObject json) throws Exception;

    public interface StepResult<P extends StepResult<?>> {
        P prevResult();

        JsonObject toJson() throws Exception;

        default boolean isExpired() throws Exception {
            return true;
        }
    }

}
