/*
 * This file is part of MinecraftAuth - https://github.com/RaphiMC/MinecraftAuth
 * Copyright (C) 2022-2024 RK_01/RaphiMC and contributors
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
import lombok.*;
import lombok.experimental.NonFinal;
import lombok.experimental.PackagePrivate;
import net.lenni0451.commons.httpclient.HttpClient;
import net.raphimc.minecraftauth.step.AbstractStep;
import net.raphimc.minecraftauth.util.JsonUtil;
import net.raphimc.minecraftauth.util.OAuthEnvironment;
import net.raphimc.minecraftauth.util.UuidUtil;
import org.jetbrains.annotations.ApiStatus;

import java.util.HashMap;
import java.util.Map;

public abstract class MsaCodeStep<I extends AbstractStep.InitialInput> extends AbstractStep<I, MsaCodeStep.MsaCode> {

    public MsaCodeStep(final AbstractStep<?, I> prevStep) {
        super("msaCode", prevStep);
    }

    @Override
    public final MsaCodeStep.MsaCode refresh(final HttpClient httpClient, final MsaCodeStep.MsaCode result) {
        throw new UnsupportedOperationException("Cannot refresh MsaCodeStep");
    }

    @Override
    public final MsaCode fromJson(final JsonObject json) {
        return new MsaCode(
                JsonUtil.getStringOr(json, "code", null),
                new ApplicationDetails(
                        json.get("clientId").getAsString(),
                        json.get("scope").getAsString(),
                        JsonUtil.getStringOr(json, "clientSecret", null),
                        JsonUtil.getStringOr(json, "redirectUri", null),
                        OAuthEnvironment.valueOf(JsonUtil.getStringOr(json, "oAuthEnvironment", "LIVE"))
                )
        );
    }

    @Override
    public final JsonObject toJson(final MsaCode msaCode) {
        final JsonObject json = new JsonObject();
        json.addProperty("code", msaCode.code);
        json.addProperty("clientId", msaCode.applicationDetails.clientId);
        json.addProperty("scope", msaCode.applicationDetails.scope);
        json.addProperty("clientSecret", msaCode.applicationDetails.clientSecret);
        json.addProperty("redirectUri", msaCode.applicationDetails.redirectUri);
        json.addProperty("oAuthEnvironment", msaCode.applicationDetails.oAuthEnvironment.name());
        return json;
    }

    @Value
    @With
    @EqualsAndHashCode(callSuper = false)
    public static class ApplicationDetails {

        String clientId;
        String scope;
        String clientSecret;
        String redirectUri;
        OAuthEnvironment oAuthEnvironment;

        public boolean isTitleClientId() {
            return !UuidUtil.isDashedUuid(this.clientId);
        }

        public Map<String, String> getOAuthParameters() {
            final Map<String, String> parameters = new HashMap<>();
            parameters.put("client_id", this.clientId);
            parameters.put("scope", this.scope);
            parameters.put("redirect_uri", this.redirectUri);
            parameters.put("response_type", "code");
            parameters.put("response_mode", "query");
            return parameters;
        }

    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class MsaCode extends AbstractStep.FirstStepResult {

        String code;
        ApplicationDetails applicationDetails;

        @ApiStatus.Internal
        @Getter(AccessLevel.NONE)
        @Setter(AccessLevel.NONE)
        @PackagePrivate
        @NonFinal
        @ToString.Exclude
        @EqualsAndHashCode.Exclude
        StepMsaToken.MsaToken msaToken; // Used in device code flow

        public MsaCode(final String code, final ApplicationDetails applicationDetails) {
            this.code = code;
            this.applicationDetails = applicationDetails;
        }

    }

}
