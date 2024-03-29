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
import net.lenni0451.commons.httpclient.HttpClient;
import net.lenni0451.commons.httpclient.requests.impl.PostRequest;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.responsehandler.XblResponseHandler;
import net.raphimc.minecraftauth.step.AbstractStep;
import net.raphimc.minecraftauth.step.xbl.session.StepFullXblSession;
import net.raphimc.minecraftauth.step.xbl.session.StepInitialXblSession;
import net.raphimc.minecraftauth.util.CryptUtil;
import net.raphimc.minecraftauth.util.JsonContent;

import java.time.Instant;
import java.time.ZoneId;

public class StepXblSisuAuthentication extends AbstractStep<StepInitialXblSession.InitialXblSession, StepXblSisuAuthentication.XblSisuTokens> {

    public static final String XBL_SISU_URL = "https://sisu.xboxlive.com/authorize";

    private final String relyingParty;

    public StepXblSisuAuthentication(final AbstractStep<?, StepInitialXblSession.InitialXblSession> prevStep, final String relyingParty) {
        super("xblSisuAuthentication", prevStep);

        this.relyingParty = relyingParty;
    }

    @Override
    public StepXblSisuAuthentication.XblSisuTokens applyStep(final HttpClient httpClient, final StepInitialXblSession.InitialXblSession initialXblSession) throws Exception {
        MinecraftAuth.LOGGER.info("Authenticating with Xbox Live using SISU...");

        if (initialXblSession.getXblDeviceToken() == null) {
            throw new IllegalStateException("An XBL Device Token is needed for SISU authentication");
        }
        if (!initialXblSession.getMsaToken().getMsaCode().getApplicationDetails().isTitleClientId()) {
            throw new IllegalStateException("A Title Client ID is needed for SISU authentication");
        }

        final JsonObject postData = new JsonObject();
        postData.addProperty("AccessToken", "t=" + initialXblSession.getMsaToken().getAccessToken());
        postData.addProperty("DeviceToken", initialXblSession.getXblDeviceToken().getToken());
        postData.addProperty("AppId", initialXblSession.getMsaToken().getMsaCode().getApplicationDetails().getClientId());
        postData.add("ProofKey", CryptUtil.getProofKey(initialXblSession.getXblDeviceToken().getPublicKey()));
        postData.addProperty("SiteName", "user.auth.xboxlive.com");
        postData.addProperty("RelyingParty", this.relyingParty);
        postData.addProperty("Sandbox", "RETAIL");
        postData.addProperty("UseModernGamertag", true);

        final PostRequest postRequest = new PostRequest(XBL_SISU_URL);
        postRequest.setContent(new JsonContent(postData));
        postRequest.setHeader(CryptUtil.getSignatureHeader(postRequest, initialXblSession.getXblDeviceToken().getPrivateKey()));
        final JsonObject obj = httpClient.execute(postRequest, new XblResponseHandler());

        final XblSisuTokens xblSisuTokens = new XblSisuTokens(
                new XblSisuTokens.SisuTitleToken(
                        Instant.parse(obj.getAsJsonObject("TitleToken").get("NotAfter").getAsString()).toEpochMilli(),
                        obj.getAsJsonObject("TitleToken").get("Token").getAsString(),
                        obj.getAsJsonObject("TitleToken").getAsJsonObject("DisplayClaims").getAsJsonObject("xti").get("tid").getAsString()
                ),
                new XblSisuTokens.SisuUserToken(
                        Instant.parse(obj.getAsJsonObject("UserToken").get("NotAfter").getAsString()).toEpochMilli(),
                        obj.getAsJsonObject("UserToken").get("Token").getAsString(),
                        obj.getAsJsonObject("UserToken").getAsJsonObject("DisplayClaims").getAsJsonArray("xui").get(0).getAsJsonObject().get("uhs").getAsString()
                ),
                new XblSisuTokens.SisuXstsToken(
                        Instant.parse(obj.getAsJsonObject("AuthorizationToken").get("NotAfter").getAsString()).toEpochMilli(),
                        obj.getAsJsonObject("AuthorizationToken").get("Token").getAsString(),
                        obj.getAsJsonObject("AuthorizationToken").getAsJsonObject("DisplayClaims").getAsJsonArray("xui").get(0).getAsJsonObject().get("uhs").getAsString()
                ),
                initialXblSession
        );
        MinecraftAuth.LOGGER.info("Got XBL Title+User+XSTS Token, expires: " + Instant.ofEpochMilli(xblSisuTokens.getExpireTimeMs()).atZone(ZoneId.systemDefault()));
        return xblSisuTokens;
    }

