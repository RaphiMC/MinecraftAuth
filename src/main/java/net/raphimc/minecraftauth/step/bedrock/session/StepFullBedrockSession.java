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
package net.raphimc.minecraftauth.step.bedrock.session;

import com.google.gson.JsonObject;
import lombok.EqualsAndHashCode;
import lombok.Value;
import net.raphimc.minecraftauth.step.AbstractStep;
import net.raphimc.minecraftauth.step.SameInputTriMergeStep;
import net.raphimc.minecraftauth.step.TriMergeStep;
import net.raphimc.minecraftauth.step.bedrock.StepMCChain;
import net.raphimc.minecraftauth.step.bedrock.StepPlayFabToken;
import net.raphimc.minecraftauth.step.xbl.StepXblXstsToken;
import org.apache.http.client.HttpClient;

public class StepFullBedrockSession extends SameInputTriMergeStep<StepMCChain.MCChain, StepPlayFabToken.PlayFabToken, StepXblXstsToken.XblXsts<?>, StepFullBedrockSession.FullBedrockSession> {

    public StepFullBedrockSession(final AbstractStep<?, StepMCChain.MCChain> prevStep1, final AbstractStep<?, StepPlayFabToken.PlayFabToken> prevStep2, final AbstractStep<?, ? extends StepXblXstsToken.XblXsts<?>> prevStep3) {
        super("fullBedrockSession", prevStep1, prevStep2, (AbstractStep<?, StepXblXstsToken.XblXsts<?>>) prevStep3);
    }

    @Override
    public FullBedrockSession applyStep(final HttpClient httpClient, final StepMCChain.MCChain mcChain, final StepPlayFabToken.PlayFabToken playFabToken, final StepXblXstsToken.XblXsts<?> realmsXsts) throws Exception {
        return new FullBedrockSession(mcChain, playFabToken, realmsXsts);
    }

    @Override
    public FullBedrockSession fromRawJson(final JsonObject json) {
        final StepMCChain.MCChain mcChain = this.prevStep != null ? this.prevStep.fromJson(json.getAsJsonObject(this.prevStep.name)) : null;
        final StepPlayFabToken.PlayFabToken playFabToken = this.prevStep2 != null ? this.prevStep2.fromJson(json.getAsJsonObject(this.prevStep2.name)) : null;
        final StepXblXstsToken.XblXsts<?> realmsXsts = this.prevStep3 != null ? this.prevStep3.fromJson(json.getAsJsonObject(this.prevStep3.name)) : null;
        return new FullBedrockSession(mcChain, playFabToken, realmsXsts);
    }

    @Override
    public JsonObject toRawJson(final FullBedrockSession fullBedrockSession) {
        final JsonObject json = new JsonObject();
        if (this.prevStep != null) json.add(this.prevStep.name, this.prevStep.toJson(fullBedrockSession.mcChain));
        if (this.prevStep2 != null) json.add(this.prevStep2.name, this.prevStep2.toJson(fullBedrockSession.playFabToken));
        if (this.prevStep3 != null) json.add(this.prevStep3.name, this.prevStep3.toJson(fullBedrockSession.realmsXsts));
        return json;
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class FullBedrockSession extends TriMergeStep.StepResult<StepMCChain.MCChain, StepPlayFabToken.PlayFabToken, StepXblXstsToken.XblXsts<?>> {

        StepMCChain.MCChain mcChain;
        StepPlayFabToken.PlayFabToken playFabToken;
        StepXblXstsToken.XblXsts<?> realmsXsts;

        @Override
        protected StepMCChain.MCChain prevResult() {
            return this.mcChain;
        }

        @Override
        protected StepPlayFabToken.PlayFabToken prevResult2() {
            return this.playFabToken;
        }

        @Override
        protected StepXblXstsToken.XblXsts<?> prevResult3() {
            return this.realmsXsts;
        }

    }

}
