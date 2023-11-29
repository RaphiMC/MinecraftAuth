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
import lombok.EqualsAndHashCode;
import lombok.Value;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.responsehandler.MsaResponseHandler;
import net.raphimc.minecraftauth.step.AbstractStep;
import net.raphimc.minecraftauth.util.JsonUtil;
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
import java.util.function.Consumer;

public class StepMsaDeviceCode extends AbstractStep<StepMsaDeviceCode.MsaDeviceCodeCallback, StepMsaDeviceCode.MsaDeviceCode> {

    public static final String CONNECT_URL = "https://login.live.com/oauth20_connect.srf";

    private final String clientId;
    private final String scope;

    public StepMsaDeviceCode(final String clientId, final String scope) {
        super("msaDeviceCode", null);

        this.clientId = clientId;
        this.scope = scope;
    }

    @Override
    public MsaDeviceCode applyStep(final HttpClient httpClient, final StepMsaDeviceCode.MsaDeviceCodeCallback msaDeviceCodeCallback) throws Exception {
        MinecraftAuth.LOGGER.info("Getting device code for MSA login...");

        if (msaDeviceCodeCallback == null) throw new IllegalStateException("Missing StepMsaDeviceCode.MsaDeviceCodeCallback input");

        final List<NameValuePair> postData = new ArrayList<>();
        postData.add(new BasicNameValuePair("client_id", this.clientId));
        postData.add(new BasicNameValuePair("response_type", "device_code"));
        postData.add(new BasicNameValuePair("scope", this.scope));

        final HttpPost httpPost = new HttpPost(CONNECT_URL);
        httpPost.setEntity(new UrlEncodedFormEntity(postData, StandardCharsets.UTF_8));
        final String response = httpClient.execute(httpPost, new MsaResponseHandler());
        final JsonObject obj = JsonUtil.parseString(response).getAsJsonObject();

        final MsaDeviceCode msaDeviceCode = new MsaDeviceCode(
                System.currentTimeMillis() + obj.get("expires_in").getAsLong() * 1000,
                obj.get("interval").getAsLong() * 1000,
                obj.get("device_code").getAsString(),
                obj.get("user_code").getAsString(),
                obj.get("verification_uri").getAsString()
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
                json.get("verificationUrl").getAsString()
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
    public static class MsaDeviceCode extends AbstractStep.FirstStepResult {

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
