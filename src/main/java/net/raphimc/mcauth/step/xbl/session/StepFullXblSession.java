package net.raphimc.mcauth.step.xbl.session;

import com.google.gson.JsonObject;
import net.raphimc.mcauth.step.AbstractStep;
import net.raphimc.mcauth.step.SameInputOptionalMergeStep;
import net.raphimc.mcauth.step.xbl.StepXblTitleToken;
import net.raphimc.mcauth.step.xbl.StepXblUserToken;
import org.apache.http.client.HttpClient;

import java.util.Objects;

public class StepFullXblSession extends SameInputOptionalMergeStep<StepXblUserToken.XblUserToken, StepXblTitleToken.XblTitleToken, StepInitialXblSession.InitialXblSession, StepFullXblSession.FullXblSession> {

    public StepFullXblSession(AbstractStep<StepInitialXblSession.InitialXblSession, StepXblUserToken.XblUserToken> prevStep1, AbstractStep<StepInitialXblSession.InitialXblSession, StepXblTitleToken.XblTitleToken> prevStep2) {
        super(prevStep1, prevStep2);
    }

    @Override
    public FullXblSession applyStep(HttpClient httpClient, StepXblUserToken.XblUserToken prevResult1, StepXblTitleToken.XblTitleToken prevResult2) throws Exception {
        return new FullXblSession(
                prevResult1,
                prevResult2
        );
    }

    @Override
    public FullXblSession fromJson(JsonObject json) throws Exception {
        final StepXblUserToken.XblUserToken prev1 = this.prevStep.fromJson(json.getAsJsonObject("prev"));
        final StepXblTitleToken.XblTitleToken prev2 = this.prevStep2 != null ? this.prevStep2.fromJson(json.getAsJsonObject("prev2")) : null;
        return new FullXblSession(
                prev1,
                prev2
        );
    }

    public static final class FullXblSession implements SameInputOptionalMergeStep.StepResult<StepXblUserToken.XblUserToken, StepXblTitleToken.XblTitleToken> {

        private final StepXblUserToken.XblUserToken prevResult;
        private final StepXblTitleToken.XblTitleToken prevResult2;

        public FullXblSession(StepXblUserToken.XblUserToken prevResult, StepXblTitleToken.XblTitleToken prevResult2) {
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
        public StepXblUserToken.XblUserToken prevResult() {
            return prevResult;
        }

        @Override
        public StepXblTitleToken.XblTitleToken prevResult2() {
            return prevResult2;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            FullXblSession that = (FullXblSession) obj;
            return Objects.equals(this.prevResult, that.prevResult) &&
                    Objects.equals(this.prevResult2, that.prevResult2);
        }

        @Override
        public int hashCode() {
            return Objects.hash(prevResult, prevResult2);
        }

        @Override
        public String toString() {
            return "FullXblSession[" +
                    "prevResult=" + prevResult + ", " +
                    "prevResult2=" + prevResult2 + ']';
        }

    }

}
