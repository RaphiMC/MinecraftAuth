package net.raphimc.mcauth.step.msa;

import com.google.gson.JsonObject;
import net.raphimc.mcauth.MinecraftAuth;
import net.raphimc.mcauth.step.AbstractStep;
import org.apache.http.*;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.*;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class StepCredentialsMsaCode extends MsaCodeStep<StepCredentialsMsaCode.MsaCredentials> {

    public static final String AUTHORIZE_URL = "https://login.live.com/oauth20_authorize.srf";

    private final String redirectUri;

    public StepCredentialsMsaCode(String clientId, String scope, final String redirectUri) {
        super(null, clientId, scope);

        this.redirectUri = redirectUri;
    }

    @Override
    public MsaCode applyStep(HttpClient httpClient, StepCredentialsMsaCode.MsaCredentials prevResult) throws Exception {
        MinecraftAuth.LOGGER.info("Trying to get MSA Code using email and password...");

        if (prevResult == null) throw new IllegalStateException("Missing login credentials");

        final BasicCookieStore cookieStore = new BasicCookieStore();
        final HttpClientContext context = HttpClientContext.create();
        context.setCookieStore(cookieStore);

        final HttpGet httpGet = new HttpGet(this.getAuthenticationUrl());
        httpGet.setHeader(HttpHeaders.ACCEPT, ContentType.TEXT_HTML.getMimeType());
        final String getResponse = httpClient.execute(httpGet, new BasicResponseHandler(), context);

        String urlPost = getResponse.substring(getResponse.indexOf("urlPost:"));
        urlPost = urlPost.substring(urlPost.indexOf("'") + 1);
        urlPost = urlPost.substring(0, urlPost.indexOf("'"));
        String sFTTag = getResponse.substring(getResponse.indexOf("sFTTag:"));
        sFTTag = sFTTag.substring(sFTTag.indexOf("value=\""));
        sFTTag = sFTTag.substring(sFTTag.indexOf("\"") + 1);
        sFTTag = sFTTag.substring(0, sFTTag.indexOf("\""));

        final List<NameValuePair> postData = new ArrayList<>();
        postData.add(new BasicNameValuePair("login", prevResult.email));
        postData.add(new BasicNameValuePair("loginfmt", prevResult.email));
        postData.add(new BasicNameValuePair("passwd", prevResult.password));
        postData.add(new BasicNameValuePair("PPFT", sFTTag));

        final HttpPost httpPost = new HttpPost(urlPost);
        httpPost.setHeader(HttpHeaders.ACCEPT, ContentType.TEXT_HTML.getMimeType());
        httpPost.setEntity(new UrlEncodedFormEntity(postData, StandardCharsets.UTF_8));
        try (final CloseableHttpResponse postResponse = (CloseableHttpResponse) httpClient.execute(httpPost, context)) {
            EntityUtils.consume(postResponse.getEntity());
            final StatusLine statusLine = postResponse.getStatusLine();
            if (statusLine.getStatusCode() != HttpStatus.SC_MOVED_TEMPORARILY) {
                throw new HttpResponseException(statusLine.getStatusCode(), statusLine.getReasonPhrase());
            }
            final URI redirect = new URI(postResponse.getFirstHeader(HttpHeaders.LOCATION).getValue());
            final String code = URLEncodedUtils.parse(redirect, StandardCharsets.UTF_8).stream().filter(p -> p.getName().equals("code")).map(NameValuePair::getValue).findFirst().orElseThrow(() -> new IllegalStateException("Could not extract code from redirect url"));

            final MsaCode result = new MsaCode(code, this.clientId, this.scope, this.redirectUri);
            MinecraftAuth.LOGGER.info("Got MSA Code");
            return result;
        }
    }

    private URI getAuthenticationUrl() throws URISyntaxException {
        return new URIBuilder(AUTHORIZE_URL)
                .setParameter("client_id", this.clientId)
                .setParameter("redirect_uri", this.redirectUri)
                .setParameter("response_type", "code")
                .setParameter("scope", this.scope)
                .build();
    }

    public static final class MsaCredentials implements AbstractStep.StepResult<AbstractStep.StepResult<?>> {

        private final String email;
        private final String password;

        public MsaCredentials(String email, String password) {
            this.email = email;
            this.password = password;
        }

        public static MsaCredentials fromJson(final JsonObject json) {
            return new MsaCredentials(json.get("email").getAsString(), json.get("password").getAsString());
        }

        @Override
        public StepResult<?> prevResult() {
            return null;
        }

        @Override
        public JsonObject toJson() {
            final JsonObject json = new JsonObject();
            json.addProperty("email", this.email);
            json.addProperty("password", this.password);
            return json;
        }

        public String email() {
            return email;
        }

        public String password() {
            return password;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            MsaCredentials that = (MsaCredentials) obj;
            return Objects.equals(this.email, that.email) &&
                    Objects.equals(this.password, that.password);
        }

        @Override
        public int hashCode() {
            return Objects.hash(email, password);
        }

        @Override
        public String toString() {
            return "MsaCredentials[" +
                    "email=" + email + ", " +
                    "password=" + password + ']';
        }

    }

}
