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
package net.raphimc.minecraftauth.step.java;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.EqualsAndHashCode;
import lombok.Value;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.step.AbstractStep;
import net.raphimc.minecraftauth.step.xbl.StepXblXstsToken;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;

import java.time.Instant;
import java.time.ZoneId;

public class StepMCToken extends AbstractStep<StepXblXstsToken.XblXsts<?>, StepMCToken.MCToken> {

    public static final String MINECRAFT_LOGIN_URL = "https://api.minecraftservices.com/authentication/login_with_xbox";

    public StepMCToken(final AbstractStep<?, StepXblXstsToken.XblXsts<?>> prevStep) {
        super(prevStep);
    }

    @Override
    public MCToken applyStep(final HttpClient httpClient, final StepXblXstsToken.XblXsts<?> xblXsts) throws Exception {
        MinecraftAuth.LOGGER.info("Authenticating with Minecraft Services...");

        final JsonObject postData = new JsonObject();
        postData.addProperty("identityToken", "XBL3.0 x=" + xblXsts.getUserHash() + ";" + xblXsts.getToken());

        final HttpPost httpPost = new HttpPost(MINECRAFT_LOGIN_URL);
        httpPost.setEntity(new StringEntity(postData.toString(), ContentType.APPLICATION_JSON));
        final String response = httpClient.execute(httpPost, new BasicResponseHandler());
        final JsonObject obj = JsonParser.parseString(response).getAsJsonObject();

        final MCToken result = new MCToken(
                obj.get("access_token").getAsString(),
                obj.get("token_type").getAsString(),
                System.currentTimeMillis() + obj.get("expires_in").getAsLong() * 1000,
                xblXsts
        );
        MinecraftAuth.LOGGER.info("Got MC Token, expires: " + Instant.ofEpochMilli(result.getExpireTimeMs()).atZone(ZoneId.systemDefault()));
        return result;
    }

    @Override
    public MCToken refresh(final HttpClient httpClient, final MCToken result) throws Exception {
        if (result.isExpired()) return super.refresh(httpClient, result);

        return result;
    }

    @Override
    public MCToken fromJson(final JsonObject json) {
        final StepXblXstsToken.XblXsts<?> xblXsts = this.prevStep != null ? this.prevStep.fromJson(json.getAsJsonObject("xblXsts")) : null;
        return new MCToken(
                json.get("accessToken").getAsString(),
                json.get("tokenType").getAsString(),
                json.get("expireTimeMs").getAsLong(),
                xblXsts
        );
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class MCToken extends AbstractStep.StepResult<StepXblXstsToken.XblXsts<?>> {

        String accessToken;
        String tokenType;
        long expireTimeMs;
        StepXblXstsToken.XblXsts<?> xblXsts;

        @Override
        protected StepXblXstsToken.XblXsts<?> prevResult() {
            return this.xblXsts;
        }

        @Override
        public JsonObject toJson() {
            final JsonObject json = new JsonObject();
            json.addProperty("accessToken", this.accessToken);
            json.addProperty("tokenType", this.tokenType);
            json.addProperty("expireTimeMs", this.expireTimeMs);
            if (this.xblXsts != null) json.add("xblXsts", this.xblXsts.toJson());
            return json;
        }

        @Override
        public boolean isExpired() {
            return this.expireTimeMs <= System.currentTimeMillis();
        }

    }

}
