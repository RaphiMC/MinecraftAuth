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
import lombok.EqualsAndHashCode;
import lombok.Value;
import net.lenni0451.commons.httpclient.HttpClient;
import net.lenni0451.commons.httpclient.utils.URLWrapper;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.step.AbstractStep;

import java.net.ServerSocket;
import java.net.URL;
import java.util.function.Consumer;

public class StepLocalWebServer extends AbstractStep<StepLocalWebServer.LocalWebServerCallback, StepLocalWebServer.LocalWebServer> {

    private final MsaCodeStep.ApplicationDetails applicationDetails;

    public StepLocalWebServer(final MsaCodeStep.ApplicationDetails applicationDetails) {
        super("localWebServer", null);

        if (applicationDetails.getRedirectUri().endsWith("/")) {
            throw new IllegalArgumentException("Redirect URI must not end with a slash");
        }

        this.applicationDetails = applicationDetails;
    }

    @Override
    public LocalWebServer applyStep(final HttpClient httpClient, final LocalWebServerCallback localWebServerCallback) throws Exception {
        MinecraftAuth.LOGGER.info("Creating URL for MSA login via local webserver...");

        if (localWebServerCallback == null) {
            throw new IllegalStateException("Missing StepLocalWebServer.LocalWebServerCallback input");
        }

        try (final ServerSocket localServer = new ServerSocket(0)) {
            final int localPort = localServer.getLocalPort();

            final URL authenticationUrl = new URLWrapper(this.applicationDetails.getOAuthEnvironment().getAuthorizeUrl()).wrapQuery()
                    .addQueries(this.applicationDetails.getOAuthParameters())
                    .setQuery("redirect_uri", this.applicationDetails.getRedirectUri() + ":" + localPort)
                    .setQuery("prompt", "select_account")
                    .apply().toURL();

            final LocalWebServer localWebServer = new LocalWebServer(
                    authenticationUrl.toString(),
                    localPort,
                    this.applicationDetails
            );
            MinecraftAuth.LOGGER.info("Created local webserver MSA authentication URL: " + localWebServer.getAuthenticationUrl());
            localWebServerCallback.callback.accept(localWebServer);
            return localWebServer;
        }
    }

    @Override
    public LocalWebServer fromJson(final JsonObject json) {
        return new LocalWebServer(
                json.get("authenticationUrl").getAsString(),
                json.get("port").getAsInt(),
                this.applicationDetails
        );
    }

    @Override
    public JsonObject toJson(final LocalWebServer localWebServer) {
        final JsonObject json = new JsonObject();
        json.addProperty("authenticationUrl", localWebServer.authenticationUrl);
        json.addProperty("port", localWebServer.port);
        return json;
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class LocalWebServer extends AbstractStep.StepResult<MsaCodeStep.ApplicationDetails> {

        String authenticationUrl;
        int port;
        MsaCodeStep.ApplicationDetails applicationDetails;

        public LocalWebServer(final String authenticationUrl, final int port, final MsaCodeStep.ApplicationDetails applicationDetails) {
            this.authenticationUrl = authenticationUrl;
            this.port = port;
            this.applicationDetails = applicationDetails.withRedirectUri(applicationDetails.getRedirectUri() + ":" + port);
        }

        @Override
        protected MsaCodeStep.ApplicationDetails prevResult() {
            return this.applicationDetails;
        }

    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class LocalWebServerCallback extends AbstractStep.InitialInput {

        Consumer<LocalWebServer> callback;

    }

}
