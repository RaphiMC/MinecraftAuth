/*
 * This file is part of MinecraftAuth - https://github.com/RaphiMC/MinecraftAuth
 * Copyright (C) 2023 RK_01/RaphiMC and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
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
import net.raphimc.mcauth.step.xbl.session.StepInitialXblSession;
import net.raphimc.mcauth.util.CryptUtil;
import net.raphimc.mcauth.util.XblResponseHandler;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;

public class StepXblUserToken extends AbstractStep<StepInitialXblSession.InitialXblSession, StepXblUserToken.XblUserToken> {

    public static final String XBL_USER_URL = "https://user.auth.xboxlive.com/user/authenticate";

    public StepXblUserToken(AbstractStep<?, StepInitialXblSession.InitialXblSession> prevStep) {
        super(prevStep);
    }

    @Override
    public XblUserToken applyStep(HttpClient httpClient, StepInitialXblSession.InitialXblSession prevResult) throws Exception {
        MinecraftAuth.LOGGER.info("Authenticating user with Xbox Live...");

        final JsonObject postData = new JsonObject();
        final JsonObject properties = new JsonObject();
        properties.addProperty("AuthMethod", "RPS");
        properties.addProperty("SiteName", "user.auth.xboxlive.com");
        properties.addProperty("RpsTicket", (prevResult.prevResult().isTitleClientId() ? "t=" : "d=") + prevResult.prevResult().access_token());
        if (prevResult.prevResult2() != null) {
            properties.add("ProofKey", CryptUtil.getProofKey(prevResult.prevResult2().publicKey()));
        }
        postData.add("Properties", properties);
        postData.addProperty("RelyingParty", "http://auth.xboxlive.com");
        postData.addProperty("TokenType", "JWT");

        final HttpPost httpPost = new HttpPost(XBL_USER_URL);
        httpPost.setEntity(new StringEntity(postData.toString(), ContentType.APPLICATION_JSON));
        httpPost.addHeader("x-xbl-contract-version", "1");
        if (prevResult.prevResult2() != null) {
            httpPost.addHeader(CryptUtil.getSignatureHeader(httpPost, prevResult.prevResult2().privateKey()));
        }
        final String response = httpClient.execute(httpPost, new XblResponseHandler());
        final JsonObject obj = JsonParser.parseString(response).getAsJsonObject();

        final XblUserToken result = new XblUserToken(
                Instant.parse(obj.get("NotAfter").getAsString()).toEpochMilli(),
                obj.get("Token").getAsString(),
                obj.getAsJsonObject("DisplayClaims").getAsJsonArray("xui").get(0).getAsJsonObject().get("uhs").getAsString(),
                prevResult
        );
        MinecraftAuth.LOGGER.info("Got XBL User Token, expires: " + Instant.ofEpochMilli(result.expireTimeMs).atZone(ZoneId.systemDefault()));
        return result;
    }

    @Override
    public XblUserToken refresh(HttpClient httpClient, XblUserToken result) throws Exception {
        if (result == null || result.isExpired()) return super.refresh(httpClient, result);

        return result;
    }

    @Override
    public XblUserToken fromJson(JsonObject json) throws Exception {
        final StepInitialXblSession.InitialXblSession prev = this.prevStep != null ? this.prevStep.fromJson(json.getAsJsonObject("prev")) : null;
        return new XblUserToken(
                json.get("expireTimeMs").getAsLong(),
                json.get("token").getAsString(),
                json.get("userHash").getAsString(),
                prev
        );
    }

    public static final class XblUserToken implements AbstractStep.StepResult<StepInitialXblSession.InitialXblSession> {

        private final long expireTimeMs;
        private final String token;
        private final String userHash;
        private final StepInitialXblSession.InitialXblSession prevResult;

        public XblUserToken(long expireTimeMs, String token, String userHash, StepInitialXblSession.InitialXblSession prevResult) {
            this.expireTimeMs = expireTimeMs;
            this.token = token;
            this.userHash = userHash;
            this.prevResult = prevResult;
        }

        @Override
        public JsonObject toJson() {
            final JsonObject json = new JsonObject();
            json.addProperty("expireTimeMs", this.expireTimeMs);
            json.addProperty("token", this.token);
            json.addProperty("userHash", this.userHash);
            if (this.prevResult != null) json.add("prev", this.prevResult.toJson());
            return json;
        }

        @Override
        public boolean isExpired() {
            return this.expireTimeMs <= System.currentTimeMillis();
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
        public StepInitialXblSession.InitialXblSession prevResult() {
            return prevResult;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            XblUserToken that = (XblUserToken) obj;
            return this.expireTimeMs == that.expireTimeMs &&
                    Objects.equals(this.token, that.token) &&
                    Objects.equals(this.userHash, that.userHash) &&
                    Objects.equals(this.prevResult, that.prevResult);
        }

        @Override
        public int hashCode() {
            return Objects.hash(expireTimeMs, token, userHash, prevResult);
        }

        @Override
        public String toString() {
            return "XblUserToken[" +
                    "expireTimeMs=" + expireTimeMs + ", " +
                    "token=" + token + ", " +
                    "userHash=" + userHash + ", " +
                    "prevResult=" + prevResult + ']';
        }

    }

}
