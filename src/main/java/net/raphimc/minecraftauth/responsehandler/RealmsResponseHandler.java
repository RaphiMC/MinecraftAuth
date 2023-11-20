/*
 * This file is part of MinecraftAuth - https://github.com/RaphiMC/MinecraftAuth
 * Copyright (C) 2023 RK_01/RaphiMC and contributors
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
import com.google.gson.JsonParser;
import net.raphimc.minecraftauth.responsehandler.exception.RealmsResponseException;
import net.raphimc.minecraftauth.responsehandler.exception.RetryException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.entity.ContentType;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

public class RealmsResponseHandler implements ResponseHandler<String> {

    @Override
    public String handleResponse(HttpResponse response) throws IOException {
        final StatusLine statusLine = response.getStatusLine();
        final HttpEntity entity = response.getEntity();
        final String body = entity == null ? null : EntityUtils.toString(entity);
        if (statusLine.getStatusCode() >= 300) {
            if (response.containsHeader("Retry-After")) {
                final String retryAfter = response.getFirstHeader("Retry-After").getValue();
                if (retryAfter.matches("\\d+")) {
                    throw new RetryException(Integer.parseInt(retryAfter));
                }
            }

            if (body != null && ContentType.getOrDefault(entity).getMimeType().equals(ContentType.APPLICATION_JSON.getMimeType())) {
                final JsonObject obj = (JsonObject) JsonParser.parseString(body);
                if (obj.has("errorCode") && obj.has("errorMsg")) {
                    throw new RealmsResponseException(statusLine.getStatusCode(), obj.get("errorCode").getAsInt(), obj.get("errorMsg").getAsString());
                }
            }
            throw new HttpResponseException(statusLine.getStatusCode(), statusLine.getReasonPhrase());
        }
        return body;
    }

}
