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
import lombok.EqualsAndHashCode;
import lombok.Value;
import net.raphimc.minecraftauth.step.AbstractStep;
import net.raphimc.minecraftauth.step.OptionalMergeStep;
import net.raphimc.minecraftauth.step.msa.StepMsaToken;
import net.raphimc.minecraftauth.step.xbl.StepXblDeviceToken;
import org.apache.http.client.HttpClient;

public class StepInitialXblSession extends OptionalMergeStep<StepMsaToken.MsaToken, StepXblDeviceToken.XblDeviceToken, StepInitialXblSession.InitialXblSession> {

    public StepInitialXblSession(final AbstractStep<?, StepMsaToken.MsaToken> prevStep1, final AbstractStep<?, StepXblDeviceToken.XblDeviceToken> prevStep2) {
        super("initialXblSession", prevStep1, prevStep2);
    }

    @Override
    public InitialXblSession applyStep(final HttpClient httpClient, final StepMsaToken.MsaToken msaToken, final StepXblDeviceToken.XblDeviceToken xblDeviceToken) throws Exception {
        return new InitialXblSession(msaToken, xblDeviceToken);
    }

    @Override
    public InitialXblSession fromJson(final JsonObject json) {
        final StepMsaToken.MsaToken msaToken = this.prevStep != null ? this.prevStep.fromJson(json.getAsJsonObject(this.prevStep.name)) : null;
        final StepXblDeviceToken.XblDeviceToken xblDeviceToken = this.prevStep2 != null ? this.prevStep2.fromJson(json.getAsJsonObject(this.prevStep2.name)) : null;
        return new InitialXblSession(msaToken, xblDeviceToken);
    }

    @Override
    public JsonObject toJson(final InitialXblSession result) {
        final JsonObject json = new JsonObject();
        if (this.prevStep != null) json.add(this.prevStep.name, this.prevStep.toJson(result.msaToken));
        if (this.prevStep2 != null) json.add(this.prevStep2.name, this.prevStep2.toJson(result.xblDeviceToken));
        return json;
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class InitialXblSession extends OptionalMergeStep.StepResult<StepMsaToken.MsaToken, StepXblDeviceToken.XblDeviceToken> {

        StepMsaToken.MsaToken msaToken;
        StepXblDeviceToken.XblDeviceToken xblDeviceToken;

        @Override
        protected StepMsaToken.MsaToken prevResult() {
            return this.msaToken;
        }

        @Override
        protected StepXblDeviceToken.XblDeviceToken prevResult2() {
            return this.xblDeviceToken;
        }

    }

}
