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

import net.lenni0451.commons.httpclient.HttpResponse;
import net.lenni0451.commons.httpclient.constants.ContentTypes;
import net.lenni0451.commons.httpclient.constants.HttpHeaders;
import net.lenni0451.commons.httpclient.exceptions.HttpRequestException;
import net.lenni0451.commons.httpclient.handler.HttpResponseHandler;
import net.lenni0451.commons.httpclient.requests.impl.GetRequest;

import java.io.IOException;
import java.net.MalformedURLException;

public class RealmsClientCompatibleRequest extends GetRequest implements HttpResponseHandler<String> {

    public RealmsClientCompatibleRequest(final String host) throws MalformedURLException {
        super("https://" + host + "/mco/client/compatible");

        this.setHeader(HttpHeaders.ACCEPT, ContentTypes.TEXT_PLAIN.getMimeType());
    }

    @Override
    public String handle(final HttpResponse response) throws IOException {
        if (response.getStatusCode() >= 300) {
            throw new HttpRequestException(response);
        }
        return response.getContent().getAsString();
    }

}
