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
import net.raphimc.minecraftauth.msa.model.MsaApplicationConfig;
import net.raphimc.minecraftauth.msa.model.MsaToken;
import net.raphimc.minecraftauth.util.http.content.JsonContent;
import net.raphimc.minecraftauth.xbl.model.*;
import net.raphimc.minecraftauth.xbl.responsehandler.XblResponseHandler;

import java.net.MalformedURLException;
import java.security.KeyPair;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.time.Instant;

public class XblSisuAuthorizeRequest extends SignedXblPostRequest implements XblResponseHandler<XblSisuTokens> {

    public XblSisuAuthorizeRequest(final MsaApplicationConfig applicationConfig, final MsaToken token, final XblDeviceToken deviceToken, final KeyPair ecdsa256KeyPair, final String relyingParty) throws MalformedURLException {
        super("https://sisu.xboxlive.com/authorize");
        if (!applicationConfig.isTitleClientId()) {
            throw new IllegalArgumentException("Client id must be a title client id for XBL SISU authentication");
        }

        final JsonObject postData = new JsonObject();
        postData.addProperty("Sandbox", "RETAIL");
        postData.addProperty("UseModernGamertag", true);
        postData.addProperty("AppId", applicationConfig.getClientId());
        postData.addProperty("AccessToken", "t=" + token.getAccessToken());
        postData.addProperty("DeviceToken", deviceToken.getToken());
        postData.add("ProofKey", this.getProofKey((ECPublicKey) ecdsa256KeyPair.getPublic()));
        postData.addProperty("RelyingParty", relyingParty);

        this.setContent(new JsonContent(postData));
        this.appendSignatureHeader((ECPrivateKey) ecdsa256KeyPair.getPrivate());
    }

    @Override
    public XblSisuTokens handle(final HttpResponse response, final GsonObject json) {
        final GsonObject userTokenJson = json.reqObject("UserToken");
        final GsonObject titleTokenJson = json.reqObject("TitleToken");
        final GsonObject xstsTokenJson = json.reqObject("AuthorizationToken");
        return new XblSisuTokens(
                new XblUserToken(
                        Instant.parse(userTokenJson.reqString("NotAfter")).toEpochMilli(),
                        userTokenJson.reqString("Token"),
                        userTokenJson.reqObject("DisplayClaims").reqArray("xui").get(0).asObject().reqString("uhs")
                ),
                new XblTitleToken(
                        Instant.parse(titleTokenJson.reqString("NotAfter")).toEpochMilli(),
                        titleTokenJson.reqString("Token"),
                        titleTokenJson.reqObject("DisplayClaims").reqObject("xti").reqString("tid")
                ),
                new XblXstsToken(
                        Instant.parse(xstsTokenJson.reqString("NotAfter")).toEpochMilli(),
                        xstsTokenJson.reqString("Token"),
                        xstsTokenJson.reqObject("DisplayClaims").reqArray("xui").get(0).asObject().reqString("uhs")
                )
        );
    }

}
