/*
 * This file is part of MinecraftAuth - https://github.com/RaphiMC/MinecraftAuth
 * Copyright (C) 2023 RK_01/RaphiMC and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
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
        final StepMsaToken.MsaToken prev1 = this.prevStep != null ? this.prevStep.fromJson(json.getAsJsonObject("prev")) : null;
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
            if (this.prevResult != null) json.add("prev", this.prevResult.toJson());
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
