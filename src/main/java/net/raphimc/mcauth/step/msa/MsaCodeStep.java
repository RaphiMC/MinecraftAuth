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
package net.raphimc.mcauth.step.msa;

import com.google.gson.JsonObject;
import net.raphimc.mcauth.step.AbstractStep;
import org.apache.http.client.HttpClient;

import java.util.Objects;

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
    public MsaCode fromJson(JsonObject json) throws Exception {
        return new MsaCode(json.get("code").getAsString(), json.get("clientId").getAsString(), json.get("scope").getAsString(), json.get("clientSecret") != null ? json.get("clientSecret").getAsString() : null, null);
    }

    public static final class MsaCode implements AbstractStep.StepResult<AbstractStep.StepResult<?>> {

        private final String code;
        private final String clientId;
        private final String scope;
        private final String clientSecret;
        private final String redirectUri;

        public MsaCode(String code, String clientId, String scope, String clientSecret, String redirectUri) {
            this.code = code;
            this.clientId = clientId;
            this.scope = scope;
            this.clientSecret = clientSecret;
            this.redirectUri = redirectUri;
        }

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

        public String code() {
            return code;
        }

        public String clientId() {
            return clientId;
        }

        public String scope() {
            return scope;
        }

        public String clientSecret() {
            return clientSecret;
        }

        public String redirectUri() {
            return redirectUri;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MsaCode msaCode = (MsaCode) o;
            return Objects.equals(code, msaCode.code) && Objects.equals(clientId, msaCode.clientId) && Objects.equals(scope, msaCode.scope) && Objects.equals(clientSecret, msaCode.clientSecret) && Objects.equals(redirectUri, msaCode.redirectUri);
        }

        @Override
        public int hashCode() {
            return Objects.hash(code, clientId, scope, clientSecret, redirectUri);
        }

        @Override
        public String toString() {
            return "MsaCode{" +
                    "code='" + code + '\'' +
                    ", clientId='" + clientId + '\'' +
                    ", scope='" + scope + '\'' +
                    ", clientSecret='" + clientSecret + '\'' +
                    ", redirectUri='" + redirectUri + '\'' +
                    '}';
        }

    }

}
