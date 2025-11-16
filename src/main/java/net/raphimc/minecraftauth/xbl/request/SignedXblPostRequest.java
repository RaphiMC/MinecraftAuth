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
package net.raphimc.minecraftauth.xbl.request;

import com.google.gson.JsonObject;
import net.lenni0451.commons.httpclient.content.HttpContent;
import net.lenni0451.commons.httpclient.requests.HttpContentRequest;
import net.lenni0451.commons.httpclient.requests.impl.PostRequest;
import net.raphimc.minecraftauth.util.CryptUtil;
import net.raphimc.minecraftauth.util.TimeUtil;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

public abstract class SignedXblPostRequest extends PostRequest {

    public SignedXblPostRequest(final String url) throws MalformedURLException {
        super(url);
    }

    public SignedXblPostRequest(final URL url) {
        super(url);
    }

    protected void appendSignatureHeader(final ECPrivateKey privateKey) {
        final long windowsTimestamp = (Instant.now().plus(TimeUtil.getClientTimeOffset()).getEpochSecond() + 11644473600L) * 10000000L;
        final ByteArrayOutputStream headerData = new ByteArrayOutputStream();
        try {
            final ByteArrayOutputStream signatureContent = new ByteArrayOutputStream();
            DataOutputStream data = new DataOutputStream(signatureContent);
            data.writeInt(1); // Policy Version
            data.writeByte(0); // 0 byte
            data.writeLong(windowsTimestamp); // Timestamp
            data.writeByte(0); // 0 byte
            data.write(this.getMethod().getBytes(StandardCharsets.UTF_8)); // HTTP Method
            data.writeByte(0); // 0 byte
            data.write((this.getURL().getPath() + (this.getURL().getQuery() != null ? this.getURL().getQuery() : "")).getBytes(StandardCharsets.UTF_8));
            data.writeByte(0); // 0 byte
            final Optional<String> authorizationHeader = this.getFirstHeader("Authorization");
            if (authorizationHeader.isPresent()) {
                data.write(authorizationHeader.get().getBytes(StandardCharsets.UTF_8)); // Authorization Header
            }
            data.writeByte(0); // 0 byte
            if (this instanceof HttpContentRequest) {
                final HttpContent content = this.getContent();
                if (content != null) {
                    data.write(content.getAsBytes());
                }
            }
            data.writeByte(0); // 0 byte

            data = new DataOutputStream(headerData);
            data.writeInt(1); // Policy Version
            data.writeLong(windowsTimestamp); // Timestamp
            data.write(CryptUtil.signSha256InP1363Format(privateKey, signatureContent.toByteArray())); // Signature
        } catch (Throwable e) {
            throw new RuntimeException("Could not sign request", e);
        }
        this.appendHeader("Signature", Base64.getEncoder().encodeToString(headerData.toByteArray()));
    }

    protected JsonObject getProofKey(final ECPublicKey publicKey) {
        final JsonObject proofKey = new JsonObject();
        proofKey.addProperty("kty", "EC");
        proofKey.addProperty("alg", "ES256");
        proofKey.addProperty("crv", "P-256");
        proofKey.addProperty("use", "sig");
        proofKey.addProperty("x", this.encodeEcCoordinate(publicKey.getParams().getCurve().getField().getFieldSize(), publicKey.getW().getAffineX()));
        proofKey.addProperty("y", this.encodeEcCoordinate(publicKey.getParams().getCurve().getField().getFieldSize(), publicKey.getW().getAffineY()));
        return proofKey;
    }

    private String encodeEcCoordinate(final int fieldSize, final BigInteger coordinate) {
        final byte[] notPadded = this.bigIntegerToByteArray(coordinate);
        final int bytesToOutput = (fieldSize + 7) / 8;
        if (notPadded.length >= bytesToOutput) {
            return Base64.getUrlEncoder().withoutPadding().encodeToString(notPadded);
        }
        final byte[] padded = new byte[bytesToOutput];
        System.arraycopy(notPadded, 0, padded, bytesToOutput - notPadded.length, notPadded.length);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(padded);
    }

    private byte[] bigIntegerToByteArray(final BigInteger bigInteger) {
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
