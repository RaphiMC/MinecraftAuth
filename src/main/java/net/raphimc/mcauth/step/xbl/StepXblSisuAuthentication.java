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
package net.raphimc.mcauth.step.xbl;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.raphimc.mcauth.MinecraftAuth;
import net.raphimc.mcauth.step.AbstractStep;
import net.raphimc.mcauth.step.xbl.session.StepFullXblSession;
import net.raphimc.mcauth.step.xbl.session.StepInitialXblSession;
import net.raphimc.mcauth.util.CryptUtil;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;

public class StepXblSisuAuthentication extends AbstractStep<StepInitialXblSession.InitialXblSession, StepXblXstsToken.XblXsts<?>> {

    public static final String XBL_SISU_URL = "https://sisu.xboxlive.com/authorize";

    private final String relyingParty;

    public StepXblSisuAuthentication(AbstractStep<?, StepInitialXblSession.InitialXblSession> prevStep, final String relyingParty) {
        super(prevStep);

        this.relyingParty = relyingParty;
    }

    @Override
    public StepXblSisuAuthentication.XblSisuTokens applyStep(HttpClient httpClient, StepInitialXblSession.InitialXblSession prevResult) throws Exception {
        MinecraftAuth.LOGGER.info("Authenticating with Xbox Live using SISU...");

        if (prevResult.prevResult2() == null) throw new IllegalStateException("A XBL Device Token is needed for SISU authentication");

        final JsonObject postData = new JsonObject();
        postData.addProperty("AccessToken", "t=" + prevResult.prevResult().access_token());
        postData.addProperty("DeviceToken", prevResult.prevResult2().token());
        postData.addProperty("AppId", prevResult.prevResult().prevResult().clientId());
        postData.add("ProofKey", CryptUtil.getProofKey(prevResult.prevResult2().publicKey()));
        postData.addProperty("SiteName", "user.auth.xboxlive.com");
        postData.addProperty("RelyingParty", this.relyingParty);
        postData.addProperty("Sandbox", "RETAIL");
        postData.addProperty("UseModernGamertag", true);

        final HttpPost httpPost = new HttpPost(XBL_SISU_URL);
        httpPost.setEntity(new StringEntity(postData.toString(), ContentType.APPLICATION_JSON));
        httpPost.addHeader(CryptUtil.getSignatureHeader(httpPost, prevResult.prevResult2().privateKey()));
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
                prevResult
        );

