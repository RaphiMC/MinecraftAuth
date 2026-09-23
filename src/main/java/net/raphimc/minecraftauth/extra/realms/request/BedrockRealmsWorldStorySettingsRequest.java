/*
 * This file is part of MinecraftAuth - https://github.com/RaphiMC/MinecraftAuth
 * Copyright (C) 2022-2026 RK_01/RaphiMC and contributors
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
package net.raphimc.minecraftauth.extra.realms.request;

import com.google.gson.JsonObject;
import net.lenni0451.commons.gson.elements.GsonObject;
import net.lenni0451.commons.httpclient.HttpResponse;
import net.lenni0451.commons.httpclient.requests.impl.PostRequest;
import net.raphimc.minecraftauth.extra.realms.model.RealmsServer;
import net.raphimc.minecraftauth.extra.realms.responsehandler.RealmsResponseHandler;
import net.raphimc.minecraftauth.util.http.content.JsonContent;

import java.io.IOException;
import java.net.MalformedURLException;

public class BedrockRealmsWorldStorySettingsRequest extends PostRequest implements RealmsResponseHandler<Void> {

    public BedrockRealmsWorldStorySettingsRequest(final RealmsServer server, final Boolean notifications, final Boolean playerOptIn) throws MalformedURLException {
        super("https://pocket.realms.minecraft.net/worlds/" + server.getId() + "/stories/settings");

        final JsonObject postData = new JsonObject();
        if (notifications != null) {
            postData.addProperty("notifications", notifications);
        }
        if (playerOptIn != null) {
            postData.addProperty("playerOptIn", playerOptIn ? "OPT_IN" : "OPT_OUT");
        }
        this.setContent(new JsonContent(postData));
    }

    @Override
    public Void handle(final HttpResponse response, final GsonObject json) throws IOException {
        throw new UnsupportedOperationException("This request is not supposed to return any data");
    }

}
