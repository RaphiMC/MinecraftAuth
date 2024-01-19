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
package net.raphimc.minecraftauth.step.java;

import com.google.gson.JsonObject;
import lombok.EqualsAndHashCode;
import lombok.Value;
import net.lenni0451.commons.httpclient.HttpClient;
import net.lenni0451.commons.httpclient.requests.impl.PostRequest;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.responsehandler.MinecraftResponseHandler;
import net.raphimc.minecraftauth.step.AbstractStep;
import net.raphimc.minecraftauth.step.xbl.StepXblXstsToken;
import net.raphimc.minecraftauth.util.JsonContent;

import java.time.Instant;
import java.time.ZoneId;

public class StepMCToken extends AbstractStep<StepXblXstsToken.XblXsts<?>, StepMCToken.MCToken> {

    public static final String MINECRAFT_LOGIN_URL = "https://api.minecraftservices.com/authentication/login_with_xbox";

    public StepMCToken(final AbstractStep<?, ? extends StepXblXstsToken.XblXsts<?>> prevStep) {
        super("mcToken", (AbstractStep<?, StepXblXstsToken.XblXsts<?>>) prevStep);
    }

    @Override
    public MCToken applyStep(final HttpClient httpClient, final StepXblXstsToken.XblXsts<?> xblXsts) throws Exception {
        MinecraftAuth.LOGGER.info("Authenticating with Minecraft Services...");

        final JsonObject postData = new JsonObject();
        postData.addProperty("identityToken", "XBL3.0 x=" + xblXsts.getServiceToken());

        final PostRequest postRequest = new PostRequest(MINECRAFT_LOGIN_URL);
        postRequest.setContent(new JsonContent(postData));
        final JsonObject obj = httpClient.execute(postRequest, new MinecraftResponseHandler());

        final MCToken mcToken = new MCToken(
                obj.get("access_token").getAsString(),
                obj.get("token_type").getAsString(),
                System.currentTimeMillis() + obj.get("expires_in").getAsLong() * 1000,
                xblXsts
        );
        MinecraftAuth.LOGGER.info("Got MC Token, expires: " + Instant.ofEpochMilli(mcToken.getExpireTimeMs()).atZone(ZoneId.systemDefault()));
        return mcToken;
    }

    @Override
    public MCToken fromJson(final JsonObject json) {
        final StepXblXstsToken.XblXsts<?> xblXsts = this.prevStep != null ? this.prevStep.fromJson(json.getAsJsonObject(this.prevStep.name)) : null;
        return new MCToken(
                json.get("accessToken").getAsString(),
                json.get("tokenType").getAsString(),
                json.get("expireTimeMs").getAsLong(),
                xblXsts
        );
    }

    @Override
    public JsonObject toJson(final MCToken mcToken) {
        final JsonObject json = new JsonObject();
        json.addProperty("accessToken", mcToken.accessToken);
        json.addProperty("tokenType", mcToken.tokenType);
        json.addProperty("expireTimeMs", mcToken.expireTimeMs);
        if (this.prevStep != null) json.add(this.prevStep.name, this.prevStep.toJson(mcToken.xblXsts));
        return json;
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
        public boolean isExpired() {
            return this.expireTimeMs <= System.currentTimeMillis();
        }

    }

}
