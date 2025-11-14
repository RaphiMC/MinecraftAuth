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
import net.lenni0451.commons.gson.elements.GsonObject;
import net.lenni0451.commons.httpclient.HttpResponse;
import net.raphimc.minecraftauth.util.http.content.JsonContent;
import net.raphimc.minecraftauth.xbl.data.XblConstants;
import net.raphimc.minecraftauth.xbl.model.XblDeviceToken;
import net.raphimc.minecraftauth.xbl.responsehandler.XblResponseHandler;

import java.net.MalformedURLException;
import java.security.KeyPair;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.time.Instant;
import java.util.UUID;

public class XblDeviceAuthenticateRequest extends SignedXblPostRequest implements XblResponseHandler<XblDeviceToken> {

    public XblDeviceAuthenticateRequest(final String deviceType, final UUID id, final KeyPair ecdsa256KeyPair) throws MalformedURLException {
        super("https://device.auth.xboxlive.com/device/authenticate");

        final JsonObject properties = new JsonObject();
        properties.addProperty("DeviceType", deviceType);
        properties.addProperty("Id", "{" + id + "}");
        properties.addProperty("AuthMethod", "ProofOfPossession");
        properties.add("ProofKey", this.getProofKey((ECPublicKey) ecdsa256KeyPair.getPublic()));
        final JsonObject postData = new JsonObject();
        postData.add("Properties", properties);
        postData.addProperty("RelyingParty", XblConstants.XBL_AUTH_RELYING_PARTY);
        postData.addProperty("TokenType", "JWT");

        this.setContent(new JsonContent(postData));
        this.setHeader("x-xbl-contract-version", "1");
        this.appendSignatureHeader((ECPrivateKey) ecdsa256KeyPair.getPrivate());
    }

    @Override
    public XblDeviceToken handle(final HttpResponse response, final GsonObject json) {
        return new XblDeviceToken(
                Instant.parse(json.reqString("NotAfter")).toEpochMilli(),
                json.reqString("Token"),
                json.reqObject("DisplayClaims").reqObject("xdi").reqString("did")
        );
    }

}
