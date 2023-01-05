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

public class StepMsaDeviceCode extends AbstractStep<AbstractStep.StepResult<?>, StepMsaDeviceCode.MsaDeviceCode> {

    public static final String CONNECT_URL = "https://login.live.com/oauth20_connect.srf";

    private final String clientId;
    private final String scope;

    public StepMsaDeviceCode(final String clientId, final String scope) {
        super(null);

        this.clientId = clientId;
        this.scope = scope;
    }

    @Override
    public MsaDeviceCode applyStep(HttpClient httpClient, StepResult<?> prevResult) throws Exception {
        MinecraftAuth.LOGGER.info("Getting device code for MSA login...");

        final List<NameValuePair> postData = new ArrayList<>();
        postData.add(new BasicNameValuePair("client_id", this.clientId));
        postData.add(new BasicNameValuePair("response_type", "device_code"));
        postData.add(new BasicNameValuePair("scope", this.scope));

        final HttpPost httpPost = new HttpPost(CONNECT_URL);
        httpPost.setEntity(new UrlEncodedFormEntity(postData, StandardCharsets.UTF_8));
        final String response = httpClient.execute(httpPost, new MsaResponseHandler());
        final JsonObject obj = JsonParser.parseString(response).getAsJsonObject();

        final MsaDeviceCode result = new MsaDeviceCode(
                System.currentTimeMillis() + obj.get("expires_in").getAsLong() * 1000,
                obj.get("interval").getAsLong() * 1000,
                obj.get("device_code").getAsString(),
                obj.get("user_code").getAsString(),
                obj.get("verification_uri").getAsString()
        );
        MinecraftAuth.LOGGER.info("Got MSA device code, expires: " + Instant.ofEpochMilli(result.expireTimeMs).atZone(ZoneId.systemDefault()));
        return result;
    }

    @Override
    public MsaDeviceCode fromJson(JsonObject json) throws Exception {
        return new MsaDeviceCode(
                json.get("expireTimeMs").getAsLong(),
                json.get("intervalMs").getAsLong(),
                json.get("deviceCode").getAsString(),
                json.get("userCode").getAsString(),
                json.get("verificationUrl").getAsString()
        );
    }

    public static final class MsaDeviceCode implements AbstractStep.StepResult<AbstractStep.StepResult<?>> {

        private final long expireTimeMs;
        private final long intervalMs;
        private final String deviceCode;
        private final String userCode;
        private final String verificationUri;

        public MsaDeviceCode(long expireTimeMs, long intervalMs, String deviceCode, String userCode, String verificationUri) {
            this.expireTimeMs = expireTimeMs;
            this.intervalMs = intervalMs;
            this.deviceCode = deviceCode;
            this.userCode = userCode;
            this.verificationUri = verificationUri;
        }

        @Override
        public StepResult<?> prevResult() {
            return null;
        }

        @Override
        public JsonObject toJson() {
            final JsonObject json = new JsonObject();
            json.addProperty("expireTimeMs", this.expireTimeMs);
            json.addProperty("intervalMs", this.intervalMs);
            json.addProperty("deviceCode", this.deviceCode);
            json.addProperty("userCode", this.userCode);
            json.addProperty("verificationUri", this.verificationUri);
            return json;
        }

        @Override
        public boolean isExpired() throws Exception {
            return this.expireTimeMs <= System.currentTimeMillis();
        }

        public long expireTimeMs() {
            return expireTimeMs;
        }

        public long intervalMs() {
            return intervalMs;
        }

        public String deviceCode() {
            return deviceCode;
        }

        public String userCode() {
            return userCode;
        }

        public String verificationUri() {
            return verificationUri;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            MsaDeviceCode that = (MsaDeviceCode) obj;
            return this.expireTimeMs == that.expireTimeMs &&
                    this.intervalMs == that.intervalMs &&
                    Objects.equals(this.deviceCode, that.deviceCode) &&
                    Objects.equals(this.userCode, that.userCode) &&
                    Objects.equals(this.verificationUri, that.verificationUri);
        }

        @Override
        public int hashCode() {
            return Objects.hash(expireTimeMs, intervalMs, deviceCode, userCode, verificationUri);
        }

        @Override
        public String toString() {
            return "MsaDeviceCode[" +
                    "expireTimeMs=" + expireTimeMs + ", " +
                    "intervalMs=" + intervalMs + ", " +
                    "deviceCode=" + deviceCode + ", " +
                    "userCode=" + userCode + ", " +
                    "verificationUri=" + verificationUri + ']';
        }

    }

}
