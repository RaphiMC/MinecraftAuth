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
import net.raphimc.minecraftauth.responsehandler.MsaResponseHandler;
import net.raphimc.minecraftauth.step.AbstractStep;
import net.raphimc.minecraftauth.util.JsonUtil;
import net.raphimc.minecraftauth.util.logging.ILogger;

import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

public class StepMsaToken extends AbstractStep<MsaCodeStep.MsaCode, StepMsaToken.MsaToken> {

    public StepMsaToken(final AbstractStep<?, MsaCodeStep.MsaCode> prevStep) {
        super("msaToken", prevStep);
    }

    @Override
    protected MsaToken execute(final ILogger logger, final HttpClient httpClient, final MsaCodeStep.MsaCode msaCode) throws Exception {
        if (msaCode.msaToken != null) {
            return msaCode.msaToken;
        }

        return this.execute(logger, httpClient, "authorization_code", msaCode.getCode(), msaCode);
    }

    @Override
    public MsaToken refresh(final ILogger logger, final HttpClient httpClient, final MsaToken msaToken) throws Exception {
        if (!msaToken.isExpired()) {
            return msaToken;
        } else if (msaToken.getRefreshToken() != null) {
            return this.execute(logger, httpClient, "refresh_token", msaToken.getRefreshToken(), msaToken.getMsaCode());
        } else {
            return super.refresh(logger, httpClient, msaToken);
        }
    }

    @Override
    public MsaToken getFromInput(final ILogger logger, final HttpClient httpClient, final InitialInput input) throws Exception {
        if (input instanceof RefreshToken) {
            final RefreshToken refreshToken = (RefreshToken) input;
            return this.execute(logger, httpClient, "refresh_token", refreshToken.getRefreshToken(), new MsaCodeStep.MsaCode(null));
        } else {
            return super.getFromInput(logger, httpClient, input);
        }
    }

    @Override
    public MsaToken fromJson(final JsonObject json) {
        final MsaCodeStep.MsaCode msaCode = this.prevStep != null ? this.prevStep.fromJson(json.getAsJsonObject(this.prevStep.name)) : null;
        return new MsaToken(
                json.get("expireTimeMs").getAsLong(),
                json.get("accessToken").getAsString(),
                JsonUtil.getStringOr(json, "refreshToken", null),
                msaCode
        );
    }

    @Override
    public JsonObject toJson(final MsaToken msaToken) {
        final JsonObject json = new JsonObject();
        json.addProperty("expireTimeMs", msaToken.expireTimeMs);
        json.addProperty("accessToken", msaToken.accessToken);
        json.addProperty("refreshToken", msaToken.refreshToken);
        if (this.prevStep != null) json.add(this.prevStep.name, this.prevStep.toJson(msaToken.msaCode));
        return json;
    }

    private MsaToken execute(final ILogger logger, final HttpClient httpClient, final String type, final String codeOrRefreshToken, final MsaCodeStep.MsaCode msaCode) throws Exception {
        logger.info("Getting MSA Token...");

        final Map<String, String> postData = new HashMap<>();
        postData.put("client_id", this.applicationDetails.getClientId());
        postData.put("scope", this.applicationDetails.getScope());
        if (this.applicationDetails.getClientSecret() != null) {
            postData.put("client_secret", this.applicationDetails.getClientSecret());
        }
        postData.put("grant_type", type);
        if (type.equals("refresh_token")) {
            postData.put("refresh_token", codeOrRefreshToken);
        } else if (type.equals("authorization_code")) {
            postData.put("code", codeOrRefreshToken);
            postData.put("redirect_uri", this.applicationDetails.getRedirectUri());
        } else {
            throw new IllegalArgumentException("Invalid type: " + type);
        }

        final PostRequest postRequest = new PostRequest(this.applicationDetails.getOAuthEnvironment().getTokenUrl());
        postRequest.setContent(new URLEncodedFormContent(postData));
        final JsonObject obj = httpClient.execute(postRequest, new MsaResponseHandler());

        final MsaToken msaToken = new MsaToken(
                System.currentTimeMillis() + obj.get("expires_in").getAsLong() * 1000,
                obj.get("access_token").getAsString(),
                JsonUtil.getStringOr(obj, "refresh_token", null),
                msaCode
        );
        logger.info("Got MSA Token, expires: " + Instant.ofEpochMilli(msaToken.getExpireTimeMs()).atZone(ZoneId.systemDefault()));
        return msaToken;
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class MsaToken extends AbstractStep.StepResult<MsaCodeStep.MsaCode> {

        long expireTimeMs;
        String accessToken;
        String refreshToken;
        MsaCodeStep.MsaCode msaCode;

        @Override
        protected MsaCodeStep.MsaCode prevResult() {
            return this.msaCode;
        }

        @Override
        public boolean isExpired() {
            return this.expireTimeMs <= System.currentTimeMillis();
        }

    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class RefreshToken extends AbstractStep.InitialInput {

        String refreshToken;

    }

}
