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
package net.raphimc.minecraftauth.java.request;

import com.google.gson.JsonObject;
import net.lenni0451.commons.gson.elements.GsonObject;
import net.lenni0451.commons.httpclient.HttpResponse;
import net.lenni0451.commons.httpclient.requests.impl.PostRequest;
import net.raphimc.minecraftauth.java.model.MinecraftToken;
import net.raphimc.minecraftauth.java.responsehandler.MinecraftServicesResponseHandler;
import net.raphimc.minecraftauth.util.http.content.JsonContent;
import net.raphimc.minecraftauth.xbl.model.XblXstsToken;

import java.io.IOException;
import java.net.MalformedURLException;

public class MinecraftLauncherLoginRequest extends PostRequest implements MinecraftServicesResponseHandler<MinecraftToken> {

    public MinecraftLauncherLoginRequest(final XblXstsToken xstsToken) throws MalformedURLException {
        super("https://api.minecraftservices.com/launcher/login");

        final JsonObject postData = new JsonObject();
        postData.addProperty("platform", "PC_LAUNCHER");
        postData.addProperty("xtoken", xstsToken.getAuthorizationHeader());
        this.setContent(new JsonContent(postData));
    }

    @Override
    public MinecraftToken handle(final HttpResponse response, final GsonObject json) throws IOException {
        return new MinecraftToken(
                System.currentTimeMillis() + json.reqInt("expires_in") * 1000L,
                json.reqString("token_type"),
                json.reqString("access_token")
        );
    }

}
