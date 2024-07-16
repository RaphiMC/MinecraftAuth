/*
 * This file is part of MinecraftAuth - https://github.com/RaphiMC/MinecraftAuth
 * Copyright (C) 2022-2024 RK_01/RaphiMC and contributors
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
import lombok.EqualsAndHashCode;
import lombok.Value;
import net.lenni0451.commons.httpclient.HttpClient;
import net.lenni0451.commons.httpclient.requests.impl.PostRequest;
import net.raphimc.minecraftauth.responsehandler.XblResponseHandler;
import net.raphimc.minecraftauth.step.AbstractStep;
import net.raphimc.minecraftauth.util.CryptUtil;
import net.raphimc.minecraftauth.util.JsonContent;
import net.raphimc.minecraftauth.util.logging.ILogger;

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
        super("xblDeviceToken");

        this.deviceType = deviceType;
    }

    @Override
    public XblDeviceToken execute(final ILogger logger, final HttpClient httpClient, final StepResult<?> prevResult) throws Exception {
        logger.info("Authenticating device with Xbox Live...");

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

        final PostRequest postRequest = new PostRequest(XBL_DEVICE_URL);
        postRequest.setContent(new JsonContent(postData));
        postRequest.setHeader("x-xbl-contract-version", "1");
        postRequest.setHeader(CryptUtil.getSignatureHeader(postRequest, privateKey));
        final JsonObject obj = httpClient.execute(postRequest, new XblResponseHandler());

        final XblDeviceToken xblDeviceToken = new XblDeviceToken(
                publicKey,
                privateKey,
                id,
                Instant.parse(obj.get("NotAfter").getAsString()).toEpochMilli(),
                obj.get("Token").getAsString(),
                obj.getAsJsonObject("DisplayClaims").getAsJsonObject("xdi").get("did").getAsString()
        );
        logger.info("Got XBL Device Token, expires: " + Instant.ofEpochMilli(xblDeviceToken.getExpireTimeMs()).atZone(ZoneId.systemDefault()));
        return xblDeviceToken;
    }

    @Override
    public XblDeviceToken fromJson(final JsonObject json) {
        return new XblDeviceToken(
                CryptUtil.publicKeyEcFromBase64(json.get("publicKey").getAsString()),
                CryptUtil.privateKeyEcFromBase64(json.get("privateKey").getAsString()),
                UUID.fromString(json.get("id").getAsString()),
                json.get("expireTimeMs").getAsLong(),
                json.get("token").getAsString(),
                json.get("deviceId").getAsString()
        );
    }

    @Override
    public JsonObject toJson(final XblDeviceToken xblDeviceToken) {
        final JsonObject json = new JsonObject();
        json.addProperty("publicKey", Base64.getEncoder().encodeToString(xblDeviceToken.publicKey.getEncoded()));
        json.addProperty("privateKey", Base64.getEncoder().encodeToString(xblDeviceToken.privateKey.getEncoded()));
        json.addProperty("id", xblDeviceToken.id.toString());
        json.addProperty("expireTimeMs", xblDeviceToken.expireTimeMs);
        json.addProperty("token", xblDeviceToken.token);
        json.addProperty("deviceId", xblDeviceToken.deviceId);
        return json;
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class XblDeviceToken extends AbstractStep.FirstStepResult {

        ECPublicKey publicKey;
        ECPrivateKey privateKey;
        UUID id;
        long expireTimeMs;
        String token;
        String deviceId;

        @Override
        public boolean isExpired() {
            return this.expireTimeMs <= System.currentTimeMillis();
        }

    }

}
