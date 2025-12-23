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
import net.raphimc.minecraftauth.playfab.responsehandler.PlayFabResponseHandler;
import net.raphimc.minecraftauth.util.http.content.JsonContent;

import java.net.MalformedURLException;
import java.util.Locale;

public class PlayFabGetEntityTokenRequest extends PostRequest implements PlayFabResponseHandler<PlayFabEntityToken> {

    public PlayFabGetEntityTokenRequest(final PlayFabEntityToken entityToken, final String titleId, final String id, final String type) throws MalformedURLException {
        super("https://" + titleId.toLowerCase(Locale.ROOT) + ".playfabapi.com/Authentication/GetEntityToken");

        final JsonObject entity = new JsonObject();
        entity.addProperty("Id", id);
        entity.addProperty("Type", type);
        final JsonObject postData = new JsonObject();
        postData.add("Entity", entity);
        this.setContent(new JsonContent(postData));
        this.setHeader("X-EntityToken", entityToken.getToken());
    }

    @Override
    public PlayFabEntityToken handle(final HttpResponse response, final GsonObject json) {
        return PlayFabEntityToken.fromApiJson(json.reqObject("data"));
    }

}
