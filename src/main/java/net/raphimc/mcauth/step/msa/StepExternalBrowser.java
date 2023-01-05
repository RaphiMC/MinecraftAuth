package net.raphimc.mcauth.step.msa;

import com.google.gson.JsonObject;
import net.raphimc.mcauth.MinecraftAuth;
import net.raphimc.mcauth.step.AbstractStep;
import org.apache.http.client.HttpClient;
import org.apache.http.client.utils.URIBuilder;

import java.net.ServerSocket;
import java.net.URISyntaxException;
import java.util.Objects;

public class StepExternalBrowser extends AbstractStep<AbstractStep.StepResult<?>, StepExternalBrowser.ExternalBrowser> {

    public static final String AUTHORIZE_URL = "https://login.live.com/oauth20_authorize.srf";

    private final String clientId;
    private final String scope;

    public StepExternalBrowser(final String clientId, final String scope) {
        super(null);

        this.clientId = clientId;
        this.scope = scope;
    }

    @Override
    public StepExternalBrowser.ExternalBrowser applyStep(HttpClient httpClient, StepResult<?> prevResult) throws Exception {
        MinecraftAuth.LOGGER.info("Creating URL for MSA login via external browser...");

        try (final ServerSocket localServer = new ServerSocket(0)) {
            final int localPort = localServer.getLocalPort();

            final ExternalBrowser result = new ExternalBrowser(
                    this.getAuthenticationUrl(localPort),
                    "http://localhost" + ":" + localPort,
                    localPort);

            MinecraftAuth.LOGGER.info("Created external browser MSA authentication URL: " + result.authenticationUrl);
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
                .setParameter("redirect_uri", "http://localhost" + ":" + localPort)
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

}
