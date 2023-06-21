/*
 * This file is part of MinecraftAuth - https://github.com/RaphiMC/MinecraftAuth
 * Copyright (C) 2023 RK_01/RaphiMC and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.raphimc.mcauth.step.msa;

import org.apache.http.*;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.ContentType;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

public class MsaCredentialsResponseHandler implements ResponseHandler<String> {

    @Override
    public String handleResponse(HttpResponse response) throws IOException {
        final StatusLine statusLine = response.getStatusLine();
        final HttpEntity entity = response.getEntity();
        if (statusLine.getStatusCode() != HttpStatus.SC_MOVED_TEMPORARILY) {
            final String body = entity == null ? null : EntityUtils.toString(entity);
            if (body != null && ContentType.getOrDefault(entity).getMimeType().equals(ContentType.TEXT_HTML.getMimeType())) {
                throw new IllegalStateException("Credentials login failed");
            }
            throw new HttpResponseException(statusLine.getStatusCode(), statusLine.getReasonPhrase());
        } else {
            EntityUtils.consumeQuietly(entity);
        }

        try {
            final URI redirect = new URI(response.getFirstHeader(HttpHeaders.LOCATION).getValue());
            return URLEncodedUtils.parse(redirect, StandardCharsets.UTF_8).stream().filter(p -> p.getName().equals("code")).map(NameValuePair::getValue).findFirst().orElseThrow(() -> new IllegalStateException("Could not extract code from redirect url"));
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

}
