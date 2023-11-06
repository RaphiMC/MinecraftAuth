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
import java.util.function.Consumer;

public class StepMsaDeviceCode extends AbstractStep<StepMsaDeviceCode.MsaDeviceCodeCallback, StepMsaDeviceCode.MsaDeviceCode> {

    public static final String CONNECT_URL = "https://login.live.com/oauth20_connect.srf";

    private final String clientId;
    private final String scope;

    public StepMsaDeviceCode(final String clientId, final String scope) {
        super(null);

        this.clientId = clientId;
        this.scope = scope;
    }

    @Override
    public MsaDeviceCode applyStep(HttpClient httpClient, StepMsaDeviceCode.MsaDeviceCodeCallback prevResult) throws Exception {
        MinecraftAuth.LOGGER.info("Getting device code for MSA login...");

        if (prevResult == null) throw new IllegalStateException("Missing StepMsaDeviceCode.MsaDeviceCodeCallback input");

        final List<NameValuePair> postData = new ArrayList<>();
        postData.add(new BasicNameValuePair("client_id", this.clientId));
        postData.add(new BasicNameValuePair("response_type", "device_code"));
        postData.add(new BasicNameValuePair("scope", this.scope));

        final HttpPost httpPost = new HttpPost(CONNECT_URL);
        httpPost.setEntity(new UrlEncodedFormEntity(postData, StandardCharsets.UTF_8));
        final String response = httpClient.execute(httpPost, new MsaResponseHandler());
        final JsonObject obj = JsonParser.parseString(response).getAsJsonObject();

        final MsaDeviceCode result = new MsaDeviceCode(
                System.currentTimeMillis() + obj.get("expires_in").getAsLong() * 1000,
                obj.get("interval").getAsLong() * 1000,
                obj.get("device_code").getAsString(),
                obj.get("user_code").getAsString(),
                obj.get("verification_uri").getAsString()
        );
        MinecraftAuth.LOGGER.info("Got MSA device code, expires: " + Instant.ofEpochMilli(result.getExpireTimeMs()).atZone(ZoneId.systemDefault()));
        prevResult.callback.accept(result);
        return result;
    }

    @Override
    public MsaDeviceCode fromJson(JsonObject json) throws Exception {
        return new MsaDeviceCode(
                json.get("expireTimeMs").getAsLong(),
                json.get("intervalMs").getAsLong(),
                json.get("deviceCode").getAsString(),
                json.get("userCode").getAsString(),
                json.get("verificationUrl").getAsString()
        );
    }

    @Value
    public static class MsaDeviceCode implements AbstractStep.StepResult<AbstractStep.StepResult<?>> {

        long expireTimeMs;
        long intervalMs;
        String deviceCode;
        String userCode;
        String verificationUri;

        @Override
        public StepResult<?> prevResult() {
            return null;
        }

        @Override
        public JsonObject toJson() {
            final JsonObject json = new JsonObject();
            json.addProperty("expireTimeMs", this.expireTimeMs);
            json.addProperty("intervalMs", this.intervalMs);
            json.addProperty("deviceCode", this.deviceCode);
            json.addProperty("userCode", this.userCode);
            json.addProperty("verificationUri", this.verificationUri);
            return json;
        }

        @Override
        public boolean isExpired() throws Exception {
            return this.expireTimeMs <= System.currentTimeMillis();
        }

        public String getDirectVerificationUri() {
            return this.verificationUri + "?otc=" + this.userCode;
        }

    }

    @Value
    public static class MsaDeviceCodeCallback implements AbstractStep.InitialInput {
        Consumer<MsaDeviceCode> callback;
    }

}
