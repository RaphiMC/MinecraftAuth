package net.raphimc.mcauth.step.msa;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.raphimc.mcauth.MinecraftAuth;
import net.raphimc.mcauth.step.AbstractStep;
import net.raphimc.mcauth.util.MsaResponseHandler;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;

public class StepMsaToken extends AbstractStep<MsaCodeStep.MsaCode, StepMsaToken.MsaToken> {

    public static final String TOKEN_URL = "https://login.live.com/oauth20_token.srf";

    public StepMsaToken(AbstractStep<?, MsaCodeStep.MsaCode> prevStep) {
        super(prevStep);
    }

    @Override
    public MsaToken applyStep(HttpClient httpClient, MsaCodeStep.MsaCode prevResult) throws Exception {
        return this.apply(httpClient, prevResult.code(), prevResult.redirectUri() != null ? "authorization_code" : "refresh_token", prevResult);
    }

    @Override
    public MsaToken refresh(HttpClient httpClient, MsaToken result) throws Exception {
        if (result == null) return super.refresh(httpClient, null);
        if (result.isExpired()) return this.apply(httpClient, result.refresh_token(), "refresh_token", result.prevResult());

        return result;
    }

    @Override
    public MsaToken fromJson(JsonObject json) throws Exception {
        final MsaCodeStep.MsaCode prev = this.prevStep.fromJson(json.getAsJsonObject("prev"));
        return new MsaToken(
                json.get("user_id").getAsString(),
                json.get("expireTimeMs").getAsLong(),
                json.get("access_token").getAsString(),
                json.get("refresh_token").getAsString(),
                prev
        );
    }

    private MsaToken apply(final HttpClient httpClient, final String code, final String type, final MsaCodeStep.MsaCode prev_result) throws Exception {
        MinecraftAuth.LOGGER.info("Getting MSA Token...");

        final List<NameValuePair> postData = new ArrayList<>();
        postData.add(new BasicNameValuePair("client_id", prev_result.clientId()));
        postData.add(new BasicNameValuePair("scope", prev_result.scope()));
        postData.add(new BasicNameValuePair("grant_type", type));
        if (type.equals("refresh_token")) {
            postData.add(new BasicNameValuePair("refresh_token", code));
        } else {
            postData.add(new BasicNameValuePair("code", code));
            postData.add(new BasicNameValuePair("redirect_uri", prev_result.redirectUri()));
        }

        final HttpPost httpPost = new HttpPost(TOKEN_URL);
        httpPost.setEntity(new UrlEncodedFormEntity(postData, StandardCharsets.UTF_8));
        final String response = httpClient.execute(httpPost, new MsaResponseHandler());
        final JsonObject obj = JsonParser.parseString(response).getAsJsonObject();

        final MsaToken result = new MsaToken(
                obj.get("user_id").getAsString(),
                System.currentTimeMillis() + obj.get("expires_in").getAsLong() * 1000,
                obj.get("access_token").getAsString(),
                obj.get("refresh_token").getAsString(),
                prev_result
        );
        MinecraftAuth.LOGGER.info("Got MSA Token, expires: " + Instant.ofEpochMilli(result.expireTimeMs).atZone(ZoneId.systemDefault()));
        return result;
    }

    public static final class MsaToken implements AbstractStep.StepResult<MsaCodeStep.MsaCode> {

        private final String user_id;
        private final long expireTimeMs;
        private final String access_token;
        private final String refresh_token;
        private final MsaCodeStep.MsaCode prevResult;

        public MsaToken(String user_id, long expireTimeMs, String access_token, String refresh_token, MsaCodeStep.MsaCode prevResult) {
            this.user_id = user_id;
            this.expireTimeMs = expireTimeMs;
            this.access_token = access_token;
            this.refresh_token = refresh_token;
            this.prevResult = prevResult;
        }

        @Override
        public JsonObject toJson() {
            final JsonObject json = new JsonObject();
            json.addProperty("user_id", this.user_id);
            json.addProperty("expireTimeMs", this.expireTimeMs);
            json.addProperty("access_token", this.access_token);
            json.addProperty("refresh_token", this.refresh_token);
            json.add("prev", this.prevResult.toJson());
            return json;
        }

        @Override
        public boolean isExpired() {
            return this.expireTimeMs <= System.currentTimeMillis();
        }

        public boolean isTitleClientId() {
            return !this.prevResult.clientId().matches("\\p{XDigit}{8}-\\p{XDigit}{4}-\\p{XDigit}{4}-\\p{XDigit}{4}-\\p{XDigit}+");
        }

        public String user_id() {
            return user_id;
        }

        public long expireTimeMs() {
            return expireTimeMs;
        }

        public String access_token() {
            return access_token;
        }

        public String refresh_token() {
            return refresh_token;
        }

        @Override
        public MsaCodeStep.MsaCode prevResult() {
            return prevResult;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            MsaToken that = (MsaToken) obj;
            return Objects.equals(this.user_id, that.user_id) &&
                    this.expireTimeMs == that.expireTimeMs &&
                    Objects.equals(this.access_token, that.access_token) &&
                    Objects.equals(this.refresh_token, that.refresh_token) &&
                    Objects.equals(this.prevResult, that.prevResult);
        }

        @Override
        public int hashCode() {
            return Objects.hash(user_id, expireTimeMs, access_token, refresh_token, prevResult);
        }

        @Override
        public String toString() {
            return "MsaToken[" +
                    "user_id=" + user_id + ", " +
                    "expireTimeMs=" + expireTimeMs + ", " +
                    "access_token=" + access_token + ", " +
                    "refresh_token=" + refresh_token + ", " +
                    "prevResult=" + prevResult + ']';
        }

    }

}
