/*
 * This file is part of MinecraftAuth - https://github.com/RaphiMC/MinecraftAuth
 * Copyright (C) 2022-2025 RK_01/RaphiMC and contributors
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
import net.raphimc.minecraftauth.responsehandler.MsaResponseHandler;
import net.raphimc.minecraftauth.step.AbstractStep;
import net.raphimc.minecraftauth.util.OAuthEnvironment;
import net.raphimc.minecraftauth.util.logging.ILogger;

import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class StepMsaDeviceCode extends InitialPreparationStep<StepMsaDeviceCode.MsaDeviceCodeCallback, StepMsaDeviceCode.MsaDeviceCode> {

    public StepMsaDeviceCode(final ApplicationDetails applicationDetails) {
        super("msaDeviceCode", applicationDetails);
    }

    @Override
    protected MsaDeviceCode execute(final ILogger logger, final HttpClient httpClient, final MsaDeviceCodeCallback msaDeviceCodeCallback) throws Exception {
        logger.info(this, "Getting device code for MSA login...");

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
                obj.get("verification_uri").getAsString()
        );
        logger.info(this, "Got MSA device code, expires: " + Instant.ofEpochMilli(msaDeviceCode.getExpireTimeMs()).atZone(ZoneId.systemDefault()));
        msaDeviceCodeCallback.callback.accept(msaDeviceCode);
        return msaDeviceCode;
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class MsaDeviceCode extends AbstractStep.InitialInput {

        long expireTimeMs;
        long intervalMs;
        String deviceCode;
        String userCode;
        String verificationUri;

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
