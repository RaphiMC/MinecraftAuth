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
import net.lenni0451.commons.httpclient.constants.Headers;
import net.lenni0451.commons.httpclient.requests.impl.GetRequest;
import net.raphimc.minecraftauth.responsehandler.MinecraftResponseHandler;
import net.raphimc.minecraftauth.step.AbstractStep;
import net.raphimc.minecraftauth.util.UuidUtil;
import net.raphimc.minecraftauth.util.logging.ILogger;

import java.util.UUID;

public class StepMCProfile extends AbstractStep<StepMCToken.MCToken, StepMCProfile.MCProfile> {

    public static final String MINECRAFT_PROFILE_URL = "https://api.minecraftservices.com/minecraft/profile";

    public StepMCProfile(final AbstractStep<?, StepMCToken.MCToken> prevStep) {
        super("mcProfile", prevStep);
    }

    @Override
    public MCProfile execute(final ILogger logger, final HttpClient httpClient, final StepMCToken.MCToken mcToken) throws Exception {
        logger.info("Getting profile...");

        final GetRequest getRequest = new GetRequest(MINECRAFT_PROFILE_URL);
        getRequest.setHeader(Headers.AUTHORIZATION, mcToken.getTokenType() + " " + mcToken.getAccessToken());
        final JsonObject obj = httpClient.execute(getRequest, new MinecraftResponseHandler());

        final MCProfile mcProfile = new MCProfile(
                UuidUtil.fromLenientString(obj.get("id").getAsString()),
                obj.get("name").getAsString(),
                obj.get("skins").getAsJsonArray().get(0).getAsJsonObject().get("url").getAsString(),
                mcToken
        );
        logger.info("Got MC Profile, name: " + mcProfile.name + ", uuid: " + mcProfile.id);
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

        @Override
        public boolean isExpired() {
            return this.prevResult().isExpired();
        }

        @Override
        public boolean isExpiredOrOutdated() {
            return true;
        }

    }

}
