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
import com.google.gson.JsonParser;
import lombok.EqualsAndHashCode;
import lombok.Value;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.responsehandler.MsaResponseHandler;
import net.raphimc.minecraftauth.step.AbstractStep;
import net.raphimc.minecraftauth.util.UuidUtil;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

public class StepMsaToken extends AbstractStep<MsaCodeStep.MsaCode, StepMsaToken.MsaToken> {

    public static final String TOKEN_URL = "https://login.live.com/oauth20_token.srf";

    public StepMsaToken(final AbstractStep<?, MsaCodeStep.MsaCode> prevStep) {
        super("msaToken", prevStep);
    }

    @Override
    public MsaToken applyStep(final HttpClient httpClient, final MsaCodeStep.MsaCode msaCode) throws Exception {
        return this.apply(httpClient, msaCode.getCode(), msaCode.getRedirectUri() != null ? "authorization_code" : "refresh_token", msaCode);
    }

    @Override
    public MsaToken refresh(final HttpClient httpClient, final MsaToken msaToken) throws Exception {
        if (msaToken.isExpired()) return this.apply(httpClient, msaToken.getRefreshToken(), "refresh_token", msaToken.getMsaCode());

        return msaToken;
    }

    @Override
    public MsaToken fromJson(final JsonObject json) {
        final MsaCodeStep.MsaCode msaCode = this.prevStep != null ? this.prevStep.fromJson(json.getAsJsonObject("msaCode")) : null;
        return new MsaToken(
                json.get("userId").getAsString(),
                json.get("expireTimeMs").getAsLong(),
                json.get("accessToken").getAsString(),
                json.get("refreshToken").getAsString(),
                msaCode
        );
    }

    @Override
    public JsonObject toJson(final MsaToken msaToken) {
        final JsonObject json = new JsonObject();
        json.addProperty("userId", msaToken.userId);
        json.addProperty("expireTimeMs", msaToken.expireTimeMs);
        json.addProperty("accessToken", msaToken.accessToken);
        json.addProperty("refreshToken", msaToken.refreshToken);
        if (this.prevStep != null) json.add("msaCode", this.prevStep.toJson(msaToken.msaCode));
        return json;
    }

    private MsaToken apply(final HttpClient httpClient, final String code, final String type, final MsaCodeStep.MsaCode msaCode) throws Exception {
        MinecraftAuth.LOGGER.info("Getting MSA Token...");

        final List<NameValuePair> postData = new ArrayList<>();
        postData.add(new BasicNameValuePair("client_id", msaCode.getClientId()));
        postData.add(new BasicNameValuePair("scope", msaCode.getScope()));
        postData.add(new BasicNameValuePair("grant_type", type));
        if (type.equals("refresh_token")) {
            postData.add(new BasicNameValuePair("refresh_token", code));
        } else {
            postData.add(new BasicNameValuePair("code", code));
            postData.add(new BasicNameValuePair("redirect_uri", msaCode.getRedirectUri()));
        }
        if (msaCode.getClientSecret() != null) {
            postData.add(new BasicNameValuePair("client_secret", msaCode.getClientSecret()));
        }

        final HttpPost httpPost = new HttpPost(TOKEN_URL);
        httpPost.setEntity(new UrlEncodedFormEntity(postData, StandardCharsets.UTF_8));
        final String response = httpClient.execute(httpPost, new MsaResponseHandler());
        final JsonObject obj = JsonParser.parseString(response).getAsJsonObject();

        final MsaToken msaToken = new MsaToken(
                obj.get("user_id").getAsString(),
                System.currentTimeMillis() + obj.get("expires_in").getAsLong() * 1000,
                obj.get("access_token").getAsString(),
                obj.get("refresh_token").getAsString(),
                msaCode
        );
        MinecraftAuth.LOGGER.info("Got MSA Token, expires: " + Instant.ofEpochMilli(msaToken.getExpireTimeMs()).atZone(ZoneId.systemDefault()));
        return msaToken;
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class MsaToken extends AbstractStep.StepResult<MsaCodeStep.MsaCode> {

        String userId;
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

        public boolean isTitleClientId() {
            return !UuidUtil.isDashedUuid(this.msaCode.getClientId());
        }

    }

}
