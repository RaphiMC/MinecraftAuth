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
package net.raphimc.minecraftauth.java.request;

import net.lenni0451.commons.gson.elements.GsonObject;
import net.lenni0451.commons.httpclient.HttpResponse;
import net.lenni0451.commons.httpclient.constants.HttpHeaders;
import net.lenni0451.commons.httpclient.requests.impl.GetRequest;
import net.raphimc.minecraftauth.java.model.MinecraftEntitlements;
import net.raphimc.minecraftauth.java.model.MinecraftToken;
import net.raphimc.minecraftauth.java.responsehandler.MinecraftServicesResponseHandler;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.stream.Collectors;

public class MinecraftEntitlementsRequest extends GetRequest implements MinecraftServicesResponseHandler<MinecraftEntitlements> {

    public MinecraftEntitlementsRequest(final MinecraftToken token) throws MalformedURLException {
        super("https://api.minecraftservices.com/entitlements/mcstore");

        this.setHeader(HttpHeaders.AUTHORIZATION, token.getAuthorizationHeader());
    }

    @Override
    public MinecraftEntitlements handle(final HttpResponse response, final GsonObject json) throws IOException {
        return new MinecraftEntitlements(
                json.reqArray("items").stream().map(item -> item.asObject().reqString("name")).collect(Collectors.toSet())
        );
    }

}
