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
package net.raphimc.minecraftauth.step.msa;

import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import lombok.EqualsAndHashCode;
import lombok.Value;
import net.lenni0451.commons.httpclient.HttpClient;
import net.lenni0451.commons.httpclient.constants.ContentTypes;
import net.lenni0451.commons.httpclient.constants.Headers;
import net.lenni0451.commons.httpclient.constants.StatusCodes;
import net.lenni0451.commons.httpclient.content.impl.URLEncodedFormContent;
import net.lenni0451.commons.httpclient.exceptions.HttpRequestException;
import net.lenni0451.commons.httpclient.requests.impl.GetRequest;
import net.lenni0451.commons.httpclient.requests.impl.PostRequest;
import net.lenni0451.commons.httpclient.utils.URLWrapper;
import net.raphimc.minecraftauth.responsehandler.exception.InformativeHttpRequestException;
import net.raphimc.minecraftauth.responsehandler.exception.MsaRequestException;
import net.raphimc.minecraftauth.step.AbstractStep;
import net.raphimc.minecraftauth.util.JsonUtil;
import net.raphimc.minecraftauth.util.logging.ILogger;

import java.io.StringReader;
import java.net.CookieManager;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class StepCredentialsMsaCode extends MsaCodeStep<StepCredentialsMsaCode.MsaCredentials> {

    public StepCredentialsMsaCode(final ApplicationDetails applicationDetails) {
        super(applicationDetails);
    }

    @Override
    public MsaCode applyStep(final ILogger logger, final HttpClient httpClient, final MsaCredentials msaCredentials) throws Exception {
        logger.info("Trying to get MSA Code using email and password...");

        if (msaCredentials == null) {
            throw new IllegalStateException("Missing StepCredentialsMsaCode.MsaCredentials input");
        }

        final CookieManager cookieManager = new CookieManager();
        final URL authenticationUrl = new URLWrapper(this.applicationDetails.getOAuthEnvironment().getAuthorizeUrl()).wrapQuery().addQueries(this.applicationDetails.getOAuthParameters()).apply().toURL();

        final GetRequest getRequest = new GetRequest(authenticationUrl);
        getRequest.setCookieManager(cookieManager);
        getRequest.setHeader(Headers.ACCEPT, ContentTypes.TEXT_HTML.getMimeType());
        final JsonObject config = httpClient.execute(getRequest, response -> {
            if (response.getStatusCode() >= 300) {
                final Optional<String> locationHeader = response.getFirstHeader(Headers.LOCATION);
                if (locationHeader.isPresent()) {
                    final Map<String, String> parameters = new URLWrapper(locationHeader.get()).wrapQuery().getQueries();
                    if (parameters.containsKey("error") && parameters.containsKey("error_description")) {
                        throw new MsaRequestException(response, parameters.get("error"), parameters.get("error_description"));
                    }
                }
                throw new HttpRequestException(response);
            }

            return this.extractConfig(response.getContentAsString());
        });

        String urlPost;
        final Map<String, String> postData = new HashMap<>();
        switch (this.applicationDetails.getOAuthEnvironment()) {
            case LIVE: {
                urlPost = config.get("urlPost").getAsString();
                final String sFTTag = config.get("sFTTag").getAsString();

                String sFT = sFTTag.substring(sFTTag.indexOf("value=\"") + 7);
                sFT = sFT.substring(0, sFT.indexOf("\""));
                String sFTName = sFTTag.substring(sFTTag.indexOf("name=\"") + 6);
                sFTName = sFTName.substring(0, sFTName.indexOf("\""));

                postData.put("login", msaCredentials.email);
                postData.put("loginfmt", msaCredentials.email);
                postData.put("passwd", msaCredentials.password);
                postData.put(sFTName, sFT);
                break;
            }
            case MICROSOFT_ONLINE_COMMON:
            case MICROSOFT_ONLINE_CONSUMERS: {
                urlPost = config.get("urlPost").getAsString();
                urlPost = new URLWrapper(urlPost).setProtocol(authenticationUrl.getProtocol()).setHost(authenticationUrl.getHost()).toURL().toString();
                final String sFT = config.get("sFT").getAsString();
                final String sFTName = config.get("sFTName").getAsString();
                final String sCtx = config.get("sCtx").getAsString();

                postData.put("login", msaCredentials.email);
                postData.put("loginfmt", msaCredentials.email);
                postData.put("passwd", msaCredentials.password);
                postData.put("ctx", sCtx);
                postData.put(sFTName, sFT);
                break;
            }
            default:
                throw new IllegalStateException("Unsupported OAuthEnvironment: " + this.applicationDetails.getOAuthEnvironment());
        }

        final PostRequest postRequest = new PostRequest(urlPost);
        postRequest.setCookieManager(cookieManager);
        postRequest.setHeader(Headers.ACCEPT, ContentTypes.TEXT_HTML.getMimeType());
        postRequest.setContent(new URLEncodedFormContent(postData));
        final String code = httpClient.execute(postRequest, response -> {
            if (response.getStatusCode() != StatusCodes.MOVED_TEMPORARILY) {
                if (!response.getContentType().orElse(ContentTypes.TEXT_PLAIN).getMimeType().equals(ContentTypes.TEXT_HTML.getMimeType())) {
                    throw new InformativeHttpRequestException(response, "Wrong content type");
                }

                final JsonObject errorConfig = StepCredentialsMsaCode.this.extractConfig(response.getContentAsString());
                switch (StepCredentialsMsaCode.this.applicationDetails.getOAuthEnvironment()) {
                    case LIVE: {
                        if (errorConfig.has("sErrorCode") && errorConfig.has("sErrTxt")) {
                            throw new MsaRequestException(response, errorConfig.get("sErrorCode").getAsString(), errorConfig.get("sErrTxt").getAsString());
                        }
                        break;
                    }
                    case MICROSOFT_ONLINE_COMMON:
                    case MICROSOFT_ONLINE_CONSUMERS: {
                        if (errorConfig.has("iErrorCode") && errorConfig.has("strServiceExceptionMessage")) {
                            throw new MsaRequestException(response, errorConfig.get("iErrorCode").getAsString(), errorConfig.get("strServiceExceptionMessage").getAsString());
                        }
                        break;
                    }
                    default:
                        throw new IllegalStateException("Unsupported OAuthEnvironment: " + StepCredentialsMsaCode.this.applicationDetails.getOAuthEnvironment());
                }
            }

            final Optional<String> locationHeader = response.getFirstHeader(Headers.LOCATION);
            if (!locationHeader.isPresent()) {
                throw new IllegalStateException("Could not get redirect url");
            }

            final Map<String, String> parameters = new URLWrapper(locationHeader.get()).wrapQuery().getQueries();
            if (!parameters.containsKey("code")) {
                throw new IllegalStateException("Could not extract MSA Code from redirect url");
            }

            return parameters.get("code");
        });

        final MsaCode msaCode = new MsaCode(code);
        logger.info("Got MSA Code");
        return msaCode;
    }

    private JsonObject extractConfig(final String html) {
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
