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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
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

public class StepXblXstsToken extends AbstractStep<StepFullXblSession.FullXblSession, StepXblXstsToken.XblXsts<?>> {

    public static final String XBL_XSTS_URL = "https://xsts.auth.xboxlive.com/xsts/authorize";

    private final String relyingParty;

    public StepXblXstsToken(AbstractStep<?, StepFullXblSession.FullXblSession> prevStep, final String relyingParty) {
        super(prevStep);

        this.relyingParty = relyingParty;
    }

    @Override
    public XblXstsToken applyStep(HttpClient httpClient, StepFullXblSession.FullXblSession prevResult) throws Exception {
        MinecraftAuth.LOGGER.info("Requesting XSTS Token...");

        final JsonObject postData = new JsonObject();
        final JsonObject properties = new JsonObject();
        properties.addProperty("SandboxId", "RETAIL");
        final JsonArray userTokens = new JsonArray();
        userTokens.add(new JsonPrimitive(prevResult.prevResult().token()));
        properties.add("UserTokens", userTokens);
        if (prevResult.prevResult2() != null) {
            properties.addProperty("TitleToken", prevResult.prevResult2().token());
            properties.addProperty("DeviceToken", prevResult.prevResult2().prevResult().prevResult2().token());
        }
        postData.add("Properties", properties);
        postData.addProperty("RelyingParty", this.relyingParty);
        postData.addProperty("TokenType", "JWT");

        final HttpPost httpPost = new HttpPost(XBL_XSTS_URL);
        httpPost.setEntity(new StringEntity(postData.toString(), ContentType.APPLICATION_JSON));
        httpPost.addHeader("x-xbl-contract-version", "1");
        if (prevResult.prevResult2() != null) {
            httpPost.addHeader(CryptUtil.getSignatureHeader(httpPost, prevResult.prevResult2().prevResult().prevResult2().privateKey()));
        }
        final String response = httpClient.execute(httpPost, new XblResponseHandler());
        final JsonObject obj = JsonParser.parseString(response).getAsJsonObject();

        final XblXstsToken result = new XblXstsToken(
                Instant.parse(obj.get("NotAfter").getAsString()).toEpochMilli(),
                obj.get("Token").getAsString(),
                obj.getAsJsonObject("DisplayClaims").getAsJsonArray("xui").get(0).getAsJsonObject().get("uhs").getAsString(),
                prevResult
        );
        MinecraftAuth.LOGGER.info("Got XSTS Token, expires: " + Instant.ofEpochMilli(result.expireTimeMs).atZone(ZoneId.systemDefault()));
        return result;
    }

    @Override
    public StepXblXstsToken.XblXsts<?> refresh(HttpClient httpClient, StepXblXstsToken.XblXsts<?> result) throws Exception {
        if (result == null || result.isExpired()) return super.refresh(httpClient, result);

        return result;
    }

    @Override
    public XblXstsToken fromJson(JsonObject json) throws Exception {
        final StepFullXblSession.FullXblSession prev = this.prevStep != null ? this.prevStep.fromJson(json.getAsJsonObject("prev")) : null;
        return new XblXstsToken(
                json.get("expireTimeMs").getAsLong(),
                json.get("token").getAsString(),
                json.get("userHash").getAsString(),
                prev
        );
    }

    public static final class XblXstsToken implements AbstractStep.StepResult<StepFullXblSession.FullXblSession>, XblXsts<StepFullXblSession.FullXblSession> {

        private final long expireTimeMs;
        private final String token;
        private final String userHash;
        private final StepFullXblSession.FullXblSession prevResult;

        public XblXstsToken(long expireTimeMs, String token, String userHash, StepFullXblSession.FullXblSession prevResult) {
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

        @Override
        public StepFullXblSession.FullXblSession fullXblSession() {
            return this.prevResult;
        }

        @Override
        public long expireTimeMs() {
            return expireTimeMs;
        }

        @Override
        public String token() {
            return token;
        }

        @Override
        public String userHash() {
            return userHash;
        }

        @Override
        public StepFullXblSession.FullXblSession prevResult() {
            return prevResult;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            XblXstsToken that = (XblXstsToken) obj;
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
            return "XblXstsToken[" +
                    "expireTimeMs=" + expireTimeMs + ", " +
                    "token=" + token + ", " +
                    "userHash=" + userHash + ", " +
                    "prevResult=" + prevResult + ']';
        }

    }

    public interface XblXsts<P extends AbstractStep.StepResult<?>> extends AbstractStep.StepResult<P> {

        long expireTimeMs();

        String token();

        String userHash();

        default StepInitialXblSession.InitialXblSession initialXblSession() {
            return this.fullXblSession().prevResult().prevResult();
        }

        StepFullXblSession.FullXblSession fullXblSession();

        @Override
        JsonObject toJson();

    }

}
