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

import lombok.SneakyThrows;

import java.security.*;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;

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

    public static ECPublicKey ecPublicKeyFromBase64(final String base64) {
        return ecPublicKeyFromBytes(Base64.getDecoder().decode(base64));
    }

    public static ECPrivateKey ecPrivateKeyFromBase64(final String base64) {
        return ecPrivateKeyFromBytes(Base64.getDecoder().decode(base64));
    }

    public static ECPublicKey ecPublicKeyFromBytes(final byte[] bytes) {
        try {
            return (ECPublicKey) EC_KEYFACTORY.generatePublic(new X509EncodedKeySpec(bytes));
        } catch (InvalidKeySpecException e) {
            throw new RuntimeException("Could not decode public key", e);
        }
    }

    public static ECPrivateKey ecPrivateKeyFromBytes(final byte[] bytes) {
        try {
            return (ECPrivateKey) EC_KEYFACTORY.generatePrivate(new PKCS8EncodedKeySpec(bytes));
        } catch (InvalidKeySpecException e) {
            throw new RuntimeException("Could not decode private key", e);
        }
    }

    public static RSAPublicKey rsaPublicKeyFromBase64(final String base64) {
        return rsaPublicKeyFromBytes(Base64.getDecoder().decode(base64));
    }

    public static RSAPrivateKey rsaPrivateKeyFromBase64(final String base64) {
        return rsaPrivateKeyFromBytes(Base64.getDecoder().decode(base64));
    }

    public static RSAPublicKey rsaPublicKeyFromBytes(final byte[] bytes) {
        try {
            return (RSAPublicKey) RSA_KEYFACTORY.generatePublic(new X509EncodedKeySpec(bytes));
        } catch (InvalidKeySpecException e) {
            throw new RuntimeException("Could not decode public key", e);
        }
    }

    public static RSAPrivateKey rsaPrivateKeyFromBytes(final byte[] bytes) {
        try {
            return (RSAPrivateKey) RSA_KEYFACTORY.generatePrivate(new PKCS8EncodedKeySpec(bytes));
        } catch (InvalidKeySpecException e) {
            throw new RuntimeException("Could not decode private key", e);
        }
    }

    @SneakyThrows
    public static KeyPair generateEcdsa256KeyPair() {
        final KeyPairGenerator secp256r1 = KeyPairGenerator.getInstance("EC");
        secp256r1.initialize(new ECGenParameterSpec("secp256r1"));
        return secp256r1.generateKeyPair();
    }

    @SneakyThrows
    public static KeyPair generateEcdsa384KeyPair() {
        final KeyPairGenerator secp384r1 = KeyPairGenerator.getInstance("EC");
        secp384r1.initialize(new ECGenParameterSpec("secp384r1"));
        return secp384r1.generateKeyPair();
    }

    public static byte[] signSha256InP1363Format(final ECPrivateKey privateKey, final byte[] data) throws SignatureException, InvalidKeyException, NoSuchAlgorithmException {
        try { // Java 9+
            final Signature ecdsaSignature = Signature.getInstance("SHA256withECDSAinP1363Format");
            ecdsaSignature.initSign(privateKey);
            ecdsaSignature.update(data);
            return ecdsaSignature.sign();
        } catch (NoSuchAlgorithmException e) { // Fallback for Java 8
            final Signature ecdsaSignature = Signature.getInstance("SHA256withECDSA");
            ecdsaSignature.initSign(privateKey);
            ecdsaSignature.update(data);
            final byte[] derSignature = ecdsaSignature.sign();
            if (derSignature[0] != 0x30) {
                throw new IllegalArgumentException("Not a valid DER sequence");
            }

            int idx = 2;
            if (derSignature[idx] != 0x02) {
                throw new IllegalArgumentException("Expected integer for r");
            }
            final int rLen = derSignature[idx + 1];
            final byte[] rBytes = Arrays.copyOfRange(derSignature, idx + 2, idx + 2 + rLen);
            idx += 2 + rLen;

            if (derSignature[idx] != 0x02) {
                throw new IllegalArgumentException("Expected integer for s");
            }
            final int sLen = derSignature[idx + 1];
            final byte[] sBytes = Arrays.copyOfRange(derSignature, idx + 2, idx + 2 + sLen);

            final int size = privateKey.getParams().getOrder().bitLength() / Byte.SIZE;
            final byte[] concat = new byte[size * 2];
            System.arraycopy(toFixedLengthP1363(rBytes, size), 0, concat, 0, size);
            System.arraycopy(toFixedLengthP1363(sBytes, size), 0, concat, size, size);
            return concat;
        }
    }

    private static byte[] toFixedLengthP1363(final byte[] val, final int size) {
        if (val.length == size) {
            return val;
        } else if (val.length == size + 1 && val[0] == 0x00) {
            return Arrays.copyOfRange(val, 1, val.length);
        } else if (val.length < size) {
            final byte[] padded = new byte[size];
            System.arraycopy(val, 0, padded, size - val.length, val.length);
            return padded;
        } else {
            throw new IllegalArgumentException("Invalid length for ECDSA integer");
        }
    }

}
