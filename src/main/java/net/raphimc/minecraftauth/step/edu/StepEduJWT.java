/*
 * This file is part of MinecraftAuth - https://github.com/RaphiMC/MinecraftAuth
 * Copyright (C) 2022-2025 RK_01/RaphiMC and contributors
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
package net.raphimc.minecraftauth.step.edu;

import com.google.gson.JsonObject;
import lombok.EqualsAndHashCode;
import lombok.Value;
import net.lenni0451.commons.httpclient.HttpClient;
import net.lenni0451.commons.httpclient.requests.impl.PostRequest;
import net.raphimc.minecraftauth.responsehandler.MinecraftEduServicesResponseHandler;
import net.raphimc.minecraftauth.step.AbstractStep;
import net.raphimc.minecraftauth.step.msa.StepMsaToken;
import net.raphimc.minecraftauth.util.JsonContent;
import net.raphimc.minecraftauth.util.logging.ILogger;

public class StepEduJWT extends AbstractStep<StepMsaToken.MsaToken, StepEduJWT.EduJWT> {

    public static final String MINECRAFT_LOGIN_URL = "https://login.minecrafteduservices.com/v2/signin";

    private final String version;
    private final int buildNumber;
    private final int protocolVersion;

    public StepEduJWT(final AbstractStep<?, StepMsaToken.MsaToken> prevStep, final String version, final int protocolVersion) {
        super("eduJwt", prevStep);

        final String[] versionParts = version.split("\\.");
        if (versionParts.length != 3) {
            throw new IllegalArgumentException("Invalid version: " + version);
        }

        this.version = version;
        this.buildNumber = Integer.parseInt(versionParts[0]) * 10_00_00_00 + Integer.parseInt(versionParts[1]) * 10_00_00 + Integer.parseInt(versionParts[2]) * 10_00;
        this.protocolVersion = protocolVersion;
    }

    @Override
    protected EduJWT execute(final ILogger logger, final HttpClient httpClient, final StepMsaToken.MsaToken msaToken) throws Exception {
        logger.info(this, "Authenticating with Minecraft Education Services...");

        final JsonObject postData = new JsonObject();
        postData.addProperty("accessToken", msaToken.getAccessToken());
        postData.addProperty("build", this.buildNumber);
        postData.addProperty("clientVersion", this.protocolVersion);
        postData.addProperty("displayVersion", this.version);
        postData.addProperty("identityToken", msaToken.getAccessToken());
        postData.addProperty("locale", "en_US");
        postData.addProperty("osVersion", "10.0");
        postData.addProperty("platform", "Windows Desktop Build (Win32)(x64)");
        postData.addProperty("platformCategory", "desktop");

        final PostRequest postRequest = new PostRequest(MINECRAFT_LOGIN_URL);
        postRequest.setContent(new JsonContent(postData));
        final JsonObject obj = httpClient.execute(postRequest, new MinecraftEduServicesResponseHandler());

        final EduJWT eduJwt = new EduJWT(
                obj.get("response").getAsString(),
                msaToken
        );
        logger.info(this, "Got Edu JWT");
        return eduJwt;
    }

    @Override
    public EduJWT fromJson(final JsonObject json) {
        final StepMsaToken.MsaToken msaToken = this.prevStep != null ? this.prevStep.fromJson(json.getAsJsonObject(this.prevStep.name)) : null;
        return new EduJWT(
                json.get("jwt").getAsString(),
                msaToken
        );
    }

    @Override
    public JsonObject toJson(final EduJWT eduJWT) {
        final JsonObject json = new JsonObject();
        json.addProperty("jwt", eduJWT.jwt);
        if (this.prevStep != null) json.add(this.prevStep.name, this.prevStep.toJson(eduJWT.msaToken));
        return json;
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class EduJWT extends AbstractStep.StepResult<StepMsaToken.MsaToken> {

        String jwt;
        StepMsaToken.MsaToken msaToken;

        @Override
        protected StepMsaToken.MsaToken prevResult() {
            return this.msaToken;
        }

        @Override
        public boolean isExpired() {
            return true; // Can't properly parse and validate the JWT because we don't have the public key
        }

        @Override
        public boolean isExpiredOrOutdated() {
            return true;
        }

    }

}