    @Override
    public StepXblSisuAuthentication.XblSisuTokens fromJson(final JsonObject json) {
        final StepInitialXblSession.InitialXblSession initialXblSession = this.prevStep != null ? this.prevStep.fromJson(json.getAsJsonObject(this.prevStep.name)) : null;
        return new StepXblSisuAuthentication.XblSisuTokens(
                XblSisuTokens.SisuTitleToken.fromJson(json.getAsJsonObject("titleToken")),
                XblSisuTokens.SisuUserToken.fromJson(json.getAsJsonObject("userToken")),
                XblSisuTokens.SisuXstsToken.fromJson(json.getAsJsonObject("xstsToken")),
                initialXblSession
        );
    }

    @Override
    public JsonObject toJson(final StepXblSisuAuthentication.XblSisuTokens xblSisuTokens) {
        final JsonObject json = new JsonObject();
        json.add("titleToken", XblSisuTokens.SisuTitleToken.toJson(xblSisuTokens.titleToken));
        json.add("userToken", XblSisuTokens.SisuUserToken.toJson(xblSisuTokens.userToken));
        json.add("xstsToken", XblSisuTokens.SisuXstsToken.toJson(xblSisuTokens.xstsToken));
        if (this.prevStep != null) json.add(this.prevStep.name, this.prevStep.toJson(xblSisuTokens.initialXblSession));
        return json;
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class XblSisuTokens extends StepXblXstsToken.XblXsts<StepInitialXblSession.InitialXblSession> {

        SisuTitleToken titleToken;
        SisuUserToken userToken;
        SisuXstsToken xstsToken;
        StepInitialXblSession.InitialXblSession initialXblSession;

        @Override
        public long getExpireTimeMs() {
            return Math.min(Math.min(this.xstsToken.expireTimeMs, this.titleToken.expireTimeMs), this.userToken.expireTimeMs);
        }

        @Override
        public String getToken() {
            return this.xstsToken.token;
        }

        @Override
        public String getUserHash() {
            return this.xstsToken.userHash;
        }

        @Override
        public StepFullXblSession.FullXblSession getFullXblSession() {
            final StepXblUserToken.XblUserToken userToken = new StepXblUserToken.XblUserToken(this.userToken.expireTimeMs, this.userToken.token, this.userToken.userHash, this.initialXblSession);
            final StepXblTitleToken.XblTitleToken titleToken = new StepXblTitleToken.XblTitleToken(this.titleToken.expireTimeMs, this.titleToken.token, this.titleToken.titleId, this.initialXblSession);
            return new StepFullXblSession.FullXblSession(userToken, titleToken);
        }

        @Override
        protected StepInitialXblSession.InitialXblSession prevResult() {
            return this.initialXblSession;
        }

        @Override
        public boolean isExpired() {
            return this.getExpireTimeMs() <= System.currentTimeMillis();
        }


        @Value
        public static class SisuTitleToken {

            long expireTimeMs;
            String token;
            String titleId;

            public static SisuTitleToken fromJson(final JsonObject json) {
                return new SisuTitleToken(
                        json.get("expireTimeMs").getAsLong(),
                        json.get("token").getAsString(),
                        json.get("titleId").getAsString()
                );
            }

            public static JsonObject toJson(final SisuTitleToken sisuTitleToken) {
                final JsonObject json = new JsonObject();
                json.addProperty("expireTimeMs", sisuTitleToken.expireTimeMs);
                json.addProperty("token", sisuTitleToken.token);
                json.addProperty("titleId", sisuTitleToken.titleId);
                return json;
            }

        }

        @Value
        public static class SisuUserToken {

            long expireTimeMs;
            String token;
            String userHash;

            public static SisuUserToken fromJson(final JsonObject json) {
                return new SisuUserToken(
                        json.get("expireTimeMs").getAsLong(),
                        json.get("token").getAsString(),
                        json.get("userHash").getAsString()
                );
            }

            public static JsonObject toJson(final SisuUserToken sisuUserToken) {
                final JsonObject json = new JsonObject();
                json.addProperty("expireTimeMs", sisuUserToken.expireTimeMs);
                json.addProperty("token", sisuUserToken.token);
                json.addProperty("userHash", sisuUserToken.userHash);
                return json;
            }

        }

        @Value
        public static class SisuXstsToken {

            long expireTimeMs;
            String token;
            String userHash;

            public static SisuXstsToken fromJson(final JsonObject json) {
                return new SisuXstsToken(
                        json.get("expireTimeMs").getAsLong(),
                        json.get("token").getAsString(),
                        json.get("userHash").getAsString()
                );
            }

            public static JsonObject toJson(final SisuXstsToken sisuXstsToken) {
                final JsonObject json = new JsonObject();
                json.addProperty("expireTimeMs", sisuXstsToken.expireTimeMs);
                json.addProperty("token", sisuXstsToken.token);
                json.addProperty("userHash", sisuXstsToken.userHash);
                return json;
            }

        }

    }

}
