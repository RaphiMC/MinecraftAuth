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
package net.raphimc.minecraftauth.bedrock.request;

import com.google.gson.JsonObject;
import net.lenni0451.commons.gson.elements.GsonObject;
import net.lenni0451.commons.httpclient.HttpResponse;
import net.lenni0451.commons.httpclient.constants.HttpHeaders;
import net.lenni0451.commons.httpclient.requests.impl.PostRequest;
import net.raphimc.minecraftauth.bedrock.model.MinecraftSession;
import net.raphimc.minecraftauth.bedrock.responsehandler.MinecraftServicesResponseHandler;
import net.raphimc.minecraftauth.playfab.data.PlayFabConstants;
import net.raphimc.minecraftauth.playfab.model.PlayFabToken;
import net.raphimc.minecraftauth.util.UuidUtil;
import net.raphimc.minecraftauth.util.http.content.JsonContent;
import net.raphimc.minecraftauth.xbl.model.XblXstsToken;

import java.io.IOException;
import java.net.MalformedURLException;
import java.time.Instant;
import java.util.UUID;

public class MinecraftSessionStartRequest extends PostRequest implements MinecraftServicesResponseHandler<MinecraftSession> {

    public MinecraftSessionStartRequest(final XblXstsToken xstsToken, final PlayFabToken playFabToken, final String gameVersion, final UUID deviceId) throws MalformedURLException {
        super("https://authorization.franchise.minecraft-services.net/api/v1.0/session/start");

        final JsonObject device = new JsonObject();
        device.addProperty("applicationType", "MinecraftPE");
        device.addProperty("gameVersion", gameVersion);
        device.addProperty("id", UuidUtil.toUndashedString(deviceId));
        device.addProperty("memory", 32L * 1024L * 1024L * 1024L);
        device.addProperty("hardwareMemoryTier", 5);
        device.addProperty("platform", "Windows10");
        device.addProperty("playFabTitleId", PlayFabConstants.BEDROCK_PLAY_FAB_TITLE_ID);
        device.addProperty("storePlatform", "uwp.store");
        device.addProperty("type", "Windows10");
        final JsonObject user = new JsonObject();
        user.addProperty("language", "en");
        user.addProperty("regionCode", "US");
        user.addProperty("languageCode", "en-US");
        user.addProperty("tokenType", "PlayFab");
        user.addProperty("token", playFabToken.getSessionTicket());
        final JsonObject postData = new JsonObject();
        postData.add("device", device);
        postData.add("user", user);

        this.setContent(new JsonContent(postData));
        this.setHeader(HttpHeaders.AUTHORIZATION, xstsToken.getAuthorizationHeader());
    }

    @Override
    public MinecraftSession handle(final HttpResponse response, final GsonObject json) throws IOException {
        final GsonObject result = json.reqObject("result");
        return new MinecraftSession(
                Instant.parse(result.reqString("validUntil")).toEpochMilli(),
                result.reqString("authorizationHeader")
        );
    }

}
