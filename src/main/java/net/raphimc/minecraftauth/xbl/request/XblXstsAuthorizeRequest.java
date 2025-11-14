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
package net.raphimc.minecraftauth.xbl.request;

import com.google.gson.JsonObject;
import net.lenni0451.commons.gson.elements.GsonArray;
import net.lenni0451.commons.gson.elements.GsonObject;
import net.lenni0451.commons.httpclient.HttpResponse;
import net.lenni0451.commons.httpclient.requests.impl.PostRequest;
import net.raphimc.minecraftauth.util.http.content.JsonContent;
import net.raphimc.minecraftauth.xbl.model.XblDeviceToken;
import net.raphimc.minecraftauth.xbl.model.XblTitleToken;
import net.raphimc.minecraftauth.xbl.model.XblUserToken;
import net.raphimc.minecraftauth.xbl.model.XblXstsToken;
import net.raphimc.minecraftauth.xbl.responsehandler.XblResponseHandler;

import java.net.MalformedURLException;
import java.time.Instant;

public class XblXstsAuthorizeRequest extends PostRequest implements XblResponseHandler<XblXstsToken> {

    public XblXstsAuthorizeRequest(final XblDeviceToken deviceToken, final XblUserToken userToken, final XblTitleToken titleToken, final String relyingParty) throws MalformedURLException {
        super("https://xsts.auth.xboxlive.com/xsts/authorize");

        final JsonObject properties = new JsonObject();
        properties.addProperty("SandboxId", "RETAIL");
        if (deviceToken != null) {
            properties.addProperty("DeviceToken", deviceToken.getToken());
        }
        properties.add("UserTokens", new GsonArray().add(userToken.getToken()).getJsonArray());
        if (titleToken != null) {
            properties.addProperty("TitleToken", titleToken.getToken());
        }
        final JsonObject postData = new JsonObject();
        postData.add("Properties", properties);
        postData.addProperty("RelyingParty", relyingParty);
        postData.addProperty("TokenType", "JWT");

        this.setContent(new JsonContent(postData));
        this.setHeader("x-xbl-contract-version", "1");
    }

    @Override
    public XblXstsToken handle(final HttpResponse response, final GsonObject json) {
        return new XblXstsToken(
                Instant.parse(json.reqString("NotAfter")).toEpochMilli(),
                json.reqString("Token"),
                json.reqObject("DisplayClaims").reqArray("xui").get(0).asObject().reqString("uhs")
        );
    }

}
