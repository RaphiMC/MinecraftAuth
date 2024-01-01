/*
 * This file is part of MinecraftAuth - https://github.com/RaphiMC/MinecraftAuth
 * Copyright (C) 2022-2024 RK_01/RaphiMC and contributors
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
package net.raphimc.minecraftauth.step.xbl;

import com.google.gson.JsonObject;
import lombok.EqualsAndHashCode;
import lombok.Value;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.responsehandler.XblResponseHandler;
import net.raphimc.minecraftauth.step.AbstractStep;
import net.raphimc.minecraftauth.step.xbl.session.StepInitialXblSession;
import net.raphimc.minecraftauth.util.CryptUtil;
import net.raphimc.minecraftauth.util.JsonUtil;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;

import java.time.Instant;
import java.time.ZoneId;

public class StepXblUserToken extends AbstractStep<StepInitialXblSession.InitialXblSession, StepXblUserToken.XblUserToken> {

    public static final String XBL_USER_URL = "https://user.auth.xboxlive.com/user/authenticate";

    public StepXblUserToken(final AbstractStep<?, StepInitialXblSession.InitialXblSession> prevStep) {
        super("xblUserToken", prevStep);
    }

    @Override
    public XblUserToken applyStep(final HttpClient httpClient, final StepInitialXblSession.InitialXblSession initialXblSession) throws Exception {
        MinecraftAuth.LOGGER.info("Authenticating user with Xbox Live...");

        final JsonObject postData = new JsonObject();
        final JsonObject properties = new JsonObject();
        properties.addProperty("AuthMethod", "RPS");
        properties.addProperty("SiteName", "user.auth.xboxlive.com");
        properties.addProperty("RpsTicket", (initialXblSession.getMsaToken().getMsaCode().getApplicationDetails().isTitleClientId() ? "t=" : "d=") + initialXblSession.getMsaToken().getAccessToken());
        if (initialXblSession.getXblDeviceToken() != null) {
            properties.add("ProofKey", CryptUtil.getProofKey(initialXblSession.getXblDeviceToken().getPublicKey()));
        }
        postData.add("Properties", properties);
        postData.addProperty("RelyingParty", "http://auth.xboxlive.com");
        postData.addProperty("TokenType", "JWT");

        final HttpPost httpPost = new HttpPost(XBL_USER_URL);
        httpPost.setEntity(new StringEntity(postData.toString(), ContentType.APPLICATION_JSON));
        httpPost.addHeader("x-xbl-contract-version", "1");
        if (initialXblSession.getXblDeviceToken() != null) {
            httpPost.addHeader(CryptUtil.getSignatureHeader(httpPost, initialXblSession.getXblDeviceToken().getPrivateKey()));
        }
        final String response = httpClient.execute(httpPost, new XblResponseHandler());
        final JsonObject obj = JsonUtil.parseString(response).getAsJsonObject();

        final XblUserToken xblUserToken = new XblUserToken(
                Instant.parse(obj.get("NotAfter").getAsString()).toEpochMilli(),
                obj.get("Token").getAsString(),
                obj.getAsJsonObject("DisplayClaims").getAsJsonArray("xui").get(0).getAsJsonObject().get("uhs").getAsString(),
                initialXblSession
        );
        MinecraftAuth.LOGGER.info("Got XBL User Token, expires: " + Instant.ofEpochMilli(xblUserToken.getExpireTimeMs()).atZone(ZoneId.systemDefault()));
        return xblUserToken;
    }

    @Override
    public XblUserToken fromJson(final JsonObject json) {
        final StepInitialXblSession.InitialXblSession initialXblSession = this.prevStep != null ? this.prevStep.fromJson(json.getAsJsonObject(this.prevStep.name)) : null;
        return new XblUserToken(
                json.get("expireTimeMs").getAsLong(),
                json.get("token").getAsString(),
                json.get("userHash").getAsString(),
                initialXblSession
        );
    }

    @Override
    public JsonObject toJson(final XblUserToken xblUserToken) {
        final JsonObject json = new JsonObject();
        json.addProperty("expireTimeMs", xblUserToken.expireTimeMs);
        json.addProperty("token", xblUserToken.token);
        json.addProperty("userHash", xblUserToken.userHash);
        if (this.prevStep != null) json.add(this.prevStep.name, this.prevStep.toJson(xblUserToken.initialXblSession));
        return json;
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class XblUserToken extends AbstractStep.StepResult<StepInitialXblSession.InitialXblSession> {

        long expireTimeMs;
        String token;
        String userHash;
        StepInitialXblSession.InitialXblSession initialXblSession;

        @Override
        protected StepInitialXblSession.InitialXblSession prevResult() {
            return this.initialXblSession;
        }

        @Override
        public boolean isExpired() {
            return this.expireTimeMs <= System.currentTimeMillis();
        }

    }

}
