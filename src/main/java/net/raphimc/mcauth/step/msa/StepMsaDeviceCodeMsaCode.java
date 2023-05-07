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
import com.google.gson.JsonParser;
import net.raphimc.mcauth.MinecraftAuth;
import net.raphimc.mcauth.step.AbstractStep;
import net.raphimc.mcauth.util.MsaResponseHandler;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

public class StepMsaDeviceCodeMsaCode extends MsaCodeStep<StepMsaDeviceCode.MsaDeviceCode> {

    public static final String TOKEN_URL = "https://login.live.com/oauth20_token.srf";

    private final int timeout;

    public StepMsaDeviceCodeMsaCode(AbstractStep<?, StepMsaDeviceCode.MsaDeviceCode> prevStep, String clientId, String scope, final int timeout) {
        this(prevStep, clientId, scope, null, timeout);
    }

    public StepMsaDeviceCodeMsaCode(AbstractStep<?, StepMsaDeviceCode.MsaDeviceCode> prevStep, String clientId, String scope, final String clientSecret, final int timeout) {
        super(prevStep, clientId, scope, clientSecret);

        this.timeout = timeout;
    }

    @Override
    public MsaCode applyStep(HttpClient httpClient, StepMsaDeviceCode.MsaDeviceCode prevResult) throws Exception {
        MinecraftAuth.LOGGER.info("Waiting for MSA login via device code...");

        final long start = System.currentTimeMillis();
        while (!prevResult.isExpired() && System.currentTimeMillis() - start <= this.timeout) {
            final List<NameValuePair> postData = new ArrayList<>();
            postData.add(new BasicNameValuePair("client_id", this.clientId));
            postData.add(new BasicNameValuePair("device_code", prevResult.deviceCode()));
            postData.add(new BasicNameValuePair("grant_type", "device_code"));

            final HttpPost httpPost = new HttpPost(TOKEN_URL);
            httpPost.setEntity(new UrlEncodedFormEntity(postData, StandardCharsets.UTF_8));
            try {
                final String response = httpClient.execute(httpPost, new MsaResponseHandler());
                final JsonObject obj = JsonParser.parseString(response).getAsJsonObject();

                final MsaCode result = new MsaCode(obj.get("refresh_token").getAsString(), this.clientId, this.scope, this.clientSecret, null);
                MinecraftAuth.LOGGER.info("Got MSA Code");
                return result;
            } catch (HttpResponseException e) {
                if (e.getStatusCode() == HttpStatus.SC_BAD_REQUEST && e.getReasonPhrase().startsWith("authorization_pending")) {
                    Thread.sleep(prevResult.intervalMs());
                    continue;
                }
                throw e;
            }
        }

        throw new TimeoutException("Failed to get MSA Code. Login timed out");
    }

}
