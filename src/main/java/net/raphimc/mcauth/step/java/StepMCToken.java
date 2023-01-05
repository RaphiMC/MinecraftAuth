package net.raphimc.mcauth.step.java;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.raphimc.mcauth.MinecraftAuth;
import net.raphimc.mcauth.step.AbstractStep;
import net.raphimc.mcauth.step.xbl.StepXblXstsToken;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;

public class StepMCToken extends AbstractStep<StepXblXstsToken.XblXsts<?>, StepMCToken.MCToken> {

    public static final String MINECRAFT_LOGIN_URL = "https://api.minecraftservices.com/authentication/login_with_xbox";

    public StepMCToken(AbstractStep<?, StepXblXstsToken.XblXsts<?>> prevStep) {
        super(prevStep);
    }

    @Override
    public MCToken applyStep(HttpClient httpClient, StepXblXstsToken.XblXsts<?> prevResult) throws Exception {
        MinecraftAuth.LOGGER.info("Authenticating with Minecraft Services...");

        final JsonObject postData = new JsonObject();
        postData.addProperty("identityToken", "XBL3.0 x=" + prevResult.userHash() + ";" + prevResult.token());

        final HttpPost httpPost = new HttpPost(MINECRAFT_LOGIN_URL);
        httpPost.setEntity(new StringEntity(postData.toString(), ContentType.APPLICATION_JSON));
        final String response = httpClient.execute(httpPost, new BasicResponseHandler());
        final JsonObject obj = JsonParser.parseString(response).getAsJsonObject();

        final MCToken result = new MCToken(
                obj.get("access_token").getAsString(),
                obj.get("token_type").getAsString(),
                System.currentTimeMillis() + obj.get("expires_in").getAsLong() * 1000,
                prevResult
        );
        MinecraftAuth.LOGGER.info("Got MC Token, expires: " + Instant.ofEpochMilli(result.expireTimeMs).atZone(ZoneId.systemDefault()));
        return result;
    }

    @Override
    public MCToken refresh(HttpClient httpClient, MCToken result) throws Exception {
        if (result == null || result.isExpired()) return super.refresh(httpClient, result);

        return result;
    }

    @Override
    public MCToken fromJson(JsonObject json) throws Exception {
        final StepXblXstsToken.XblXsts<?> prev = this.prevStep.fromJson(json.getAsJsonObject("prev"));
        return new MCToken(
                json.get("access_token").getAsString(),
                json.get("token_type").getAsString(),
                json.get("expireTimeMs").getAsLong(),
                prev
        );
    }

    public static final class MCToken implements AbstractStep.StepResult<StepXblXstsToken.XblXsts<?>> {

        private final String access_token;
        private final String token_type;
        private final long expireTimeMs;
        private final StepXblXstsToken.XblXsts<?> prevResult;

        public MCToken(String access_token, String token_type, long expireTimeMs, StepXblXstsToken.XblXsts<?> prevResult) {
            this.access_token = access_token;
            this.token_type = token_type;
            this.expireTimeMs = expireTimeMs;
            this.prevResult = prevResult;
        }

        @Override
        public JsonObject toJson() {
            final JsonObject json = new JsonObject();
            json.addProperty("access_token", this.access_token);
            json.addProperty("token_type", this.token_type);
            json.addProperty("expireTimeMs", this.expireTimeMs);
            json.add("prev", this.prevResult.toJson());
            return json;
        }

        @Override
        public boolean isExpired() {
            return this.expireTimeMs <= System.currentTimeMillis();
        }

        public String access_token() {
            return access_token;
        }

        public String token_type() {
            return token_type;
        }

        public long expireTimeMs() {
            return expireTimeMs;
        }

        @Override
        public StepXblXstsToken.XblXsts<?> prevResult() {
            return prevResult;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            MCToken that = (MCToken) obj;
            return Objects.equals(this.access_token, that.access_token) &&
                    Objects.equals(this.token_type, that.token_type) &&
                    this.expireTimeMs == that.expireTimeMs &&
                    Objects.equals(this.prevResult, that.prevResult);
        }

        @Override
        public int hashCode() {
            return Objects.hash(access_token, token_type, expireTimeMs, prevResult);
        }

        @Override
        public String toString() {
            return "MCToken[" +
                    "access_token=" + access_token + ", " +
                    "token_type=" + token_type + ", " +
                    "expireTimeMs=" + expireTimeMs + ", " +
                    "prevResult=" + prevResult + ']';
        }

    }

}
