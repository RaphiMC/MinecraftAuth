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
package net.raphimc.minecraftauth.msa.service.impl;

import com.google.gson.stream.JsonReader;
import net.lenni0451.commons.gson.GsonParser;
import net.lenni0451.commons.gson.elements.GsonObject;
import net.lenni0451.commons.httpclient.HttpClient;
import net.lenni0451.commons.httpclient.HttpResponse;
import net.lenni0451.commons.httpclient.constants.ContentTypes;
import net.lenni0451.commons.httpclient.constants.HttpHeaders;
import net.lenni0451.commons.httpclient.constants.StatusCodes;
import net.lenni0451.commons.httpclient.content.impl.URLEncodedFormContent;
import net.lenni0451.commons.httpclient.exceptions.HttpRequestException;
import net.lenni0451.commons.httpclient.requests.HttpRequest;
import net.lenni0451.commons.httpclient.requests.impl.GetRequest;
import net.lenni0451.commons.httpclient.requests.impl.PostRequest;
import net.lenni0451.commons.httpclient.utils.URLWrapper;
import net.raphimc.minecraftauth.msa.exception.MsaRequestException;
import net.raphimc.minecraftauth.msa.model.MsaApplicationConfig;
import net.raphimc.minecraftauth.msa.model.MsaCredentials;
import net.raphimc.minecraftauth.msa.model.MsaToken;
import net.raphimc.minecraftauth.msa.request.MsaAuthCodeTokenRequest;
import net.raphimc.minecraftauth.msa.service.MsaAuthService;
import net.raphimc.minecraftauth.util.http.exception.InformativeHttpRequestException;

