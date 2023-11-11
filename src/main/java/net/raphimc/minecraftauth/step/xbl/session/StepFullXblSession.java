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
package net.raphimc.minecraftauth.step.xbl.session;

import com.google.gson.JsonObject;
import lombok.Value;
import net.raphimc.minecraftauth.step.AbstractStep;
import net.raphimc.minecraftauth.step.SameInputOptionalMergeStep;
import net.raphimc.minecraftauth.step.xbl.StepXblTitleToken;
import net.raphimc.minecraftauth.step.xbl.StepXblUserToken;
import org.apache.http.client.HttpClient;

public class StepFullXblSession extends SameInputOptionalMergeStep<StepXblUserToken.XblUserToken, StepXblTitleToken.XblTitleToken, StepInitialXblSession.InitialXblSession, StepFullXblSession.FullXblSession> {

    public StepFullXblSession(final AbstractStep<StepInitialXblSession.InitialXblSession, StepXblUserToken.XblUserToken> prevStep1, final AbstractStep<StepInitialXblSession.InitialXblSession, StepXblTitleToken.XblTitleToken> prevStep2) {
        super(prevStep1, prevStep2);
    }

    @Override
    public FullXblSession applyStep(final HttpClient httpClient, final StepXblUserToken.XblUserToken xblUserToken, final StepXblTitleToken.XblTitleToken xblTitleToken) throws Exception {
        return new FullXblSession(xblUserToken, xblTitleToken);
    }

    @Override
    public FullXblSession fromDeduplicatedJson(final JsonObject json) {
        final StepXblUserToken.XblUserToken xblUserToken = this.prevStep != null ? this.prevStep.fromJson(json.getAsJsonObject("xblUserToken")) : null;
        final StepXblTitleToken.XblTitleToken xblTitleToken = this.prevStep2 != null ? this.prevStep2.fromJson(json.getAsJsonObject("xblTitleToken")) : null;
        return new FullXblSession(xblUserToken, xblTitleToken);
    }

    @Value
    public static class FullXblSession implements SameInputOptionalMergeStep.StepResult<StepXblUserToken.XblUserToken, StepXblTitleToken.XblTitleToken> {

        StepXblUserToken.XblUserToken xblUserToken;
        StepXblTitleToken.XblTitleToken xblTitleToken;

        @Override
        public StepXblUserToken.XblUserToken prevResult() {
            return this.xblUserToken;
        }

        @Override
        public StepXblTitleToken.XblTitleToken prevResult2() {
            return this.xblTitleToken;
        }

        @Override
        public JsonObject _toJson() {
            final JsonObject json = new JsonObject();
            if (this.xblUserToken != null) json.add("xblUserToken", this.xblUserToken.toJson());
            if (this.xblTitleToken != null) json.add("xblTitleToken", this.xblTitleToken.toJson());
            return json;
        }

    }

}
