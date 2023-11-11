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
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;

import java.io.IOException;
import java.util.UUID;

public class StepMCProfile extends AbstractStep<StepMCToken.MCToken, StepMCProfile.MCProfile> {

    public static final String MINECRAFT_PROFILE_URL = "https://api.minecraftservices.com/minecraft/profile";

    public StepMCProfile(final AbstractStep<?, StepMCToken.MCToken> prevStep) {
        super(prevStep);
    }

    @Override
    public MCProfile applyStep(final HttpClient httpClient, final StepMCToken.MCToken mcToken) throws Exception {
        MinecraftAuth.LOGGER.info("Getting profile...");

        final HttpGet httpGet = new HttpGet(MINECRAFT_PROFILE_URL);
        httpGet.addHeader("Authorization", mcToken.getTokenType() + " " + mcToken.getAccessToken());
        final String response = httpClient.execute(httpGet, new BasicResponseHandler());
        final JsonObject obj = JsonParser.parseString(response).getAsJsonObject();

        if (obj.has("error")) {
            throw new IOException("No valid minecraft profile found: " + obj);
        }

        final MCProfile result = new MCProfile(
                UUID.fromString(obj.get("id").getAsString().replaceFirst("(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)", "$1-$2-$3-$4-$5")),
                obj.get("name").getAsString(),
                obj.get("skins").getAsJsonArray().get(0).getAsJsonObject().get("url").getAsString(),
                mcToken
        );
        MinecraftAuth.LOGGER.info("Got MC Profile, name: " + result.name + ", uuid: " + result.id);
        return result;
    }

    @Override
    public MCProfile fromJson(final JsonObject json) {
        final StepMCToken.MCToken mcToken = this.prevStep != null ? this.prevStep.fromJson(json.getAsJsonObject("mcToken")) : null;
        return new MCProfile(
                UUID.fromString(json.get("id").getAsString()),
                json.get("name").getAsString(),
                json.get("skinUrl").getAsString(),
                mcToken
        );
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class MCProfile extends AbstractStep.StepResult<StepMCToken.MCToken> {

        UUID id;
        String name;
        String skinUrl;
        StepMCToken.MCToken mcToken;

        @Override
        protected StepMCToken.MCToken prevResult() {
            return this.mcToken;
        }

        @Override
        public JsonObject toJson() {
            final JsonObject json = new JsonObject();
            json.addProperty("id", this.id.toString());
            json.addProperty("name", this.name);
            json.addProperty("skinUrl", this.skinUrl);
            if (this.mcToken != null) json.add("mcToken", this.mcToken.toJson());
            return json;
        }

    }

}
