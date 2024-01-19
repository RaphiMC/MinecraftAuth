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
import net.lenni0451.commons.httpclient.HttpClient;
import net.lenni0451.commons.httpclient.content.impl.URLEncodedFormContent;
import net.lenni0451.commons.httpclient.requests.impl.PostRequest;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.responsehandler.MsaResponseHandler;
import net.raphimc.minecraftauth.step.AbstractStep;
import net.raphimc.minecraftauth.util.OAuthEnvironment;

import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class StepMsaDeviceCode extends AbstractStep<StepMsaDeviceCode.MsaDeviceCodeCallback, StepMsaDeviceCode.MsaDeviceCode> {

    private final MsaCodeStep.ApplicationDetails applicationDetails;

    public StepMsaDeviceCode(final MsaCodeStep.ApplicationDetails applicationDetails) {
        super("msaDeviceCode", null);

        this.applicationDetails = applicationDetails;
    }

    @Override
    public MsaDeviceCode applyStep(final HttpClient httpClient, final MsaDeviceCodeCallback msaDeviceCodeCallback) throws Exception {
        MinecraftAuth.LOGGER.info("Getting device code for MSA login...");

        if (msaDeviceCodeCallback == null) {
            throw new IllegalStateException("Missing StepMsaDeviceCode.MsaDeviceCodeCallback input");
        }

        final Map<String, String> postData = new HashMap<>();
        postData.put("client_id", this.applicationDetails.getClientId());
        postData.put("scope", this.applicationDetails.getScope());
        if (this.applicationDetails.getOAuthEnvironment() == OAuthEnvironment.LIVE) {
            postData.put("response_type", "device_code");
        }

        final PostRequest postRequest = new PostRequest(this.applicationDetails.getOAuthEnvironment().getDeviceCodeUrl());
        postRequest.setContent(new URLEncodedFormContent(postData));
        final JsonObject obj = httpClient.execute(postRequest, new MsaResponseHandler());

        final MsaDeviceCode msaDeviceCode = new MsaDeviceCode(
                System.currentTimeMillis() + obj.get("expires_in").getAsLong() * 1000,
                obj.get("interval").getAsLong() * 1000,
                obj.get("device_code").getAsString(),
                obj.get("user_code").getAsString(),
                obj.get("verification_uri").getAsString(),
                this.applicationDetails
        );
        MinecraftAuth.LOGGER.info("Got MSA device code, expires: " + Instant.ofEpochMilli(msaDeviceCode.getExpireTimeMs()).atZone(ZoneId.systemDefault()));
        msaDeviceCodeCallback.callback.accept(msaDeviceCode);
        return msaDeviceCode;
    }

    @Override
    public MsaDeviceCode fromJson(final JsonObject json) {
        return new MsaDeviceCode(
                json.get("expireTimeMs").getAsLong(),
                json.get("intervalMs").getAsLong(),
                json.get("deviceCode").getAsString(),
                json.get("userCode").getAsString(),
                json.get("verificationUrl").getAsString(),
                this.applicationDetails
        );
    }

    @Override
    public JsonObject toJson(final MsaDeviceCode msaDeviceCode) {
        final JsonObject json = new JsonObject();
        json.addProperty("expireTimeMs", msaDeviceCode.expireTimeMs);
        json.addProperty("intervalMs", msaDeviceCode.intervalMs);
        json.addProperty("deviceCode", msaDeviceCode.deviceCode);
        json.addProperty("userCode", msaDeviceCode.userCode);
        json.addProperty("verificationUrl", msaDeviceCode.verificationUri);
        return json;
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class MsaDeviceCode extends AbstractStep.StepResult<MsaCodeStep.ApplicationDetails> {

        long expireTimeMs;
        long intervalMs;
        String deviceCode;
        String userCode;
        String verificationUri;
        MsaCodeStep.ApplicationDetails applicationDetails;

        @Override
        protected MsaCodeStep.ApplicationDetails prevResult() {
            return this.applicationDetails;
        }

        @Override
        public boolean isExpired() {
            return this.expireTimeMs <= System.currentTimeMillis();
        }

        public String getDirectVerificationUri() {
            return this.verificationUri + "?otc=" + this.userCode;
        }

    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class MsaDeviceCodeCallback extends AbstractStep.InitialInput {

        Consumer<MsaDeviceCode> callback;

    }

}
