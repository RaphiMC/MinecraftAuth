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
import net.lenni0451.commons.gson.elements.GsonObject;

import java.security.KeyPair;
import java.util.Base64;

public class JsonUtil {

    public static JsonObject encodeKeyPair(final KeyPair keyPair) {
        if (keyPair.getPublic() == null || keyPair.getPrivate() == null) {
            throw new IllegalArgumentException("KeyPair must contain both public and private key");
        }
        if (!keyPair.getPublic().getAlgorithm().equals(keyPair.getPrivate().getAlgorithm())) {
            throw new IllegalArgumentException("Public and private key must use the same algorithm");
        }

        final JsonObject json = new JsonObject();
        json.addProperty("algorithm", keyPair.getPublic().getAlgorithm());
        json.addProperty("publicKey", Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()));
        json.addProperty("privateKey", Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded()));
        return json;
    }

    public static KeyPair decodeKeyPair(final GsonObject json) {
        final String algorithm = json.reqString("algorithm");
        switch (algorithm) {
            case "RSA":
                return new KeyPair(CryptUtil.rsaPublicKeyFromBase64(json.reqString("publicKey")), CryptUtil.rsaPrivateKeyFromBase64(json.reqString("privateKey")));
            case "EC":
                return new KeyPair(CryptUtil.ecPublicKeyFromBase64(json.reqString("publicKey")), CryptUtil.ecPrivateKeyFromBase64(json.reqString("privateKey")));
            default:
                throw new IllegalArgumentException("Unsupported key algorithm: " + algorithm);
        }
    }

}
