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
package net.raphimc.minecraftauth.step.msa;

import com.google.gson.JsonObject;
import lombok.Value;
import net.raphimc.minecraftauth.step.AbstractStep;
import org.apache.http.client.HttpClient;

public class MsaCodeStep<I extends AbstractStep.StepResult<?>> extends AbstractStep<I, MsaCodeStep.MsaCode> {

    protected final String clientId;
    protected final String scope;
    protected final String clientSecret;

    public MsaCodeStep(final AbstractStep<?, I> prevStep, final String clientId, final String scope, final String clientSecret) {
        super(prevStep);

        this.clientId = clientId;
        this.scope = scope;
        this.clientSecret = clientSecret;
    }

    @Override
    public MsaCode applyStep(HttpClient httpClient, I prevResult) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public MsaCode fromJson(JsonObject json) {
        return new MsaCode(json.get("code").getAsString(), json.get("clientId").getAsString(), json.get("scope").getAsString(), json.get("clientSecret") != null && !json.get("clientSecret").isJsonNull() ? json.get("clientSecret").getAsString() : null, null);
    }

    @Value
    public static class MsaCode implements AbstractStep.StepResult<AbstractStep.StepResult<?>> {

        String code;
        String clientId;
        String scope;
        String clientSecret;
        String redirectUri;

        @Override
        public StepResult<?> prevResult() {
            return null;
        }

        @Override
        public JsonObject toJson() {
            final JsonObject json = new JsonObject();
            json.addProperty("code", this.code);
            json.addProperty("clientId", this.clientId);
            json.addProperty("scope", this.scope);
            json.addProperty("clientSecret", this.clientSecret);
            return json;
        }

    }

}
