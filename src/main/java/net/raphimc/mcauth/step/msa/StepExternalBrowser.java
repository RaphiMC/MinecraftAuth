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
package net.raphimc.mcauth.step.msa;

import com.google.gson.JsonObject;
import net.raphimc.mcauth.MinecraftAuth;
import net.raphimc.mcauth.step.AbstractStep;
import org.apache.http.client.HttpClient;
import org.apache.http.client.utils.URIBuilder;

import java.net.ServerSocket;
import java.net.URISyntaxException;
import java.util.Objects;
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
    public ExternalBrowser fromJson(JsonObject json) throws Exception {
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

    public static final class ExternalBrowser implements AbstractStep.StepResult<AbstractStep.StepResult<?>> {

        private final String authenticationUrl;
        private final String redirectUri;
        private final int port;

        public ExternalBrowser(String authenticationUrl, String redirectUri, int port) {
            this.authenticationUrl = authenticationUrl;
            this.redirectUri = redirectUri;
            this.port = port;
        }

        @Override
        public StepResult<?> prevResult() {
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

        public String authenticationUrl() {
            return authenticationUrl;
        }

        public String redirectUri() {
            return redirectUri;
        }

        public int port() {
            return port;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            ExternalBrowser that = (ExternalBrowser) obj;
            return Objects.equals(this.authenticationUrl, that.authenticationUrl) &&
                    Objects.equals(this.redirectUri, that.redirectUri) &&
                    this.port == that.port;
        }

        @Override
        public int hashCode() {
            return Objects.hash(authenticationUrl, redirectUri, port);
        }

        @Override
        public String toString() {
            return "ExternalBrowser[" +
                    "authenticationUrl=" + authenticationUrl + ", " +
                    "redirectUri=" + redirectUri + ", " +
                    "port=" + port + ']';
        }

    }

    public static final class ExternalBrowserCallback implements AbstractStep.InitialInput {

        private final Consumer<ExternalBrowser> callback;

        public ExternalBrowserCallback(Consumer<ExternalBrowser> callback) {
            this.callback = callback;
        }

        public Consumer<ExternalBrowser> callback() {
            return callback;
        }

    }

}
