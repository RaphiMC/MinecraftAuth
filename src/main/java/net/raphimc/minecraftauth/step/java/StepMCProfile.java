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
import lombok.EqualsAndHashCode;
import lombok.Value;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.responsehandler.MinecraftResponseHandler;
import net.raphimc.minecraftauth.step.AbstractStep;
import net.raphimc.minecraftauth.util.JsonUtil;
import net.raphimc.minecraftauth.util.UuidUtil;
import org.apache.http.HttpHeaders;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;

import java.io.IOException;
import java.util.UUID;

public class StepMCProfile extends AbstractStep<StepMCToken.MCToken, StepMCProfile.MCProfile> {

    public static final String MINECRAFT_PROFILE_URL = "https://api.minecraftservices.com/minecraft/profile";

    public StepMCProfile(final AbstractStep<?, StepMCToken.MCToken> prevStep) {
        super("mcProfile", prevStep);
    }

    @Override
    public MCProfile applyStep(final HttpClient httpClient, final StepMCToken.MCToken mcToken) throws Exception {
        MinecraftAuth.LOGGER.info("Getting profile...");

        final HttpGet httpGet = new HttpGet(MINECRAFT_PROFILE_URL);
        httpGet.addHeader(HttpHeaders.AUTHORIZATION, mcToken.getTokenType() + " " + mcToken.getAccessToken());
        final String response = httpClient.execute(httpGet, new MinecraftResponseHandler());
        final JsonObject obj = JsonUtil.parseString(response).getAsJsonObject();

        if (obj.has("error")) {
            throw new IOException("No valid minecraft profile found: " + obj);
        }

        final MCProfile mcProfile = new MCProfile(
                UuidUtil.fromLenientString(obj.get("id").getAsString()),
                obj.get("name").getAsString(),
                obj.get("skins").getAsJsonArray().get(0).getAsJsonObject().get("url").getAsString(),
                mcToken
        );
        MinecraftAuth.LOGGER.info("Got MC Profile, name: " + mcProfile.name + ", uuid: " + mcProfile.id);
        return mcProfile;
    }

    @Override
    public MCProfile fromJson(final JsonObject json) {
        final StepMCToken.MCToken mcToken = this.prevStep != null ? this.prevStep.fromJson(json.getAsJsonObject(this.prevStep.name)) : null;
        return new MCProfile(
                UUID.fromString(json.get("id").getAsString()),
                json.get("name").getAsString(),
                json.get("skinUrl").getAsString(),
                mcToken
        );
    }

    @Override
    public JsonObject toJson(final MCProfile mcProfile) {
        final JsonObject json = new JsonObject();
        json.addProperty("id", mcProfile.id.toString());
        json.addProperty("name", mcProfile.name);
        json.addProperty("skinUrl", mcProfile.skinUrl);
        if (this.prevStep != null) json.add(this.prevStep.name, this.prevStep.toJson(mcProfile.mcToken));
        return json;
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

    }

}
