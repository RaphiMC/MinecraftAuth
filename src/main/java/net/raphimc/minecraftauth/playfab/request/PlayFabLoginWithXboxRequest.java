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
package net.raphimc.minecraftauth.playfab.request;

import com.google.gson.JsonObject;
import net.lenni0451.commons.gson.elements.GsonObject;
import net.lenni0451.commons.httpclient.HttpResponse;
import net.lenni0451.commons.httpclient.requests.impl.PostRequest;
import net.raphimc.minecraftauth.playfab.model.PlayFabEntityToken;
import net.raphimc.minecraftauth.playfab.model.PlayFabToken;
import net.raphimc.minecraftauth.playfab.responsehandler.PlayFabResponseHandler;
import net.raphimc.minecraftauth.util.http.content.JsonContent;
import net.raphimc.minecraftauth.xbl.model.XblXstsToken;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Locale;

public class PlayFabLoginWithXboxRequest extends PostRequest implements PlayFabResponseHandler<PlayFabToken> {

    public PlayFabLoginWithXboxRequest(final XblXstsToken xstsToken, final String titleId) throws MalformedURLException {
        super("https://" + titleId.toLowerCase(Locale.ROOT) + ".playfabapi.com/Client/LoginWithXbox");

        final JsonObject infoRequestParameters = new JsonObject();
        infoRequestParameters.addProperty("GetPlayerProfile", true);
        infoRequestParameters.addProperty("GetUserAccountInfo", true);
        final JsonObject postData = new JsonObject();
        postData.addProperty("CreateAccount", true);
        postData.add("InfoRequestParameters", infoRequestParameters);
        postData.addProperty("TitleId", titleId.toUpperCase(Locale.ROOT));
        postData.addProperty("XboxToken", xstsToken.getAuthorizationHeader());
        this.setContent(new JsonContent(postData));
    }

    @Override
    public PlayFabToken handle(final HttpResponse response, final GsonObject json) throws IOException {
        final GsonObject data = json.reqObject("data");
        return new PlayFabToken(
                PlayFabEntityToken.fromApiJson(data.reqObject("EntityToken")),
                data.reqString("PlayFabId"),
                data.reqString("SessionTicket")
        );
    }

}
