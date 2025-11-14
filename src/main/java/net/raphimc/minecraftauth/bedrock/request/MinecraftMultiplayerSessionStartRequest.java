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
import net.raphimc.minecraftauth.bedrock.model.MinecraftMultiplayerToken;
import net.raphimc.minecraftauth.bedrock.model.MinecraftSession;
import net.raphimc.minecraftauth.bedrock.responsehandler.MinecraftServicesResponseHandler;
import net.raphimc.minecraftauth.util.http.content.JsonContent;

import java.io.IOException;
import java.net.MalformedURLException;
import java.security.KeyPair;
import java.time.Instant;
import java.util.Base64;

public class MinecraftMultiplayerSessionStartRequest extends PostRequest implements MinecraftServicesResponseHandler<MinecraftMultiplayerToken> {

    public MinecraftMultiplayerSessionStartRequest(final MinecraftSession session, final KeyPair ecdsa384KeyPair) throws MalformedURLException {
        super("https://authorization.franchise.minecraft-services.net/api/v1.0/multiplayer/session/start");

        final JsonObject postData = new JsonObject();
        postData.addProperty("publicKey", Base64.getEncoder().encodeToString(ecdsa384KeyPair.getPublic().getEncoded()));

        this.setContent(new JsonContent(postData));
        this.setHeader(HttpHeaders.AUTHORIZATION, session.getAuthorizationHeader());
    }

    @Override
    public MinecraftMultiplayerToken handle(final HttpResponse response, final GsonObject json) throws IOException {
        final GsonObject result = json.reqObject("result");
        return new MinecraftMultiplayerToken(
                Instant.parse(result.reqString("validUntil")).toEpochMilli(),
                result.reqString("signedToken")
        );
    }

}
