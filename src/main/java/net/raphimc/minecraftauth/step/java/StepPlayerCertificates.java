/*
 * This file is part of MinecraftAuth - https://github.com/RaphiMC/MinecraftAuth
 * Copyright (C) 2022-2025 RK_01/RaphiMC and contributors
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
package net.raphimc.minecraftauth.step.java;

import com.google.gson.JsonObject;
import lombok.EqualsAndHashCode;
import lombok.Value;
import net.lenni0451.commons.httpclient.HttpClient;
import net.lenni0451.commons.httpclient.constants.ContentTypes;
import net.lenni0451.commons.httpclient.constants.Headers;
import net.lenni0451.commons.httpclient.content.impl.StringContent;
import net.lenni0451.commons.httpclient.requests.impl.PostRequest;
import net.raphimc.minecraftauth.responsehandler.MinecraftResponseHandler;
import net.raphimc.minecraftauth.step.AbstractStep;
import net.raphimc.minecraftauth.util.CryptUtil;
import net.raphimc.minecraftauth.util.logging.ILogger;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Base64;

public class StepPlayerCertificates extends AbstractStep<StepMCToken.MCToken, StepPlayerCertificates.PlayerCertificates> {

    public static final String PLAYER_CERTIFICATES_URL = "https://api.minecraftservices.com/player/certificates";

    public StepPlayerCertificates(final AbstractStep<?, StepMCToken.MCToken> prevStep) {
        super("playerCertificates", prevStep);
    }

    @Override
    protected PlayerCertificates execute(final ILogger logger, final HttpClient httpClient, final StepMCToken.MCToken mcToken) throws Exception {
        logger.info(this, "Getting player certificates...");

        final PostRequest postRequest = new PostRequest(PLAYER_CERTIFICATES_URL);
        postRequest.setContent(new StringContent(ContentTypes.APPLICATION_JSON, ""));
        postRequest.setHeader(Headers.AUTHORIZATION, mcToken.getTokenType() + " " + mcToken.getAccessToken());
        final JsonObject obj = httpClient.execute(postRequest, new MinecraftResponseHandler());
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

        final PlayerCertificates playerCertificates = new PlayerCertificates(
                Instant.parse(obj.get("expiresAt").getAsString()).toEpochMilli(),
                publicKey,
                privateKey,
                Base64.getMimeDecoder().decode(obj.get("publicKeySignatureV2").getAsString()),
                obj.has("publicKeySignature") ? Base64.getMimeDecoder().decode(obj.get("publicKeySignature").getAsString()) : new byte[0],
                mcToken
        );
        logger.info(this, "Got player certificates, expires: " + Instant.ofEpochMilli(playerCertificates.getExpireTimeMs()).atZone(ZoneId.systemDefault()));
        return playerCertificates;
    }

    @Override
    public PlayerCertificates fromJson(final JsonObject json) {
        final StepMCToken.MCToken mcToken = this.prevStep != null ? this.prevStep.fromJson(json.getAsJsonObject(this.prevStep.name)) : null;
        return new PlayerCertificates(
                json.get("expireTimeMs").getAsLong(),
                CryptUtil.publicKeyRsaFromBase64(json.get("publicKey").getAsString()),
                CryptUtil.privateKeyRsaFromBase64(json.get("privateKey").getAsString()),
                Base64.getDecoder().decode(json.get("publicKeySignature").getAsString()),
                Base64.getDecoder().decode(json.get("legacyPublicKeySignature").getAsString()),
                mcToken
        );
    }

    @Override
    public JsonObject toJson(final PlayerCertificates playerCertificates) {
        final JsonObject json = new JsonObject();
        json.addProperty("expireTimeMs", playerCertificates.expireTimeMs);
        json.addProperty("publicKey", Base64.getEncoder().encodeToString(playerCertificates.publicKey.getEncoded()));
        json.addProperty("privateKey", Base64.getEncoder().encodeToString(playerCertificates.privateKey.getEncoded()));
        json.addProperty("publicKeySignature", Base64.getEncoder().encodeToString(playerCertificates.publicKeySignature));
        json.addProperty("legacyPublicKeySignature", Base64.getEncoder().encodeToString(playerCertificates.legacyPublicKeySignature));
        if (this.prevStep != null) json.add(this.prevStep.name, this.prevStep.toJson(playerCertificates.mcToken));
        return json;
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class PlayerCertificates extends AbstractStep.StepResult<StepMCToken.MCToken> {

        long expireTimeMs;
        RSAPublicKey publicKey;
        RSAPrivateKey privateKey;
        byte[] publicKeySignature;
        byte[] legacyPublicKeySignature;
        StepMCToken.MCToken mcToken;

        @Override
        protected StepMCToken.MCToken prevResult() {
            return this.mcToken;
        }

        @Override
        public boolean isExpired() {
            return this.expireTimeMs <= System.currentTimeMillis() || this.prevResult().isExpired();
        }

    }

}
