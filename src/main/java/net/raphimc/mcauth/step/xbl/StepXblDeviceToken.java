package net.raphimc.mcauth.step.xbl;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.raphimc.mcauth.MinecraftAuth;
import net.raphimc.mcauth.step.AbstractStep;
import net.raphimc.mcauth.util.CryptUtil;
import net.raphimc.mcauth.util.XblResponseHandler;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.*;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;

public class StepXblDeviceToken extends AbstractStep<AbstractStep.StepResult<?>, StepXblDeviceToken.XblDeviceToken> {

    public static final String XBL_DEVICE_URL = "https://device.auth.xboxlive.com/device/authenticate";

    private final String deviceType;

    public StepXblDeviceToken(final String deviceType) {
        super(null);

        this.deviceType = deviceType;
    }

    @Override
    public XblDeviceToken applyStep(HttpClient httpClient, StepResult<?> prevResult) throws Exception {
        MinecraftAuth.LOGGER.info("Authenticating device with Xbox Live...");

        final UUID id = UUID.randomUUID();
        final KeyPairGenerator secp256r1 = KeyPairGenerator.getInstance("EC");
        secp256r1.initialize(new ECGenParameterSpec("secp256r1"));
        final KeyPair ecdsa256KeyPair = secp256r1.generateKeyPair();
        final ECPublicKey publicKey = (ECPublicKey) ecdsa256KeyPair.getPublic();
        final ECPrivateKey privateKey = (ECPrivateKey) ecdsa256KeyPair.getPrivate();

        final JsonObject postData = new JsonObject();
        final JsonObject properties = new JsonObject();
        properties.addProperty("AuthMethod", "ProofOfPossession");
        properties.addProperty("DeviceType", this.deviceType);
        properties.addProperty("Id", "{" + id + "}");
        properties.add("ProofKey", CryptUtil.getProofKey(publicKey));
        properties.addProperty("Version", "0.0.0");
        postData.add("Properties", properties);
        postData.addProperty("RelyingParty", "http://auth.xboxlive.com");
        postData.addProperty("TokenType", "JWT");

        final HttpPost httpPost = new HttpPost(XBL_DEVICE_URL);
        httpPost.setEntity(new StringEntity(postData.toString(), ContentType.APPLICATION_JSON));
        httpPost.addHeader("x-xbl-contract-version", "1");
        httpPost.addHeader(CryptUtil.getSignatureHeader(httpPost, privateKey));
        final String response = httpClient.execute(httpPost, new XblResponseHandler());
        final JsonObject obj = JsonParser.parseString(response).getAsJsonObject();

        final XblDeviceToken result = new XblDeviceToken(
                publicKey,
                privateKey,
                id,
                Instant.parse(obj.get("NotAfter").getAsString()).toEpochMilli(),
                obj.get("Token").getAsString(),
                obj.getAsJsonObject("DisplayClaims").getAsJsonObject("xdi").get("did").getAsString()
        );
        MinecraftAuth.LOGGER.info("Got XBL Device Token, expires: " + Instant.ofEpochMilli(result.expireTimeMs).atZone(ZoneId.systemDefault()));
        return result;
    }

    @Override
    public XblDeviceToken refresh(HttpClient httpClient, XblDeviceToken result) throws Exception {
        if (result == null || result.isExpired()) return this.applyStep(httpClient, null);

        return result;
    }

    @Override
    public XblDeviceToken fromJson(JsonObject json) throws Exception {
        return new XblDeviceToken(
                (ECPublicKey) CryptUtil.EC_KEYFACTORY.generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(json.get("publicKey").getAsString()))),
                (ECPrivateKey) CryptUtil.EC_KEYFACTORY.generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(json.get("privateKey").getAsString()))),
                UUID.fromString(json.get("id").getAsString()),
                json.get("expireTimeMs").getAsLong(),
                json.get("token").getAsString(),
                json.get("deviceId").getAsString()
        );
    }

    public static final class XblDeviceToken implements AbstractStep.StepResult<AbstractStep.StepResult<?>> {

        private final ECPublicKey publicKey;
        private final ECPrivateKey privateKey;
        private final UUID id;
        private final long expireTimeMs;
        private final String token;
        private final String deviceId;

        public XblDeviceToken(ECPublicKey publicKey, ECPrivateKey privateKey, UUID id, long expireTimeMs, String token, String deviceId) {
            this.publicKey = publicKey;
            this.privateKey = privateKey;
            this.id = id;
            this.expireTimeMs = expireTimeMs;
            this.token = token;
            this.deviceId = deviceId;
        }

        @Override
        public StepResult<?> prevResult() {
            return null;
        }

        @Override
        public JsonObject toJson() {
            final JsonObject json = new JsonObject();
            json.addProperty("publicKey", Base64.getEncoder().encodeToString(this.publicKey.getEncoded()));
            json.addProperty("privateKey", Base64.getEncoder().encodeToString(this.privateKey.getEncoded()));
            json.addProperty("id", this.id.toString());
            json.addProperty("expireTimeMs", this.expireTimeMs);
            json.addProperty("token", this.token);
            json.addProperty("deviceId", this.deviceId);
            return json;
        }

        @Override
        public boolean isExpired() {
            return this.expireTimeMs <= System.currentTimeMillis();
        }

        public ECPublicKey publicKey() {
            return publicKey;
        }

        public ECPrivateKey privateKey() {
            return privateKey;
        }

        public UUID id() {
            return id;
        }

        public long expireTimeMs() {
            return expireTimeMs;
        }

        public String token() {
            return token;
        }

        public String deviceId() {
            return deviceId;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            XblDeviceToken that = (XblDeviceToken) obj;
            return Objects.equals(this.publicKey, that.publicKey) &&
                    Objects.equals(this.privateKey, that.privateKey) &&
                    Objects.equals(this.id, that.id) &&
                    this.expireTimeMs == that.expireTimeMs &&
                    Objects.equals(this.token, that.token) &&
                    Objects.equals(this.deviceId, that.deviceId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(publicKey, privateKey, id, expireTimeMs, token, deviceId);
        }

        @Override
        public String toString() {
            return "XblDeviceToken[" +
                    "publicKey=" + publicKey + ", " +
                    "privateKey=" + privateKey + ", " +
                    "id=" + id + ", " +
                    "expireTimeMs=" + expireTimeMs + ", " +
                    "token=" + token + ", " +
                    "deviceId=" + deviceId + ']';
        }

    }

}
