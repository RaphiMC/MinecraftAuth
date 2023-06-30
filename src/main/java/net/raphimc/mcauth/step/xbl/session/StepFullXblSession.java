/*
 * This file is part of MinecraftAuth - https://github.com/RaphiMC/MinecraftAuth
 * Copyright (C) 2023 RK_01/RaphiMC and contributors
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
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
        final StepXblUserToken.XblUserToken prev1 = this.prevStep != null ? this.prevStep.fromJson(json.getAsJsonObject("prev")) : null;
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
            if (this.prevResult != null) json.add("prev", this.prevResult.toJson());
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
