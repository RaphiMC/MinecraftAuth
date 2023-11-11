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
import net.raphimc.minecraftauth.step.AbstractStep;
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

    public StepMsaToken(AbstractStep<?, MsaCodeStep.MsaCode> prevStep) {
        super(prevStep);
    }

    @Override
    public MsaToken applyStep(HttpClient httpClient, MsaCodeStep.MsaCode prevResult) throws Exception {
        return this.apply(httpClient, prevResult.getCode(), prevResult.getRedirectUri() != null ? "authorization_code" : "refresh_token", prevResult);
    }

    @Override
    public MsaToken refresh(HttpClient httpClient, MsaToken result) throws Exception {
        if (result.isExpired()) return this.apply(httpClient, result.getRefreshToken(), "refresh_token", result.getMsaCode());

        return result;
    }

    @Override
    public MsaToken fromJson(JsonObject json) {
        final MsaCodeStep.MsaCode msaCode = this.prevStep != null ? this.prevStep.fromJson(json.getAsJsonObject("msaCode")) : null;
        return new MsaToken(
                json.get("userId").getAsString(),
                json.get("expireTimeMs").getAsLong(),
                json.get("accessToken").getAsString(),
                json.get("refreshToken").getAsString(),
                msaCode
        );
    }

    private MsaToken apply(final HttpClient httpClient, final String code, final String type, final MsaCodeStep.MsaCode prev_result) throws Exception {
        MinecraftAuth.LOGGER.info("Getting MSA Token...");

        final List<NameValuePair> postData = new ArrayList<>();
        postData.add(new BasicNameValuePair("client_id", prev_result.getClientId()));
        postData.add(new BasicNameValuePair("scope", prev_result.getScope()));
        postData.add(new BasicNameValuePair("grant_type", type));
        if (type.equals("refresh_token")) {
            postData.add(new BasicNameValuePair("refresh_token", code));
        } else {
            postData.add(new BasicNameValuePair("code", code));
            postData.add(new BasicNameValuePair("redirect_uri", prev_result.getRedirectUri()));
        }
        if (prev_result.getClientSecret() != null) {
            postData.add(new BasicNameValuePair("client_secret", prev_result.getClientSecret()));
        }

        final HttpPost httpPost = new HttpPost(TOKEN_URL);
        httpPost.setEntity(new UrlEncodedFormEntity(postData, StandardCharsets.UTF_8));
        final String response = httpClient.execute(httpPost, new MsaResponseHandler());
        final JsonObject obj = JsonParser.parseString(response).getAsJsonObject();

        final MsaToken result = new MsaToken(
                obj.get("user_id").getAsString(),
                System.currentTimeMillis() + obj.get("expires_in").getAsLong() * 1000,
                obj.get("access_token").getAsString(),
                obj.get("refresh_token").getAsString(),
                prev_result
        );
        MinecraftAuth.LOGGER.info("Got MSA Token, expires: " + Instant.ofEpochMilli(result.getExpireTimeMs()).atZone(ZoneId.systemDefault()));
        return result;
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
        public JsonObject toJson() {
            final JsonObject json = new JsonObject();
            json.addProperty("userId", this.userId);
            json.addProperty("expireTimeMs", this.expireTimeMs);
            json.addProperty("accessToken", this.accessToken);
            json.addProperty("refreshToken", this.refreshToken);
            if (this.msaCode != null) json.add("msaCode", this.msaCode.toJson());
            return json;
        }

        @Override
        public boolean isExpired() {
            return this.expireTimeMs <= System.currentTimeMillis();
        }

        public boolean isTitleClientId() {
            return !this.msaCode.getClientId().matches("\\p{XDigit}{8}-\\p{XDigit}{4}-\\p{XDigit}{4}-\\p{XDigit}{4}-\\p{XDigit}+");
        }

    }

}
