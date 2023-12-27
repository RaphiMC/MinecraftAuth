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
import com.google.gson.stream.JsonReader;
import lombok.EqualsAndHashCode;
import lombok.Value;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.responsehandler.exception.MsaResponseException;
import net.raphimc.minecraftauth.step.AbstractStep;
import net.raphimc.minecraftauth.util.JsonUtil;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

        final BasicCookieStore cookieStore = new BasicCookieStore();
        final HttpClientContext context = HttpClientContext.create();
        context.setCookieStore(cookieStore);

        final URI authenticationUrl = this.getAuthenticationUrl();
        final HttpGet httpGet = new HttpGet(authenticationUrl);
        httpGet.setHeader(HttpHeaders.ACCEPT, ContentType.TEXT_HTML.getMimeType());
        final HttpResponse getResponse = httpClient.execute(httpGet, context);
        if (getResponse.getStatusLine().getStatusCode() >= 300) {
            EntityUtils.consumeQuietly(getResponse.getEntity());
            if (getResponse.containsHeader(HttpHeaders.LOCATION)) {
                final URI redirect = new URI(getResponse.getFirstHeader(HttpHeaders.LOCATION).getValue());
                final Map<String, String> parameters = URLEncodedUtils.parse(redirect, StandardCharsets.UTF_8).stream().collect(Collectors.toMap(NameValuePair::getName, NameValuePair::getValue));
                if (parameters.containsKey("error") && parameters.containsKey("error_description")) {
                    throw new MsaResponseException(getResponse.getStatusLine().getStatusCode(), parameters.get("error"), parameters.get("error_description"));
                }
            }
            throw new HttpResponseException(getResponse.getStatusLine().getStatusCode(), getResponse.getStatusLine().getReasonPhrase());
        }
        final String getBody = EntityUtils.toString(getResponse.getEntity());
        final JsonObject config = this.extractConfig(getBody);

        String urlPost;
        final List<NameValuePair> postData = new ArrayList<>();
        switch (this.applicationDetails.getOAuthEnvironment()) {
            case LIVE: {
                urlPost = config.get("urlPost").getAsString();
                final String sFTTag = config.get("sFTTag").getAsString();

                String sFT = sFTTag.substring(sFTTag.indexOf("value=\"") + 7);
                sFT = sFT.substring(0, sFT.indexOf("\""));
                String sFTName = sFTTag.substring(sFTTag.indexOf("name=\"") + 6);
                sFTName = sFTName.substring(0, sFTName.indexOf("\""));

                postData.add(new BasicNameValuePair("login", msaCredentials.email));
                postData.add(new BasicNameValuePair("loginfmt", msaCredentials.email));
                postData.add(new BasicNameValuePair("passwd", msaCredentials.password));
                postData.add(new BasicNameValuePair(sFTName, sFT));
                break;
            }
            case MICROSOFT_ONLINE_COMMON:
            case MICROSOFT_ONLINE_CONSUMERS: {
                urlPost = config.get("urlPost").getAsString();
                urlPost = new URIBuilder(urlPost).setScheme(authenticationUrl.getScheme()).setHost(authenticationUrl.getHost()).build().toString();
                final String sFT = config.get("sFT").getAsString();
                final String sFTName = config.get("sFTName").getAsString();
                final String sCtx = config.get("sCtx").getAsString();

                postData.add(new BasicNameValuePair("login", msaCredentials.email));
                postData.add(new BasicNameValuePair("loginfmt", msaCredentials.email));
                postData.add(new BasicNameValuePair("passwd", msaCredentials.password));
                postData.add(new BasicNameValuePair("ctx", sCtx));
                postData.add(new BasicNameValuePair(sFTName, sFT));
                break;
            }
            default:
                throw new IllegalStateException("Unsupported OAuthEnvironment: " + this.applicationDetails.getOAuthEnvironment());
        }

        final HttpPost httpPost = new HttpPost(urlPost);
        httpPost.setHeader(HttpHeaders.ACCEPT, ContentType.TEXT_HTML.getMimeType());
        httpPost.setEntity(new UrlEncodedFormEntity(postData, StandardCharsets.UTF_8));
        final HttpResponse postResponse = httpClient.execute(httpPost, context);
        if (postResponse.getStatusLine().getStatusCode() != HttpStatus.SC_MOVED_TEMPORARILY) {
            final String body = postResponse.getEntity() == null ? null : EntityUtils.toString(postResponse.getEntity());
            if (body != null && ContentType.getOrDefault(postResponse.getEntity()).getMimeType().equals(ContentType.TEXT_HTML.getMimeType())) {
                final JsonObject errorConfig = this.extractConfig(body);
                switch (this.applicationDetails.getOAuthEnvironment()) {
                    case LIVE: {
                        if (errorConfig.has("sErrorCode") && errorConfig.has("sErrTxt")) {
                            throw new MsaResponseException(postResponse.getStatusLine().getStatusCode(), errorConfig.get("sErrorCode").getAsString(), errorConfig.get("sErrTxt").getAsString());
                        }
                        break;
                    }
                    case MICROSOFT_ONLINE_COMMON:
                    case MICROSOFT_ONLINE_CONSUMERS: {
                        if (errorConfig.has("iErrorCode") && errorConfig.has("strServiceExceptionMessage")) {
                            throw new MsaResponseException(postResponse.getStatusLine().getStatusCode(), errorConfig.get("iErrorCode").getAsString(), errorConfig.get("strServiceExceptionMessage").getAsString());
                        }
                        break;
                    }
                    default:
                        throw new IllegalStateException("Unsupported OAuthEnvironment: " + this.applicationDetails.getOAuthEnvironment());
                }
            }
            throw new HttpResponseException(postResponse.getStatusLine().getStatusCode(), postResponse.getStatusLine().getReasonPhrase());
        }

        EntityUtils.consumeQuietly(postResponse.getEntity());
        final URI redirect = new URI(postResponse.getFirstHeader(HttpHeaders.LOCATION).getValue());
        final Map<String, String> parameters = URLEncodedUtils.parse(redirect, StandardCharsets.UTF_8).stream().collect(Collectors.toMap(NameValuePair::getName, NameValuePair::getValue));
        if (!parameters.containsKey("code")) {
            throw new IllegalStateException("Could not extract MSA Code from redirect url");
        }

        final MsaCode msaCode = new MsaCode(parameters.get("code"), this.applicationDetails);
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

    protected JsonObject extractConfig(final String html) {
        switch (this.applicationDetails.getOAuthEnvironment()) {
            case LIVE: {
                final JsonReader jsonReader = new JsonReader(new StringReader(html.substring(html.indexOf("var ServerData = ") + 17)));
                jsonReader.setLenient(true);
                return JsonUtil.GSON.fromJson(jsonReader, JsonObject.class);
            }
            case MICROSOFT_ONLINE_COMMON:
            case MICROSOFT_ONLINE_CONSUMERS: {
                final JsonReader jsonReader = new JsonReader(new StringReader(html.substring(html.indexOf("$Config=") + 8)));
                jsonReader.setLenient(true);
                return JsonUtil.GSON.fromJson(jsonReader, JsonObject.class);
            }
            default:
                throw new IllegalStateException("Unsupported OAuthEnvironment: " + this.applicationDetails.getOAuthEnvironment());
        }
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
