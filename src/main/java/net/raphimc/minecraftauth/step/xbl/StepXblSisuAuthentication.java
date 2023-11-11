/*
 * This file is part of MinecraftAuth - https://github.com/RaphiMC/MinecraftAuth
 * Copyright (C) 2023 RK_01/RaphiMC and contributors
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
import com.google.gson.JsonParser;
import lombok.EqualsAndHashCode;
import lombok.Value;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.step.AbstractStep;
import net.raphimc.minecraftauth.step.xbl.session.StepFullXblSession;
import net.raphimc.minecraftauth.step.xbl.session.StepInitialXblSession;
import net.raphimc.minecraftauth.util.CryptUtil;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;

import java.time.Instant;
import java.time.ZoneId;

public class StepXblSisuAuthentication extends AbstractStep<StepInitialXblSession.InitialXblSession, StepXblXstsToken.XblXsts<?>> {

    public static final String XBL_SISU_URL = "https://sisu.xboxlive.com/authorize";

    private final String relyingParty;

    public StepXblSisuAuthentication(final AbstractStep<?, StepInitialXblSession.InitialXblSession> prevStep, final String relyingParty) {
        super(prevStep);

        this.relyingParty = relyingParty;
    }

    @Override
    public StepXblSisuAuthentication.XblSisuTokens applyStep(final HttpClient httpClient, final StepInitialXblSession.InitialXblSession initialXblSession) throws Exception {
        MinecraftAuth.LOGGER.info("Authenticating with Xbox Live using SISU...");

        if (initialXblSession.getXblDeviceToken() == null) throw new IllegalStateException("A XBL Device Token is needed for SISU authentication");

        final JsonObject postData = new JsonObject();
        postData.addProperty("AccessToken", "t=" + initialXblSession.getMsaToken().getAccessToken());
        postData.addProperty("DeviceToken", initialXblSession.getXblDeviceToken().getToken());
        postData.addProperty("AppId", initialXblSession.getMsaToken().getMsaCode().getClientId());
        postData.add("ProofKey", CryptUtil.getProofKey(initialXblSession.getXblDeviceToken().getPublicKey()));
        postData.addProperty("SiteName", "user.auth.xboxlive.com");
        postData.addProperty("RelyingParty", this.relyingParty);
        postData.addProperty("Sandbox", "RETAIL");
        postData.addProperty("UseModernGamertag", true);

        final HttpPost httpPost = new HttpPost(XBL_SISU_URL);
        httpPost.setEntity(new StringEntity(postData.toString(), ContentType.APPLICATION_JSON));
        httpPost.addHeader(CryptUtil.getSignatureHeader(httpPost, initialXblSession.getXblDeviceToken().getPrivateKey()));
        final String response = httpClient.execute(httpPost, new XblResponseHandler());
        final JsonObject obj = JsonParser.parseString(response).getAsJsonObject();

        final XblSisuTokens result = new XblSisuTokens(
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

        MinecraftAuth.LOGGER.info("Got XBL Title+User+XSTS Token, expires: " + Instant.ofEpochMilli(result.getExpireTimeMs()).atZone(ZoneId.systemDefault()));
        return result;
    }

    @Override
    public StepXblXstsToken.XblXsts<?> refresh(final HttpClient httpClient, final StepXblXstsToken.XblXsts<?> result) throws Exception {
        if (result.isExpired()) return super.refresh(httpClient, result);

        return result;
    }

    @Override
    public StepXblSisuAuthentication.XblSisuTokens fromJson(final JsonObject json) {
        final StepInitialXblSession.InitialXblSession initialXblSession = this.prevStep != null ? this.prevStep.fromJson(json.getAsJsonObject("initialXblSession")) : null;
        return new StepXblSisuAuthentication.XblSisuTokens(
                XblSisuTokens.SisuTitleToken.fromJson(json.getAsJsonObject("titleToken")),
                XblSisuTokens.SisuUserToken.fromJson(json.getAsJsonObject("userToken")),
                XblSisuTokens.SisuXstsToken.fromJson(json.getAsJsonObject("xstsToken")),
                initialXblSession
        );
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
        public JsonObject toJson() {
            final JsonObject json = new JsonObject();
            json.add("titleToken", this.titleToken.toJson());
            json.add("userToken", this.userToken.toJson());
            json.add("xstsToken", this.xstsToken.toJson());
            if (this.initialXblSession != null) json.add("initialXblSession", this.initialXblSession.toJson());
            return json;
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

            public JsonObject toJson() {
                final JsonObject json = new JsonObject();
                json.addProperty("expireTimeMs", this.expireTimeMs);
                json.addProperty("token", this.token);
                json.addProperty("titleId", this.titleId);
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

            public JsonObject toJson() {
                final JsonObject json = new JsonObject();
                json.addProperty("expireTimeMs", this.expireTimeMs);
                json.addProperty("token", this.token);
                json.addProperty("userHash", this.userHash);
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

            public JsonObject toJson() {
                final JsonObject json = new JsonObject();
                json.addProperty("expireTimeMs", this.expireTimeMs);
                json.addProperty("token", this.token);
                json.addProperty("userHash", this.userHash);
                return json;
            }

        }

    }

}
