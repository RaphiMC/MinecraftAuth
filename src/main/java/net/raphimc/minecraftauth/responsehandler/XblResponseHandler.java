/*
 * This file is part of MinecraftAuth - https://github.com/RaphiMC/MinecraftAuth
 * Copyright (C) 2022-2024 RK_01/RaphiMC and contributors
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
package net.raphimc.minecraftauth.responsehandler;

import com.google.gson.JsonObject;
import net.lenni0451.commons.httpclient.HttpResponse;
import net.raphimc.minecraftauth.responsehandler.exception.XblRequestException;

import java.io.IOException;
import java.util.Optional;

public class XblResponseHandler extends JsonHttpResponseHandler {

    @Override
    public JsonObject handle(final HttpResponse response) throws IOException {
        if (response.getStatusCode() >= 300) {
            final Optional<String> xErrHeader = response.getFirstHeader("X-Err");
            if (xErrHeader.isPresent()) {
                throw new XblRequestException(response, Long.parseLong(xErrHeader.get()));
            }
        }

        return super.handle(response);
    }

    @Override
    protected void handleJsonError(final HttpResponse response, final JsonObject obj) throws IOException {
        if (obj.has("XErr")) {
            throw new XblRequestException(response, obj.get("XErr").getAsLong());
        }
    }

}
