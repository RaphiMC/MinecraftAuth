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
package net.raphimc.minecraftauth.step.msa;

import com.google.gson.JsonObject;
import lombok.EqualsAndHashCode;
import lombok.Value;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.responsehandler.MsaCredentialsResponseHandler;
import net.raphimc.minecraftauth.step.AbstractStep;
import net.raphimc.minecraftauth.util.OAuthEnvironment;
import org.apache.http.HttpHeaders;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.message.BasicNameValuePair;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class StepCredentialsMsaCode extends MsaCodeStep<StepCredentialsMsaCode.MsaCredentials> {

    private final ApplicationDetails applicationDetails;

    public StepCredentialsMsaCode(final ApplicationDetails applicationDetails) {
        super(null);

        this.applicationDetails = applicationDetails;
    }

    @Override
    public MsaCode applyStep(final HttpClient httpClient, final StepCredentialsMsaCode.MsaCredentials msaCredentials) throws Exception {
        MinecraftAuth.LOGGER.info("Trying to get MSA Code using email and password...");

        if (msaCredentials == null) {
            throw new IllegalStateException("Missing StepCredentialsMsaCode.MsaCredentials input");
        }
        if (this.applicationDetails.getOAuthEnvironment() != OAuthEnvironment.LIVE) {
            throw new IllegalStateException("Credentials can only be used with OAuthEnvironment.LIVE");
        }

        final BasicCookieStore cookieStore = new BasicCookieStore();
        final HttpClientContext context = HttpClientContext.create();
        context.setCookieStore(cookieStore);

        final HttpGet httpGet = new HttpGet(this.getAuthenticationUrl());
        httpGet.setHeader(HttpHeaders.ACCEPT, ContentType.TEXT_HTML.getMimeType());
        final String getResponse = httpClient.execute(httpGet, new BasicResponseHandler(), context);

        String urlPost = getResponse.substring(getResponse.indexOf("urlPost:"));
        urlPost = urlPost.substring(urlPost.indexOf("'") + 1);
        urlPost = urlPost.substring(0, urlPost.indexOf("'"));
        String sFTTag = getResponse.substring(getResponse.indexOf("sFTTag:"));
        sFTTag = sFTTag.substring(sFTTag.indexOf("value=\""));
        sFTTag = sFTTag.substring(sFTTag.indexOf("\"") + 1);
        sFTTag = sFTTag.substring(0, sFTTag.indexOf("\""));

        final List<NameValuePair> postData = new ArrayList<>();
        postData.add(new BasicNameValuePair("login", msaCredentials.email));
        postData.add(new BasicNameValuePair("loginfmt", msaCredentials.email));
        postData.add(new BasicNameValuePair("passwd", msaCredentials.password));
        postData.add(new BasicNameValuePair("PPFT", sFTTag));

        final HttpPost httpPost = new HttpPost(urlPost);
        httpPost.setHeader(HttpHeaders.ACCEPT, ContentType.TEXT_HTML.getMimeType());
        httpPost.setEntity(new UrlEncodedFormEntity(postData, StandardCharsets.UTF_8));
        final String code = httpClient.execute(httpPost, new MsaCredentialsResponseHandler(), context);

        final MsaCode msaCode = new MsaCode(code, this.applicationDetails);
        MinecraftAuth.LOGGER.info("Got MSA Code");
        return msaCode;
    }

    private URI getAuthenticationUrl() throws URISyntaxException {
        return new URIBuilder(this.applicationDetails.getOAuthEnvironment().getAuthorizeUrl())
                .setParameter("client_id", this.applicationDetails.getClientId())
                .setParameter("redirect_uri", this.applicationDetails.getRedirectUri())
                .setParameter("scope", this.applicationDetails.getScope())
                .setParameter("response_type", "code")
                .setParameter("response_mode", "query")
                .build();
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class MsaCredentials extends AbstractStep.InitialInput {

        String email;
        String password;

        public static MsaCredentials fromJson(final JsonObject json) {
            return new MsaCredentials(
                    json.get("email").getAsString(),
                    json.get("password").getAsString()
            );
        }

        public static JsonObject toJson(final MsaCredentials msaCredentials) {
            final JsonObject json = new JsonObject();
            json.addProperty("email", msaCredentials.email);
            json.addProperty("password", msaCredentials.password);
            return json;
        }

    }

}
