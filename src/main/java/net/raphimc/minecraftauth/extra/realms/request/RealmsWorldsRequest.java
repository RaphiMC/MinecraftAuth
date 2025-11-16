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
package net.raphimc.minecraftauth.extra.realms.request;

import net.lenni0451.commons.gson.elements.GsonElement;
import net.lenni0451.commons.gson.elements.GsonObject;
import net.lenni0451.commons.httpclient.HttpResponse;
import net.lenni0451.commons.httpclient.requests.impl.GetRequest;
import net.raphimc.minecraftauth.extra.realms.model.RealmsServer;
import net.raphimc.minecraftauth.extra.realms.responsehandler.RealmsResponseHandler;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

public class RealmsWorldsRequest extends GetRequest implements RealmsResponseHandler<List<RealmsServer>> {

    public RealmsWorldsRequest(final String host) throws MalformedURLException {
        super("https://" + host + "/worlds");
    }

    @Override
    public List<RealmsServer> handle(final HttpResponse response, final GsonObject json) throws IOException {
        final List<RealmsServer> servers = new ArrayList<>();
        for (GsonElement server : json.reqArray("servers")) {
            servers.add(RealmsServer.fromApiJson(server.asObject()));
        }
        return servers;
    }

}
