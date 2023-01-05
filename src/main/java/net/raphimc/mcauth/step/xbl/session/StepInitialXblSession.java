package net.raphimc.mcauth.step.xbl.session;

import com.google.gson.JsonObject;
import net.raphimc.mcauth.step.AbstractStep;
import net.raphimc.mcauth.step.OptionalMergeStep;
import net.raphimc.mcauth.step.msa.StepMsaToken;
import net.raphimc.mcauth.step.xbl.StepXblDeviceToken;
import org.apache.http.client.HttpClient;

import java.util.Objects;

public class StepInitialXblSession extends OptionalMergeStep<StepMsaToken.MsaToken, StepXblDeviceToken.XblDeviceToken, StepInitialXblSession.InitialXblSession> {

    public StepInitialXblSession(AbstractStep<?, StepMsaToken.MsaToken> prevStep1, AbstractStep<?, StepXblDeviceToken.XblDeviceToken> prevStep2) {
        super(prevStep1, prevStep2);
    }

    @Override
    public InitialXblSession applyStep(HttpClient httpClient, StepMsaToken.MsaToken prevResult1, StepXblDeviceToken.XblDeviceToken prevResult2) throws Exception {
        return new InitialXblSession(
                prevResult1,
                prevResult2
        );
    }

    @Override
    public InitialXblSession fromJson(JsonObject json) throws Exception {
        final StepMsaToken.MsaToken prev1 = this.prevStep.fromJson(json.getAsJsonObject("prev"));
        final StepXblDeviceToken.XblDeviceToken prev2 = this.prevStep2 != null ? this.prevStep2.fromJson(json.getAsJsonObject("prev2")) : null;
        return new InitialXblSession(
                prev1,
                prev2
        );
    }

    public static final class InitialXblSession implements OptionalMergeStep.StepResult<StepMsaToken.MsaToken, StepXblDeviceToken.XblDeviceToken> {

        private final StepMsaToken.MsaToken prevResult;
        private final StepXblDeviceToken.XblDeviceToken prevResult2;

        public InitialXblSession(StepMsaToken.MsaToken prevResult, StepXblDeviceToken.XblDeviceToken prevResult2) {
            this.prevResult = prevResult;
            this.prevResult2 = prevResult2;
        }

        @Override
        public JsonObject toJson() {
            final JsonObject json = new JsonObject();
            json.add("prev", this.prevResult.toJson());
            if (this.prevResult2 != null) json.add("prev2", this.prevResult2.toJson());
            return json;
        }

        @Override
        public StepMsaToken.MsaToken prevResult() {
            return prevResult;
        }

        @Override
        public StepXblDeviceToken.XblDeviceToken prevResult2() {
            return prevResult2;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            InitialXblSession that = (InitialXblSession) obj;
            return Objects.equals(this.prevResult, that.prevResult) &&
                    Objects.equals(this.prevResult2, that.prevResult2);
        }

        @Override
        public int hashCode() {
            return Objects.hash(prevResult, prevResult2);
        }

        @Override
        public String toString() {
            return "InitialXblSession[" +
                    "prevResult=" + prevResult + ", " +
                    "prevResult2=" + prevResult2 + ']';
        }

    }

}
