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

import com.sun.net.httpserver.HttpServer;
import net.lenni0451.commons.httpclient.HttpClient;
import net.lenni0451.commons.httpclient.HttpResponse;
import net.lenni0451.commons.httpclient.constants.StatusCodes;
import net.lenni0451.commons.httpclient.utils.URLWrapper;
import net.raphimc.minecraftauth.msa.exception.MsaRequestException;
import net.raphimc.minecraftauth.msa.model.MsaApplicationConfig;
import net.raphimc.minecraftauth.msa.model.MsaToken;
import net.raphimc.minecraftauth.msa.request.MsaAuthCodeTokenRequest;
import net.raphimc.minecraftauth.msa.service.MsaAuthService;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

public class LocalWebServerMsaAuthService extends MsaAuthService {

    private final Consumer<URL> callback;
    private final int timeoutMs;

    public LocalWebServerMsaAuthService(final HttpClient httpClient, final MsaApplicationConfig applicationConfig, final Consumer<URL> callback) {
        this(httpClient, applicationConfig, callback, 300_000);
    }

    public LocalWebServerMsaAuthService(final HttpClient httpClient, final MsaApplicationConfig applicationConfig, final Consumer<URL> callback, final int timeoutMs) {
        super(httpClient, applicationConfig);
        if (this.applicationConfig.getRedirectUri() == null) {
            throw new IllegalArgumentException("The application config must have a redirect uri set");
        }
        this.callback = callback;
        this.timeoutMs = timeoutMs;
    }

    @Override
    public MsaToken acquireToken() throws IOException, InterruptedException, TimeoutException {
        final HttpServer httpServer = HttpServer.create(new InetSocketAddress(0), 0);
        try {
            final URLWrapper redirectUrl = URLWrapper.ofURI(this.applicationConfig.getRedirectUri()).setPort(httpServer.getAddress().getPort());
            final MsaApplicationConfig applicationConfig = this.applicationConfig.withRedirectUri(redirectUrl.toString());
            final URL authenticationUrl = URLWrapper.ofURL(applicationConfig.getEnvironment().getAuthorizeUrl()).wrapQueryParameters().addParameters(applicationConfig.getAuthCodeParameters()).addParameter("prompt", "select_account").apply().toURL();
            final CompletableFuture<String> authCodeFuture = new CompletableFuture<>();

            httpServer.createContext(redirectUrl.getPathOr("/"), httpExchange -> {
                try {
                    final URLWrapper.QueryParametersWrapper parameters = URLWrapper.of(httpExchange.getRequestURI()).wrapQueryParameters();
                    final Optional<String> error = parameters.getFirstValue("error");
                    final Optional<String> errorDescription = parameters.getFirstValue("error_description");
                    if (error.isPresent() && errorDescription.isPresent()) {
                        final HttpResponse fakeResponse = new HttpResponse(null, StatusCodes.INTERNAL_SERVER_ERROR, new byte[0], Collections.emptyMap());
                        throw new MsaRequestException(fakeResponse, error.get(), errorDescription.get());
                    }
                    final Optional<String> code = parameters.getFirstValue("code");
                    if (!code.isPresent()) {
                        throw new IllegalStateException("Could not extract auth code from response url");
                    }

                    final byte[] response = "You have been logged in! You can now close this window.".getBytes(StandardCharsets.UTF_8);
                    httpExchange.sendResponseHeaders(StatusCodes.OK, response.length);
                    httpExchange.getResponseBody().write(response);
                    httpExchange.close();
                    authCodeFuture.complete(code.get());
                } catch (Throwable e) {
                    final byte[] response = ("Login failed. Error message: " + e.getMessage()).getBytes(StandardCharsets.UTF_8);
                    httpExchange.sendResponseHeaders(StatusCodes.INTERNAL_SERVER_ERROR, response.length);
                    httpExchange.getResponseBody().write(response);
                    httpExchange.close();
                    authCodeFuture.completeExceptionally(e);
                }
            });
            httpServer.start();
            this.callback.accept(authenticationUrl);

            try {
                final String authCode = authCodeFuture.get(this.timeoutMs, TimeUnit.MILLISECONDS);
                return this.httpClient.executeAndHandle(new MsaAuthCodeTokenRequest(applicationConfig, authCode));
            } catch (TimeoutException e) {
                throw new TimeoutException("Login timed out");
            } catch (ExecutionException e) {
                if (e.getCause() instanceof RuntimeException) {
                    throw (RuntimeException) e.getCause();
                } else {
                    throw new RuntimeException(e);
                }
            }
        } finally {
            httpServer.stop(0);
        }
    }

}