import java.io.IOException;
import java.io.StringReader;
import java.net.CookieManager;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class CredentialsMsaAuthService extends MsaAuthService {

    private final MsaCredentials credentials;

    public CredentialsMsaAuthService(final HttpClient httpClient, final MsaApplicationConfig applicationConfig, final MsaCredentials credentials) {
        super(httpClient, applicationConfig);
        this.credentials = credentials;
    }

    @Override
    public MsaToken acquireToken() throws IOException {
        return this.acquireToken(this.credentials);
    }

    public MsaToken acquireToken(final MsaCredentials credentials) throws IOException {
        final CookieManager cookieManager = new CookieManager();
        final PostRequest loginRequest = this.prepareLoginRequest(credentials, cookieManager);
        final HttpResponse loginResponse = this.sendLoginRequest(loginRequest);
        final String location = loginResponse.getFirstHeader(HttpHeaders.LOCATION).orElseThrow(() -> new IllegalStateException("Could not get redirect url"));
        final String code = URLWrapper.ofURI(location).wrapQueryParameters().getFirstValue("code").orElseThrow(() -> new IllegalStateException("Could not extract auth code from redirect url"));
        return this.httpClient.executeAndHandle(new MsaAuthCodeTokenRequest(this.applicationConfig, code));
    }

    private PostRequest prepareLoginRequest(final MsaCredentials credentials, final CookieManager cookieManager) throws IOException {
        final URL authenticationUrl = URLWrapper.ofURL(this.applicationConfig.getEnvironment().getAuthorizeUrl()).wrapQueryParameters().addParameters(this.applicationConfig.getAuthCodeParameters()).apply().toURL();
        final GetRequest getRequest = new GetRequest(authenticationUrl);
        getRequest.setCookieManager(cookieManager);
        getRequest.setHeader(HttpHeaders.ACCEPT, ContentTypes.TEXT_HTML.getMimeType());
        final GsonObject config = this.httpClient.execute(getRequest, response -> {
            if (response.getStatusCode() >= 300) {
                final Optional<String> locationHeader = response.getFirstHeader(HttpHeaders.LOCATION);
                if (locationHeader.isPresent()) {
                    final URLWrapper.QueryParametersWrapper parameters = URLWrapper.ofURI(locationHeader.get()).wrapQueryParameters();
                    final Optional<String> error = parameters.getFirstValue("error");
                    final Optional<String> errorDescription = parameters.getFirstValue("error_description");
                    if (error.isPresent() && errorDescription.isPresent()) {
                        throw new MsaRequestException(response, error.get(), errorDescription.get());
                    }
                }
                throw new HttpRequestException(response);
            }
            return this.extractConfig(response.getContent().getAsString());
        });

        final String postUrl;
        final Map<String, String> postData = new HashMap<>();
        switch (this.applicationConfig.getEnvironment()) {
            case LIVE: {
                postUrl = config.reqString("urlPost");
                final String sFTTag = config.reqString("sFTTag");

                String sFT = sFTTag.substring(sFTTag.indexOf("value=\"") + 7);
                sFT = sFT.substring(0, sFT.indexOf("\""));
                String sFTName = sFTTag.substring(sFTTag.indexOf("name=\"") + 6);
                sFTName = sFTName.substring(0, sFTName.indexOf("\""));

                postData.put("login", credentials.getEmail());
                postData.put("loginfmt", credentials.getEmail());
                postData.put("passwd", credentials.getPassword());
                postData.put(sFTName, sFT);
                break;
            }
            case MICROSOFT_ONLINE_COMMON:
            case MICROSOFT_ONLINE_CONSUMERS: {
                postUrl = URLWrapper.ofURI(config.reqString("urlPost")).setProtocol(authenticationUrl.getProtocol()).setHost(authenticationUrl.getHost()).toURL().toString();
                postData.put("login", credentials.getEmail());
                postData.put("loginfmt", credentials.getEmail());
                postData.put("passwd", credentials.getPassword());
                postData.put("ctx", config.reqString("sCtx"));
                postData.put(config.reqString("sFTName"), config.reqString("sFT"));
                break;
            }
            default:
                throw new IllegalStateException("Unsupported MsaEnvironment: " + this.applicationConfig.getEnvironment());
        }

        final PostRequest postRequest = new PostRequest(postUrl);
        postRequest.setCookieManager(cookieManager);
        postRequest.setHeader(HttpHeaders.ACCEPT, ContentTypes.TEXT_HTML.getMimeType());
        postRequest.setContent(new URLEncodedFormContent(postData));
        return postRequest;
    }

    private HttpResponse sendLoginRequest(final HttpRequest request) throws IOException {
        final HttpResponse loginResponse = this.httpClient.execute(request);
        if (loginResponse.getStatusCode() != StatusCodes.MOVED_TEMPORARILY) {
            if (!loginResponse.getContent().getType().getMimeType().equals(ContentTypes.TEXT_HTML.getMimeType())) {
                throw new InformativeHttpRequestException(loginResponse, "Wrong content type");
            }

            final String responseString = loginResponse.getContent().getAsString();
            if (responseString.contains("<body onload=\"javascript:DoSubmit();\">")) { // Dialog informing the user about something. Can be skipped by getting the return url.
                String actionUrl = responseString.substring(responseString.indexOf("action=\"") + 8);
                actionUrl = actionUrl.substring(0, actionUrl.indexOf("\""));
                final String returnUrl = URLWrapper.ofURL(actionUrl).wrapQueryParameters().getFirstValue("ru").orElse(null);
                if (returnUrl == null) {
                    throw new IllegalStateException("Could not extract return url from html");
                }

                final GetRequest getRequest = new GetRequest(returnUrl);
                getRequest.setCookieManager(request.getCookieManager());
                getRequest.setHeader(HttpHeaders.ACCEPT, ContentTypes.TEXT_HTML.getMimeType());
                return this.sendLoginRequest(getRequest);
            } else {
                final GsonObject errorConfig = this.extractConfig(responseString);
                switch (this.applicationConfig.getEnvironment()) {
                    case LIVE: {
                        if (errorConfig.hasString("sErrorCode") && errorConfig.hasString("sErrTxt")) {
                            throw new MsaRequestException(loginResponse, errorConfig.reqString("sErrorCode"), errorConfig.reqString("sErrTxt"));
                        }
                        break;
                    }
                    case MICROSOFT_ONLINE_COMMON:
                    case MICROSOFT_ONLINE_CONSUMERS: {
                        if (errorConfig.hasString("iErrorCode") && errorConfig.hasString("strServiceExceptionMessage")) {
                            throw new MsaRequestException(loginResponse, errorConfig.reqString("iErrorCode"), errorConfig.reqString("strServiceExceptionMessage"));
                        }
                        break;
                    }
                    default:
                        throw new IllegalStateException("Unsupported MsaEnvironment: " + this.applicationConfig.getEnvironment());
                }
                throw new IllegalStateException("Could not extract config from html. This most likely indicates that the application config or credentials are not valid");
            }
        } else {
            return loginResponse;
        }
    }

    private GsonObject extractConfig(final String html) {
        final String configStart;
        switch (this.applicationConfig.getEnvironment()) {
            case LIVE: {
                final int configStartIndex = html.indexOf("var ServerData = ");
                if (configStartIndex == -1) {
                    throw new IllegalStateException("Could not find config start in html");
                }
                configStart = html.substring(configStartIndex + 17);
                break;
            }
            case MICROSOFT_ONLINE_COMMON:
            case MICROSOFT_ONLINE_CONSUMERS: {
                final int configStartIndex = html.indexOf("$Config=");
                if (configStartIndex == -1) {
                    throw new IllegalStateException("Could not find config start in html");
                }
                configStart = html.substring(configStartIndex + 8);
                break;
            }
            default:
                throw new IllegalStateException("Unsupported MsaEnvironment: " + this.applicationConfig.getEnvironment());
        }
        try {
            final JsonReader jsonReader = new JsonReader(new StringReader(configStart));
            jsonReader.setLenient(true);
            return GsonParser.parse(jsonReader).asObject();
        } catch (Throwable e) {
            throw new IllegalStateException("Could not extract config from html. This most likely indicates that the application config or credentials are not valid", e);
        }
    }

}
