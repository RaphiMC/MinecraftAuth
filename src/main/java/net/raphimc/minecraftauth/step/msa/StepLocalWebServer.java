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
import net.raphimc.minecraftauth.step.AbstractStep;
import org.apache.http.client.HttpClient;
import org.apache.http.client.utils.URIBuilder;

import java.net.ServerSocket;
import java.net.URISyntaxException;
import java.util.function.Consumer;

public class StepLocalWebServer extends AbstractStep<StepLocalWebServer.LocalWebServerCallback, StepLocalWebServer.LocalWebServer> {

    public static final String AUTHORIZE_URL = "https://login.live.com/oauth20_authorize.srf";
    // public static final String AUTHORIZE_URL = "https://login.microsoftonline.com/consumers/oauth2/v2.0/authorize";

    private final String clientId;
    private final String scope;
    private final String redirectUri;

    public StepLocalWebServer(final String clientId, final String scope, final String redirectUri) {
        super("localWebServer", null);

        this.clientId = clientId;
        this.scope = scope;
        this.redirectUri = redirectUri;

        if (this.redirectUri.endsWith("/")) {
            throw new IllegalArgumentException("Redirect URI must not end with a slash");
        }
    }

    @Override
    public LocalWebServer applyStep(final HttpClient httpClient, final LocalWebServerCallback localWebServerCallback) throws Exception {
        MinecraftAuth.LOGGER.info("Creating URL for MSA login via local webserver...");

        if (localWebServerCallback == null) throw new IllegalStateException("Missing StepLocalWebServer.LocalWebServerCallback input");

        try (final ServerSocket localServer = new ServerSocket(0)) {
            final int localPort = localServer.getLocalPort();

            final LocalWebServer localWebServer = new LocalWebServer(
                    this.getAuthenticationUrl(localPort),
                    this.redirectUri + ":" + localPort,
                    localPort);
            MinecraftAuth.LOGGER.info("Created local webserver MSA authentication URL: " + localWebServer.authenticationUrl);
            localWebServerCallback.callback.accept(localWebServer);
            return localWebServer;
        }
    }

    @Override
    public LocalWebServer fromJson(final JsonObject json) {
        return new LocalWebServer(
                json.get("authenticationUrl").getAsString(),
                json.get("redirectUri").getAsString(),
                json.get("port").getAsInt()
        );
    }

    @Override
    public JsonObject toJson(final LocalWebServer localWebServer) {
        final JsonObject json = new JsonObject();
        json.addProperty("authenticationUrl", localWebServer.authenticationUrl);
        json.addProperty("redirectUri", localWebServer.redirectUri);
        json.addProperty("port", localWebServer.port);
        return json;
    }

    private String getAuthenticationUrl(final int localPort) throws URISyntaxException {
        return new URIBuilder(AUTHORIZE_URL)
                .setParameter("client_id", this.clientId)
                .setParameter("redirect_uri", this.redirectUri + ":" + localPort)
                .setParameter("response_type", "code")
                .setParameter("prompt", "select_account")
                .setParameter("scope", this.scope)
                .build().toString();
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class LocalWebServer extends AbstractStep.FirstStepResult {
        String authenticationUrl;
        String redirectUri;
        int port;
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class LocalWebServerCallback extends AbstractStep.InitialInput {
        Consumer<LocalWebServer> callback;
    }

}
