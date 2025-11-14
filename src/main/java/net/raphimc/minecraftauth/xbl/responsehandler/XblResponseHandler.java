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
package net.raphimc.minecraftauth.xbl.responsehandler;

import net.lenni0451.commons.gson.elements.GsonObject;
import net.lenni0451.commons.httpclient.HttpResponse;
import net.raphimc.minecraftauth.util.http.responsehandler.JsonHttpResponseHandler;
import net.raphimc.minecraftauth.xbl.exception.XblRequestException;

import java.io.IOException;
import java.util.Optional;

public interface XblResponseHandler<R> extends JsonHttpResponseHandler<R> {

    @Override
    default R handle(final HttpResponse response) throws IOException {
        if (response.getStatusCode() >= 300) {
            final Optional<String> xErrHeader = response.getFirstHeader("X-Err");
            if (xErrHeader.isPresent()) {
                throw new XblRequestException(response, Long.parseLong(xErrHeader.get()));
            }
        }
        return JsonHttpResponseHandler.super.handle(response);
    }

    @Override
    default void handleError(final HttpResponse response, final GsonObject json) throws IOException {
        if (json.hasNumber("XErr")) {
            throw new XblRequestException(response, json.reqLong("XErr"));
        }
    }

}
