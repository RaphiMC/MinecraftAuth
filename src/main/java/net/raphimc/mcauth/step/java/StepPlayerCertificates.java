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
package net.raphimc.mcauth.step.java;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.raphimc.mcauth.MinecraftAuth;
import net.raphimc.mcauth.step.AbstractStep;
import net.raphimc.mcauth.util.CryptUtil;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;

public class StepPlayerCertificates extends AbstractStep<StepMCToken.MCToken, StepPlayerCertificates.PlayerCertificates> {

    public static final String PLAYER_CERTIFICATES_URL = "https://api.minecraftservices.com/player/certificates";

    public StepPlayerCertificates(AbstractStep<?, StepMCToken.MCToken> prevStep) {
        super(prevStep);
    }

    @Override
    public PlayerCertificates applyStep(HttpClient httpClient, StepMCToken.MCToken prevResult) throws Exception {
        MinecraftAuth.LOGGER.info("Getting player certificates...");

        final HttpPost httpPost = new HttpPost(PLAYER_CERTIFICATES_URL);
        httpPost.setEntity(new StringEntity("", ContentType.APPLICATION_JSON));
        httpPost.addHeader("Authorization", "Bearer " + prevResult.access_token());
        final String response = httpClient.execute(httpPost, new BasicResponseHandler());
        final JsonObject obj = JsonParser.parseString(response).getAsJsonObject();
        final JsonObject keyPair = obj.getAsJsonObject("keyPair");

        final PKCS8EncodedKeySpec encodedPrivateKey = new PKCS8EncodedKeySpec(Base64.getMimeDecoder().decode(keyPair.get("privateKey").getAsString()
                .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replace("-----END RSA PRIVATE KEY-----", ""))
        );
        final RSAPrivateKey privateKey = (RSAPrivateKey) CryptUtil.RSA_KEYFACTORY.generatePrivate(encodedPrivateKey);

        final X509EncodedKeySpec encodedPublicKey = new X509EncodedKeySpec(Base64.getMimeDecoder().decode(keyPair.get("publicKey").getAsString()
                .replace("-----BEGIN RSA PUBLIC KEY-----", "")
                .replace("-----END RSA PUBLIC KEY-----", ""))
        );
        final RSAPublicKey publicKey = (RSAPublicKey) CryptUtil.RSA_KEYFACTORY.generatePublic(encodedPublicKey);

        final PlayerCertificates result = new PlayerCertificates(
                Instant.parse(obj.get("expiresAt").getAsString()).toEpochMilli(),
                publicKey,
                privateKey,
                Base64.getMimeDecoder().decode(obj.get("publicKeySignatureV2").getAsString()),
                obj.has("publicKeySignature") ? Base64.getMimeDecoder().decode(obj.get("publicKeySignature").getAsString()) : new byte[0],
                prevResult
        );
        MinecraftAuth.LOGGER.info("Got player certificates, expires: " + Instant.ofEpochMilli(result.expireTimeMs).atZone(ZoneId.systemDefault()));
        return result;
    }

    @Override
    public PlayerCertificates refresh(HttpClient httpClient, PlayerCertificates result) throws Exception {
        if (result == null || result.isExpired()) return super.refresh(httpClient, result);

        return result;
    }

    @Override
    public PlayerCertificates fromJson(JsonObject json) throws Exception {
        final StepMCToken.MCToken prev = this.prevStep != null ? this.prevStep.fromJson(json.getAsJsonObject("prev")) : null;
        return new PlayerCertificates(
                json.get("expireTimeMs").getAsLong(),
                (RSAPublicKey) CryptUtil.RSA_KEYFACTORY.generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(json.get("publicKey").getAsString()))),
                (RSAPrivateKey) CryptUtil.RSA_KEYFACTORY.generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(json.get("privateKey").getAsString()))),
                Base64.getDecoder().decode(json.get("publicKeySignature").getAsString()),
                Base64.getDecoder().decode(json.get("legacyPublicKeySignature").getAsString()),
                prev
        );
    }

    public static final class PlayerCertificates implements AbstractStep.StepResult<StepMCToken.MCToken> {

        private final long expireTimeMs;
        private final RSAPublicKey publicKey;
        private final RSAPrivateKey privateKey;
        private final byte[] publicKeySignature;
        private final byte[] legacyPublicKeySignature;
        private final StepMCToken.MCToken prevResult;

        public PlayerCertificates(long expireTimeMs, RSAPublicKey publicKey, RSAPrivateKey privateKey, byte[] publicKeySignature, byte[] legacyPublicKeySignature, StepMCToken.MCToken prevResult) {
            this.expireTimeMs = expireTimeMs;
            this.publicKey = publicKey;
            this.privateKey = privateKey;
            this.publicKeySignature = publicKeySignature;
            this.legacyPublicKeySignature = legacyPublicKeySignature;
            this.prevResult = prevResult;
        }

        @Override
        public JsonObject toJson() {
            final JsonObject json = new JsonObject();
            json.addProperty("expireTimeMs", this.expireTimeMs);
            json.addProperty("publicKey", Base64.getEncoder().encodeToString(this.publicKey.getEncoded()));
            json.addProperty("privateKey", Base64.getEncoder().encodeToString(this.privateKey.getEncoded()));
            json.addProperty("publicKeySignature", Base64.getEncoder().encodeToString(this.publicKeySignature));
            json.addProperty("legacyPublicKeySignature", Base64.getEncoder().encodeToString(this.legacyPublicKeySignature));
            if (this.prevResult != null) json.add("prev", this.prevResult.toJson());
            return json;
        }

        @Override
        public boolean isExpired() {
            return this.expireTimeMs <= System.currentTimeMillis();
        }

        public long expireTimeMs() {
            return expireTimeMs;
        }

        public RSAPublicKey publicKey() {
            return publicKey;
        }

        public RSAPrivateKey privateKey() {
            return privateKey;
        }

        public byte[] publicKeySignature() {
            return publicKeySignature;
        }

        public byte[] legacyPublicKeySignature() {
            return legacyPublicKeySignature;
        }

        @Override
        public StepMCToken.MCToken prevResult() {
            return prevResult;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PlayerCertificates that = (PlayerCertificates) o;
            return expireTimeMs == that.expireTimeMs && Objects.equals(publicKey, that.publicKey) && Objects.equals(privateKey, that.privateKey) && Arrays.equals(publicKeySignature, that.publicKeySignature) && Arrays.equals(legacyPublicKeySignature, that.legacyPublicKeySignature) && Objects.equals(prevResult, that.prevResult);
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(expireTimeMs, publicKey, privateKey, prevResult);
            result = 31 * result + Arrays.hashCode(publicKeySignature);
            result = 31 * result + Arrays.hashCode(legacyPublicKeySignature);
            return result;
        }

        @Override
        public String toString() {
            return "PlayerCertificates{" +
                    "expireTimeMs=" + expireTimeMs +
                    ", publicKey=" + publicKey +
                    ", privateKey=" + privateKey +
                    ", publicKeySignature=" + Arrays.toString(publicKeySignature) +
                    ", legacyPublicKeySignature=" + Arrays.toString(legacyPublicKeySignature) +
                    ", prevResult=" + prevResult +
                    '}';
        }

    }

}
