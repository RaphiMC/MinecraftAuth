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
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.With;
import net.raphimc.minecraftauth.step.AbstractStep;
import net.raphimc.minecraftauth.util.JsonUtil;
import net.raphimc.minecraftauth.util.OAuthEnvironment;
import net.raphimc.minecraftauth.util.UuidUtil;

public abstract class MsaCodeStep<I extends AbstractStep.StepResult<?>> extends AbstractStep<I, MsaCodeStep.MsaCode> {

    public MsaCodeStep(final AbstractStep<?, I> prevStep) {
        super("msaCode", prevStep);
    }

    @Override
    public MsaCode fromJson(final JsonObject json) {
        return new MsaCode(
                json.get("code").getAsString(),
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
    public JsonObject toJson(final MsaCode msaCode) {
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
    public static class ApplicationDetails extends AbstractStep.FirstStepResult {

        String clientId;
        String scope;
        String clientSecret;
        String redirectUri;
        OAuthEnvironment oAuthEnvironment;

        public boolean isTitleClientId() {
            return !UuidUtil.isDashedUuid(this.clientId);
        }

    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class MsaCode extends AbstractStep.StepResult<ApplicationDetails> {

        String code;
        ApplicationDetails applicationDetails;

        @Override
        protected ApplicationDetails prevResult() {
            return this.applicationDetails;
        }

    }

}
