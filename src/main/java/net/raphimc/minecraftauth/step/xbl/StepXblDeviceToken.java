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
package net.raphimc.minecraftauth.step.xbl;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Value;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.step.AbstractStep;
import net.raphimc.minecraftauth.util.CryptUtil;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Base64;
import java.util.UUID;

public class StepXblDeviceToken extends AbstractStep<AbstractStep.StepResult<?>, StepXblDeviceToken.XblDeviceToken> {

    public static final String XBL_DEVICE_URL = "https://device.auth.xboxlive.com/device/authenticate";

    private final String deviceType;

    public StepXblDeviceToken(final String deviceType) {
        super(null);

        this.deviceType = deviceType;
    }

    @Override
    public XblDeviceToken applyStep(final HttpClient httpClient, final StepResult<?> prevResult) throws Exception {
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
        MinecraftAuth.LOGGER.info("Got XBL Device Token, expires: " + Instant.ofEpochMilli(result.getExpireTimeMs()).atZone(ZoneId.systemDefault()));
        return result;
    }

    @Override
    public XblDeviceToken refresh(final HttpClient httpClient, final XblDeviceToken result) throws Exception {
        if (result.isExpired()) return this.applyStep(httpClient, null);

        return result;
    }

    @Override
    public XblDeviceToken fromJson(final JsonObject json) throws Exception {
        return new XblDeviceToken(
                CryptUtil.publicKeyFromBase64(json.get("publicKey").getAsString()),
                CryptUtil.privateKeyFromBase64(json.get("privateKey").getAsString()),
                UUID.fromString(json.get("id").getAsString()),
                json.get("expireTimeMs").getAsLong(),
                json.get("token").getAsString(),
                json.get("deviceId").getAsString()
        );
    }

    @Value
    public static class XblDeviceToken implements AbstractStep.StepResult<AbstractStep.StepResult<?>> {

        ECPublicKey publicKey;
        ECPrivateKey privateKey;
        UUID id;
        long expireTimeMs;
        String token;
        String deviceId;

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

    }

}
