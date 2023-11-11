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

public class StepExternalBrowser extends AbstractStep<StepExternalBrowser.ExternalBrowserCallback, StepExternalBrowser.ExternalBrowser> {

    public static final String AUTHORIZE_URL = "https://login.live.com/oauth20_authorize.srf";

    private final String clientId;
    private final String scope;
    private final String redirectUri;

    public StepExternalBrowser(final String clientId, final String scope, final String redirectUri) {
        super(null);

        this.clientId = clientId;
        this.scope = scope;
        this.redirectUri = redirectUri;

        if (this.redirectUri.endsWith("/")) {
            throw new IllegalArgumentException("Redirect URI must not end with a slash");
        }
    }

    @Override
    public StepExternalBrowser.ExternalBrowser applyStep(HttpClient httpClient, StepExternalBrowser.ExternalBrowserCallback prevResult) throws Exception {
        MinecraftAuth.LOGGER.info("Creating URL for MSA login via external browser...");

        if (prevResult == null) throw new IllegalStateException("Missing StepExternalBrowser.ExternalBrowserCallback input");

        try (final ServerSocket localServer = new ServerSocket(0)) {
            final int localPort = localServer.getLocalPort();

            final ExternalBrowser result = new ExternalBrowser(
                    this.getAuthenticationUrl(localPort),
                    this.redirectUri + ":" + localPort,
                    localPort);

            MinecraftAuth.LOGGER.info("Created external browser MSA authentication URL: " + result.authenticationUrl);
            prevResult.callback.accept(result);
            return result;
        }
    }

    @Override
    public ExternalBrowser fromJson(JsonObject json) {
        return new ExternalBrowser(
                json.get("authenticationUrl").getAsString(),
                json.get("redirectUri").getAsString(),
                json.get("port").getAsInt()
        );
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
    public static class ExternalBrowser extends AbstractStep.StepResult<AbstractStep.StepResult<?>> {

        String authenticationUrl;
        String redirectUri;
        int port;

        @Override
        protected StepResult<?> prevResult() {
            return null;
        }

        @Override
        public JsonObject toJson() {
            final JsonObject json = new JsonObject();
            json.addProperty("authenticationUrl", this.authenticationUrl);
            json.addProperty("redirectUri", this.redirectUri);
            json.addProperty("port", this.port);
            return json;
        }

    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class ExternalBrowserCallback extends AbstractStep.InitialInput {
        Consumer<ExternalBrowser> callback;
    }

}
