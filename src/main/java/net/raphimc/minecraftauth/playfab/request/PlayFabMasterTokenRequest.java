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
import net.raphimc.minecraftauth.playfab.model.PlayFabMasterToken;
import net.raphimc.minecraftauth.playfab.model.PlayFabToken;
import net.raphimc.minecraftauth.playfab.responsehandler.PlayFabResponseHandler;
import net.raphimc.minecraftauth.util.http.content.JsonContent;

import java.net.MalformedURLException;
import java.time.Instant;
import java.util.Locale;

public class PlayFabMasterTokenRequest extends PostRequest implements PlayFabResponseHandler<PlayFabMasterToken> {
    public PlayFabMasterTokenRequest(final PlayFabToken playFabToken, final String titleId) throws MalformedURLException {
        super("https://" + titleId.toLowerCase(Locale.ROOT) + ".playfabapi.com/Authentication/GetEntityToken");

        this.appendHeader("X-EntityToken", playFabToken.getEntityToken());
        final JsonObject entity = new JsonObject();
        entity.addProperty("Id", playFabToken.getPlayFabId());
        entity.addProperty("Type", "master_player_account");
        final JsonObject object = new JsonObject();
        object.add("Entity", entity);
        this.setContent(new JsonContent(object));
    }

    @Override
    public PlayFabMasterToken handle(final HttpResponse response, final GsonObject json) {
        final GsonObject data = json.reqObject("data");
        final GsonObject entity = data.reqObject("Entity");
        return new PlayFabMasterToken(
                Instant.parse(data.reqString("TokenExpiration")).toEpochMilli(),
                data.reqString("EntityToken"),
                entity.reqString("Id"),
                entity.reqString("Type")
        );
    }
}