        MinecraftAuth.LOGGER.info("Got XBL Title+User+XSTS Token, expires: " + Instant.ofEpochMilli(result.expireTimeMs()).atZone(ZoneId.systemDefault()));
        return result;
    }

    @Override
    public StepXblXstsToken.XblXsts<?> refresh(HttpClient httpClient, StepXblXstsToken.XblXsts<?> result) throws Exception {
        if (result == null || result.isExpired()) return super.refresh(httpClient, result);

        return result;
    }

    @Override
    public StepXblSisuAuthentication.XblSisuTokens fromJson(JsonObject json) throws Exception {
        final StepInitialXblSession.InitialXblSession prev = this.prevStep != null ? this.prevStep.fromJson(json.getAsJsonObject("prev")) : null;
        return new StepXblSisuAuthentication.XblSisuTokens(
                XblSisuTokens.SisuTitleToken.fromJson(json.getAsJsonObject("titleToken")),
                XblSisuTokens.SisuUserToken.fromJson(json.getAsJsonObject("userToken")),
                XblSisuTokens.SisuXstsToken.fromJson(json.getAsJsonObject("xstsToken")),
                prev
        );
    }

    public static final class XblSisuTokens implements AbstractStep.StepResult<StepInitialXblSession.InitialXblSession>, StepXblXstsToken.XblXsts<StepInitialXblSession.InitialXblSession> {

        private final SisuTitleToken titleToken;
        private final SisuUserToken userToken;
        private final SisuXstsToken xstsToken;
        private final StepInitialXblSession.InitialXblSession prevResult;

        public XblSisuTokens(SisuTitleToken titleToken, SisuUserToken userToken, SisuXstsToken xstsToken, StepInitialXblSession.InitialXblSession prevResult) {
            this.titleToken = titleToken;
            this.userToken = userToken;
            this.xstsToken = xstsToken;
            this.prevResult = prevResult;
        }

        @Override
        public long expireTimeMs() {
            return Math.min(Math.min(this.xstsToken.expireTimeMs, this.titleToken.expireTimeMs), this.userToken.expireTimeMs);
        }

        @Override
        public String token() {
            return this.xstsToken.token;
        }

        @Override
        public String userHash() {
            return this.xstsToken.userHash;
        }

        @Override
        public JsonObject toJson() {
            final JsonObject json = new JsonObject();
            json.add("titleToken", titleToken.toJson());
            json.add("userToken", userToken.toJson());
            json.add("xstsToken", xstsToken.toJson());
            if (this.prevResult != null) json.add("prev", this.prevResult.toJson());
            return json;
        }

        @Override
        public boolean isExpired() {
            return this.expireTimeMs() <= System.currentTimeMillis();
        }

        @Override
        public StepFullXblSession.FullXblSession fullXblSession() {
            final StepXblUserToken.XblUserToken userToken = new StepXblUserToken.XblUserToken(this.userToken.expireTimeMs, this.userToken.token, this.userToken.userHash, this.prevResult);
            final StepXblTitleToken.XblTitleToken titleToken = new StepXblTitleToken.XblTitleToken(this.titleToken.expireTimeMs, this.titleToken.token, this.titleToken.titleId, this.prevResult);
            return new StepFullXblSession.FullXblSession(userToken, titleToken);
        }

        public SisuTitleToken titleToken() {
            return titleToken;
        }

        public SisuUserToken userToken() {
            return userToken;
        }

        public SisuXstsToken xstsToken() {
            return xstsToken;
        }

        @Override
        public StepInitialXblSession.InitialXblSession prevResult() {
            return prevResult;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            XblSisuTokens that = (XblSisuTokens) obj;
            return Objects.equals(this.titleToken, that.titleToken) &&
                    Objects.equals(this.userToken, that.userToken) &&
                    Objects.equals(this.xstsToken, that.xstsToken) &&
                    Objects.equals(this.prevResult, that.prevResult);
        }

        @Override
        public int hashCode() {
            return Objects.hash(titleToken, userToken, xstsToken, prevResult);
        }

        @Override
        public String toString() {
            return "XblSisuTokens[" +
                    "titleToken=" + titleToken + ", " +
                    "userToken=" + userToken + ", " +
                    "xstsToken=" + xstsToken + ", " +
                    "prevResult=" + prevResult + ']';
        }


        public static final class SisuTitleToken {

            private final long expireTimeMs;
            private final String token;
            private final String titleId;

            public SisuTitleToken(long expireTimeMs, String token, String titleId) {
                this.expireTimeMs = expireTimeMs;
                this.token = token;
                this.titleId = titleId;
            }

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

            public long expireTimeMs() {
                return expireTimeMs;
            }

            public String token() {
                return token;
            }

            public String titleId() {
                return titleId;
            }

            @Override
            public boolean equals(Object obj) {
                if (obj == this) return true;
                if (obj == null || obj.getClass() != this.getClass()) return false;
                SisuTitleToken that = (SisuTitleToken) obj;
                return this.expireTimeMs == that.expireTimeMs &&
                        Objects.equals(this.token, that.token) &&
                        Objects.equals(this.titleId, that.titleId);
            }

            @Override
            public int hashCode() {
                return Objects.hash(expireTimeMs, token, titleId);
            }

            @Override
            public String toString() {
                return "SisuTitleToken[" +
                        "expireTimeMs=" + expireTimeMs + ", " +
                        "token=" + token + ", " +
                        "titleId=" + titleId + ']';
            }

        }

        public static final class SisuUserToken {

            private final long expireTimeMs;
            private final String token;
            private final String userHash;

            public SisuUserToken(long expireTimeMs, String token, String userHash) {
                this.expireTimeMs = expireTimeMs;
                this.token = token;
                this.userHash = userHash;
            }

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

            public long expireTimeMs() {
                return expireTimeMs;
            }

            public String token() {
                return token;
            }

            public String userHash() {
                return userHash;
            }

            @Override
            public boolean equals(Object obj) {
                if (obj == this) return true;
                if (obj == null || obj.getClass() != this.getClass()) return false;
                SisuUserToken that = (SisuUserToken) obj;
                return this.expireTimeMs == that.expireTimeMs &&
                        Objects.equals(this.token, that.token) &&
                        Objects.equals(this.userHash, that.userHash);
            }

            @Override
            public int hashCode() {
                return Objects.hash(expireTimeMs, token, userHash);
            }

            @Override
            public String toString() {
                return "SisuUserToken[" +
                        "expireTimeMs=" + expireTimeMs + ", " +
                        "token=" + token + ", " +
                        "userHash=" + userHash + ']';
            }

        }

        public static final class SisuXstsToken {

            private final long expireTimeMs;
            private final String token;
            private final String userHash;

            public SisuXstsToken(long expireTimeMs, String token, String userHash) {
                this.expireTimeMs = expireTimeMs;
                this.token = token;
                this.userHash = userHash;
            }

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

            public long expireTimeMs() {
                return expireTimeMs;
            }

            public String token() {
                return token;
            }

            public String userHash() {
                return userHash;
            }

            @Override
            public boolean equals(Object obj) {
                if (obj == this) return true;
                if (obj == null || obj.getClass() != this.getClass()) return false;
                SisuXstsToken that = (SisuXstsToken) obj;
                return this.expireTimeMs == that.expireTimeMs &&
                        Objects.equals(this.token, that.token) &&
                        Objects.equals(this.userHash, that.userHash);
            }

            @Override
            public int hashCode() {
                return Objects.hash(expireTimeMs, token, userHash);
            }

            @Override
            public String toString() {
                return "SisuXstsToken[" +
                        "expireTimeMs=" + expireTimeMs + ", " +
                        "token=" + token + ", " +
                        "userHash=" + userHash + ']';
            }

        }

    }

}
