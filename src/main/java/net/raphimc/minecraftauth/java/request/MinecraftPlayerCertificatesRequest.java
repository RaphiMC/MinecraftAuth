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
package net.raphimc.minecraftauth.java.request;

import net.lenni0451.commons.gson.elements.GsonObject;
import net.lenni0451.commons.httpclient.HttpResponse;
import net.lenni0451.commons.httpclient.constants.HttpHeaders;
import net.lenni0451.commons.httpclient.requests.impl.PostRequest;
import net.raphimc.minecraftauth.java.model.MinecraftPlayerCertificates;
import net.raphimc.minecraftauth.java.model.MinecraftToken;
import net.raphimc.minecraftauth.java.responsehandler.MinecraftServicesResponseHandler;
import net.raphimc.minecraftauth.util.CryptUtil;

import java.io.IOException;
import java.net.MalformedURLException;
import java.security.KeyPair;
import java.time.Instant;
import java.util.Base64;

public class MinecraftPlayerCertificatesRequest extends PostRequest implements MinecraftServicesResponseHandler<MinecraftPlayerCertificates> {

    public MinecraftPlayerCertificatesRequest(final MinecraftToken token) throws MalformedURLException {
        super("https://api.minecraftservices.com/player/certificates");

        this.setHeader(HttpHeaders.AUTHORIZATION, token.getAuthorizationHeader());
    }

    @Override
    public MinecraftPlayerCertificates handle(final HttpResponse response, final GsonObject json) throws IOException {
        final GsonObject keyPairJson = json.reqObject("keyPair");
        return new MinecraftPlayerCertificates(
                Instant.parse(json.reqString("expiresAt")).toEpochMilli(),
                new KeyPair(
                        CryptUtil.rsaPublicKeyFromBytes(Base64.getMimeDecoder().decode(keyPairJson.reqString("publicKey")
                                .replace("-----BEGIN RSA PUBLIC KEY-----", "")
                                .replace("-----END RSA PUBLIC KEY-----", ""))
                        ),
                        CryptUtil.rsaPrivateKeyFromBytes(Base64.getMimeDecoder().decode(keyPairJson.reqString("privateKey")
                                .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                                .replace("-----END RSA PRIVATE KEY-----", ""))
                        )
                ),
                Base64.getDecoder().decode(json.reqString("publicKeySignatureV2")),
                json.optString("publicKeySignature").map(Base64.getDecoder()::decode).orElse(null)
        );
    }

}
