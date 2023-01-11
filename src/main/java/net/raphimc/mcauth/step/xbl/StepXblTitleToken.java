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

public class StepXblTitleToken extends AbstractStep<StepInitialXblSession.InitialXblSession, StepXblTitleToken.XblTitleToken> {

    public static final String XBL_TITLE_URL = "https://title.auth.xboxlive.com/title/authenticate";

    public StepXblTitleToken(AbstractStep<?, StepInitialXblSession.InitialXblSession> prevStep) {
        super(prevStep);
    }

    @Override
    public XblTitleToken applyStep(HttpClient httpClient, StepInitialXblSession.InitialXblSession prevResult) throws Exception {
        MinecraftAuth.LOGGER.info("Authenticating title with Xbox Live...");

        if (prevResult.prevResult2() == null) throw new IllegalStateException("A XBL Device Token is needed for Title authentication");

        final JsonObject postData = new JsonObject();
        final JsonObject properties = new JsonObject();
        properties.addProperty("AuthMethod", "RPS");
        properties.addProperty("SiteName", "user.auth.xboxlive.com");
        properties.addProperty("RpsTicket", "t=" + prevResult.prevResult().access_token());
        properties.addProperty("DeviceToken", prevResult.prevResult2().token());
        properties.add("ProofKey", CryptUtil.getProofKey(prevResult.prevResult2().publicKey()));
        postData.add("Properties", properties);
        postData.addProperty("RelyingParty", "http://auth.xboxlive.com");
        postData.addProperty("TokenType", "JWT");

        final HttpPost httpPost = new HttpPost(XBL_TITLE_URL);
        httpPost.setEntity(new StringEntity(postData.toString(), ContentType.APPLICATION_JSON));
        httpPost.addHeader("x-xbl-contract-version", "1");
        httpPost.addHeader(CryptUtil.getSignatureHeader(httpPost, prevResult.prevResult2().privateKey()));
        final String response = httpClient.execute(httpPost, new XblResponseHandler());
        final JsonObject obj = JsonParser.parseString(response).getAsJsonObject();

        final XblTitleToken result = new XblTitleToken(
                Instant.parse(obj.get("NotAfter").getAsString()).toEpochMilli(),
                obj.get("Token").getAsString(),
                obj.getAsJsonObject("DisplayClaims").getAsJsonObject("xti").get("tid").getAsString(),
                prevResult
        );
        MinecraftAuth.LOGGER.info("Got XBL Title Token, expires: " + Instant.ofEpochMilli(result.expireTimeMs).atZone(ZoneId.systemDefault()));
        return result;
    }

    @Override
    public XblTitleToken refresh(HttpClient httpClient, XblTitleToken result) throws Exception {
        if (result == null || result.isExpired()) return super.refresh(httpClient, result);

        return result;
    }

    @Override
    public XblTitleToken fromJson(JsonObject json) throws Exception {
        final StepInitialXblSession.InitialXblSession prev = this.prevStep.fromJson(json.getAsJsonObject("prev"));
        return new XblTitleToken(
                json.get("expireTimeMs").getAsLong(),
                json.get("token").getAsString(),
                json.get("userHash").getAsString(),
                prev
        );
    }

    public static final class XblTitleToken implements AbstractStep.StepResult<StepInitialXblSession.InitialXblSession> {

        private final long expireTimeMs;
        private final String token;
        private final String titleId;
        private final StepInitialXblSession.InitialXblSession prevResult;

        public XblTitleToken(long expireTimeMs, String token, String titleId, StepInitialXblSession.InitialXblSession prevResult) {
            this.expireTimeMs = expireTimeMs;
            this.token = token;
            this.titleId = titleId;
            this.prevResult = prevResult;
        }

        @Override
        public JsonObject toJson() {
            final JsonObject json = new JsonObject();
            json.addProperty("expireTimeMs", this.expireTimeMs);
            json.addProperty("token", this.token);
            json.addProperty("titleId", this.titleId);
            json.add("prev", this.prevResult.toJson());
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

        public String titleId() {
            return titleId;
        }

        @Override
        public StepInitialXblSession.InitialXblSession prevResult() {
            return prevResult;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            XblTitleToken that = (XblTitleToken) obj;
            return this.expireTimeMs == that.expireTimeMs &&
                    Objects.equals(this.token, that.token) &&
                    Objects.equals(this.titleId, that.titleId) &&
                    Objects.equals(this.prevResult, that.prevResult);
        }

        @Override
        public int hashCode() {
            return Objects.hash(expireTimeMs, token, titleId, prevResult);
        }

        @Override
        public String toString() {
            return "XblTitleToken[" +
                    "expireTimeMs=" + expireTimeMs + ", " +
                    "token=" + token + ", " +
                    "titleId=" + titleId + ", " +
                    "prevResult=" + prevResult + ']';
        }

    }

}
