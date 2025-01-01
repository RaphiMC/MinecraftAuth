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
package net.raphimc.minecraftauth.util;

import com.google.gson.JsonObject;
import net.lenni0451.commons.httpclient.content.HttpContent;
import net.lenni0451.commons.httpclient.model.HttpHeader;
import net.lenni0451.commons.httpclient.requests.HttpContentRequest;
import net.lenni0451.commons.httpclient.requests.HttpRequest;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

public class CryptUtil {

    public static final KeyFactory RSA_KEYFACTORY;
    public static final KeyFactory EC_KEYFACTORY;

    static {
        try {
            RSA_KEYFACTORY = KeyFactory.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Could not create RSA KeyFactory", e);
        }
        try {
            EC_KEYFACTORY = KeyFactory.getInstance("EC");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Could not create EllipticCurve KeyFactory", e);
        }
    }

    public static <T extends PublicKey> T publicKeyEcFromBase64(final String base64) {
        try {
            return (T) EC_KEYFACTORY.generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(base64)));
        } catch (InvalidKeySpecException e) {
            throw new RuntimeException("Could not decode base64 public key", e);
        }
    }

    public static <T extends PrivateKey> T privateKeyEcFromBase64(final String base64) {
        try {
            return (T) EC_KEYFACTORY.generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(base64)));
        } catch (InvalidKeySpecException e) {
            throw new RuntimeException("Could not decode base64 private key", e);
        }
    }

    public static <T extends PublicKey> T publicKeyRsaFromBase64(final String base64) {
        try {
            return (T) RSA_KEYFACTORY.generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(base64)));
        } catch (InvalidKeySpecException e) {
            throw new RuntimeException("Could not decode base64 public key", e);
        }
    }

    public static <T extends PrivateKey> T privateKeyRsaFromBase64(final String base64) {
        try {
            return (T) RSA_KEYFACTORY.generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(base64)));
        } catch (InvalidKeySpecException e) {
            throw new RuntimeException("Could not decode base64 private key", e);
        }
    }

    public static HttpHeader getSignatureHeader(final HttpRequest httpRequest, final ECPrivateKey privateKey) throws IOException {
        final long windowsTimestamp = (Instant.now().plus(TimeUtil.getClientTimeOffset()).getEpochSecond() + 11644473600L) * 10000000L;

        final ByteArrayOutputStream signatureContent = new ByteArrayOutputStream();
        DataOutputStream data = new DataOutputStream(signatureContent);
        data.writeInt(1); // Policy Version
        data.writeByte(0); // 0 byte
        data.writeLong(windowsTimestamp); // Timestamp
        data.writeByte(0); // 0 byte
        data.write(httpRequest.getMethod().getBytes(StandardCharsets.UTF_8)); // HTTP Method
        data.writeByte(0); // 0 byte
        data.write((httpRequest.getURL().getPath() + (httpRequest.getURL().getQuery() != null ? httpRequest.getURL().getQuery() : "")).getBytes(StandardCharsets.UTF_8));
        data.writeByte(0); // 0 byte
        final Optional<String> authorizationHeader = httpRequest.getFirstHeader("Authorization");
        if (authorizationHeader.isPresent()) {
            data.write(authorizationHeader.get().getBytes(StandardCharsets.UTF_8)); // Authorization Header
        }
        data.writeByte(0); // 0 byte
        if (httpRequest instanceof HttpContentRequest) {
            final HttpContent content = ((HttpContentRequest) httpRequest).getContent();
            if (content != null) {
                data.write(content.getAsBytes());
            }
        }
        data.writeByte(0); // 0 byte

        final ByteArrayOutputStream header = new ByteArrayOutputStream();
        data = new DataOutputStream(header);
        data.writeInt(1); // Policy Version
        data.writeLong(windowsTimestamp); // Timestamp

        try {
            byte[] signature;
            try { // Java 9+ only
                final Signature ecdsaSignature = Signature.getInstance("SHA256withECDSAinP1363Format");
                ecdsaSignature.initSign(privateKey);
                ecdsaSignature.update(signatureContent.toByteArray());
                signature = ecdsaSignature.sign();
            } catch (NoSuchAlgorithmException e) { // Fallback for Java 8
                signature = JwtUtil.signES256(privateKey, signatureContent.toByteArray());
            }
            data.write(signature); // Signature
        } catch (Throwable e) {
            throw new RuntimeException("Could not sign request", e);
        }

        return new HttpHeader("Signature", Base64.getEncoder().encodeToString(header.toByteArray()));
    }

    public static JsonObject getProofKey(final ECPublicKey publicKey) {
        final JsonObject proofKey = new JsonObject();
        proofKey.addProperty("alg", "ES256");
        proofKey.addProperty("crv", "P-256");
        proofKey.addProperty("kty", "EC");
        proofKey.addProperty("use", "sig");
        proofKey.addProperty("x", encodeECCoordinate(publicKey.getParams().getCurve().getField().getFieldSize(), publicKey.getW().getAffineX()));
        proofKey.addProperty("y", encodeECCoordinate(publicKey.getParams().getCurve().getField().getFieldSize(), publicKey.getW().getAffineY()));
        return proofKey;
    }

    private static String encodeECCoordinate(final int fieldSize, final BigInteger coordinate) {
        final byte[] notPadded = bigIntegerToByteArray(coordinate);
        final int bytesToOutput = (fieldSize + 7) / 8;
        if (notPadded.length >= bytesToOutput) {
            return Base64.getUrlEncoder().withoutPadding().encodeToString(notPadded);
        }
        final byte[] padded = new byte[bytesToOutput];
        System.arraycopy(notPadded, 0, padded, bytesToOutput - notPadded.length, notPadded.length);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(padded);
    }

    private static byte[] bigIntegerToByteArray(final BigInteger bigInteger) {
        int bitlen = bigInteger.bitLength();
        bitlen = bitlen + 7 >> 3 << 3;
        final byte[] bigBytes = bigInteger.toByteArray();
        if (bigInteger.bitLength() % 8 != 0 && bigInteger.bitLength() / 8 + 1 == bitlen / 8) {
            return bigBytes;
        }
        int startSrc = 0;
        int len = bigBytes.length;
        if (bigInteger.bitLength() % 8 == 0) {
            startSrc = 1;
            --len;
        }
        final int startDst = bitlen / 8 - len;
        final byte[] resizedBytes = new byte[bitlen / 8];
        System.arraycopy(bigBytes, startSrc, resizedBytes, startDst, len);
        return resizedBytes;
    }

}
