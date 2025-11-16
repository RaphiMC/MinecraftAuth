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
package net.raphimc.minecraftauth.util.http.responsehandler;

import net.lenni0451.commons.gson.GsonParser;
import net.lenni0451.commons.gson.elements.GsonObject;
import net.lenni0451.commons.httpclient.HttpResponse;
import net.lenni0451.commons.httpclient.constants.ContentTypes;
import net.lenni0451.commons.httpclient.constants.StatusCodes;
import net.lenni0451.commons.httpclient.handler.HttpResponseHandler;
import net.raphimc.minecraftauth.util.http.exception.InformativeHttpRequestException;

import java.io.IOException;

public interface JsonHttpResponseHandler<R> extends HttpResponseHandler<R> {

    @Override
    default R handle(final HttpResponse response) throws IOException {
        final String content = response.getContent().getAsString();
        if (content.isEmpty() && response.getStatusCode() == StatusCodes.NO_CONTENT) {
            return null;
        }
        if (content.isEmpty() && response.getStatusCode() >= 300) {
            throw new InformativeHttpRequestException(response, "Empty response");
        }
        if (!response.getContent().getType().getMimeType().equals(ContentTypes.APPLICATION_JSON.getMimeType())) {
            throw new InformativeHttpRequestException(response, "Wrong content type");
        }
        final GsonObject json = GsonParser.parse(content).asObject();
        if (response.getStatusCode() >= 300) {
            this.handleError(response, json);
            throw new InformativeHttpRequestException(response, content);
        }
        return this.handle(response, json);
    }

    R handle(final HttpResponse response, final GsonObject json) throws IOException;

    void handleError(final HttpResponse response, final GsonObject json) throws IOException;

}
