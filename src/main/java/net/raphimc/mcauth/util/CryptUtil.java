/*
 * This file is part of MinecraftAuth - https://github.com/RaphiMC/MinecraftAuth
 * Copyright (C) 2023 RK_01/RaphiMC and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.raphimc.mcauth.util;

import com.google.gson.JsonObject;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.impl.crypto.EllipticCurveProvider;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.message.BasicHeader;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.time.Instant;
import java.util.Base64;

public class CryptUtil {

    public static final KeyFactory EC_KEYFACTORY;

    static {
        try {
            EC_KEYFACTORY = KeyFactory.getInstance("EC");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Could not create EllipticCurve KeyFactory", e);
        }
    }

    public static BasicHeader getSignatureHeader(final HttpUriRequest httpRequest, final ECPrivateKey privateKey) throws IOException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        final long windowsTimestamp = (Instant.now().getEpochSecond() + 11644473600L) * 10000000L;

        final ByteArrayOutputStream signatureContent = new ByteArrayOutputStream();
        DataOutputStream data = new DataOutputStream(signatureContent);
        data.writeInt(1); // Policy Version
        data.writeByte(0); // 0 byte
        data.writeLong(windowsTimestamp); // Timestamp
        data.writeByte(0); // 0 byte
        data.write(httpRequest.getMethod().getBytes(StandardCharsets.UTF_8)); // HTTP Method
        data.writeByte(0); // 0 byte
        data.write((httpRequest.getURI().getPath() + (httpRequest.getURI().getQuery() != null ? httpRequest.getURI().getQuery() : "")).getBytes(StandardCharsets.UTF_8));
        data.writeByte(0); // 0 byte
        if (httpRequest.containsHeader("Authorization")) {
            data.write(httpRequest.getFirstHeader("Authorization").getValue().getBytes(StandardCharsets.UTF_8)); // Authorization Header
        }
        data.writeByte(0); // 0 byte
        if (httpRequest instanceof HttpEntityEnclosingRequest) {
            final InputStream content = ((HttpEntityEnclosingRequest) httpRequest).getEntity().getContent();
            final byte[] buffer = new byte[1024];
            int read;
            while ((read = content.read(buffer)) != -1) {
                data.write(buffer, 0, read); // Body
            }
        }
        data.writeByte(0); // 0 byte

        final Signature sha256withECDSA = Signature.getInstance("SHA256withECDSA");
        sha256withECDSA.initSign(privateKey);
        sha256withECDSA.update(signatureContent.toByteArray());
        final byte[] signature = sha256withECDSA.sign();

        final ByteArrayOutputStream header = new ByteArrayOutputStream();
        data = new DataOutputStream(header);
        data.writeInt(1); // Policy Version
        data.writeLong(windowsTimestamp); // Timestamp
        data.write(EllipticCurveProvider.transcodeDERToConcat(signature, EllipticCurveProvider.getSignatureByteArrayLength(SignatureAlgorithm.ES256))); // Signature

        return new BasicHeader("Signature", Base64.getEncoder().encodeToString(header.toByteArray()));
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
