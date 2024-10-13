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

import com.sun.net.httpserver.HttpServer;
import lombok.SneakyThrows;
import net.lenni0451.commons.httpclient.HttpClient;
import net.lenni0451.commons.httpclient.HttpResponse;
import net.lenni0451.commons.httpclient.constants.StatusCodes;
import net.lenni0451.commons.httpclient.utils.URLWrapper;
import net.raphimc.minecraftauth.responsehandler.exception.MsaRequestException;
import net.raphimc.minecraftauth.step.AbstractStep;
import net.raphimc.minecraftauth.util.logging.ILogger;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class StepLocalWebServerMsaCode extends MsaCodeStep<StepLocalWebServer.LocalWebServer> {

    private final int timeout;

    public StepLocalWebServerMsaCode(final AbstractStep<?, StepLocalWebServer.LocalWebServer> prevStep, final int timeout) {
        super(prevStep);

        this.timeout = timeout;
    }

    @Override
    @SneakyThrows
    protected MsaCode execute(final ILogger logger, final HttpClient httpClient, final StepLocalWebServer.LocalWebServer localWebServer) throws Exception {
        logger.info(this, "Waiting for MSA login via local webserver...");

        final CompletableFuture<MsaCode> msaCodeFuture = new CompletableFuture<>();
        final HttpServer httpServer = HttpServer.create(new InetSocketAddress(localWebServer.getPort()), 0);
        httpServer.createContext("/", httpExchange -> {
            try {
                final Map<String, String> parameters = new URLWrapper(httpExchange.getRequestURI()).wrapQuery().getQueries();
                if (parameters.containsKey("error") && parameters.containsKey("error_description")) {
                    final HttpResponse fakeResponse = new HttpResponse(null, 500, new byte[0], Collections.emptyMap());
                    throw new MsaRequestException(fakeResponse, parameters.get("error"), parameters.get("error_description"));
                }
                if (!parameters.containsKey("code")) {
                    throw new IllegalStateException("Could not extract MSA Code from response url");
                }

                final byte[] response = "You have been logged in! You can now close this window.".getBytes(StandardCharsets.UTF_8);
                httpExchange.sendResponseHeaders(StatusCodes.OK, response.length);
                httpExchange.getResponseBody().write(response);
                httpExchange.close();

                msaCodeFuture.complete(new MsaCode(parameters.get("code")));
            } catch (Throwable e) {
                final byte[] response = ("Login failed. Error message: " + e.getMessage()).getBytes(StandardCharsets.UTF_8);
                httpExchange.sendResponseHeaders(StatusCodes.INTERNAL_SERVER_ERROR, response.length);
                httpExchange.getResponseBody().write(response);
                httpExchange.close();

                msaCodeFuture.completeExceptionally(e);
            }
        });
        httpServer.start();

        try {
            final MsaCode msaCode = msaCodeFuture.get(this.timeout, TimeUnit.MILLISECONDS);
            msaCode.customRedirectUri = localWebServer.getCustomRedirectUri();
            httpServer.stop(0);
            logger.info(this, "Got MSA Code");
            return msaCode;
        } catch (TimeoutException e) {
            httpServer.stop(0);
            throw new TimeoutException("MSA login timed out");
        } catch (ExecutionException e) {
            httpServer.stop(0);
            if (e.getCause() != null) {
                throw e.getCause();
            } else {
                throw e;
            }
        }
    }

}
