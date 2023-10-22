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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
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

public class StepXblXstsToken extends AbstractStep<StepFullXblSession.FullXblSession, StepXblXstsToken.XblXsts<?>> {

    public static final String XBL_XSTS_URL = "https://xsts.auth.xboxlive.com/xsts/authorize";

    private final String relyingParty;

    public StepXblXstsToken(final AbstractStep<?, StepFullXblSession.FullXblSession> prevStep, final String relyingParty) {
        super(prevStep);

        this.relyingParty = relyingParty;
    }

    @Override
    public XblXstsToken applyStep(final HttpClient httpClient, final StepFullXblSession.FullXblSession fullXblSession) throws Exception {
        MinecraftAuth.LOGGER.info("Requesting XSTS Token...");

        final JsonObject postData = new JsonObject();
        final JsonObject properties = new JsonObject();
        properties.addProperty("SandboxId", "RETAIL");
        final JsonArray userTokens = new JsonArray();
        userTokens.add(new JsonPrimitive(fullXblSession.getXblUserToken().getToken()));
        properties.add("UserTokens", userTokens);
        if (fullXblSession.getXblTitleToken() != null) {
            properties.addProperty("TitleToken", fullXblSession.getXblTitleToken().getToken());
            properties.addProperty("DeviceToken", fullXblSession.getXblTitleToken().getInitialXblSession().getXblDeviceToken().getToken());
        }
        postData.add("Properties", properties);
        postData.addProperty("RelyingParty", this.relyingParty);
        postData.addProperty("TokenType", "JWT");

        final HttpPost httpPost = new HttpPost(XBL_XSTS_URL);
        httpPost.setEntity(new StringEntity(postData.toString(), ContentType.APPLICATION_JSON));
        httpPost.addHeader("x-xbl-contract-version", "1");
        if (fullXblSession.getXblTitleToken() != null) {
            httpPost.addHeader(CryptUtil.getSignatureHeader(httpPost, fullXblSession.getXblTitleToken().getInitialXblSession().getXblDeviceToken().getPrivateKey()));
        }
        final String response = httpClient.execute(httpPost, new XblResponseHandler());
        final JsonObject obj = JsonParser.parseString(response).getAsJsonObject();

        final XblXstsToken result = new XblXstsToken(
                Instant.parse(obj.get("NotAfter").getAsString()).toEpochMilli(),
                obj.get("Token").getAsString(),
                obj.getAsJsonObject("DisplayClaims").getAsJsonArray("xui").get(0).getAsJsonObject().get("uhs").getAsString(),
                fullXblSession
        );
        MinecraftAuth.LOGGER.info("Got XSTS Token, expires: " + Instant.ofEpochMilli(result.getExpireTimeMs()).atZone(ZoneId.systemDefault()));
        return result;
    }

    @Override
    public StepXblXstsToken.XblXsts<?> refresh(final HttpClient httpClient, final StepXblXstsToken.XblXsts<?> result) throws Exception {
        if (result == null || result.isExpired()) return super.refresh(httpClient, result);

        return result;
    }

    @Override
    public XblXstsToken fromJson(final JsonObject json) throws Exception {
        final StepFullXblSession.FullXblSession fullXblSession = this.prevStep != null ? this.prevStep.fromJson(json.getAsJsonObject("fullXblSession")) : null;
        return new XblXstsToken(
                json.get("expireTimeMs").getAsLong(),
                json.get("token").getAsString(),
                json.get("userHash").getAsString(),
                fullXblSession
        );
    }

    @Value
    public static class XblXstsToken implements AbstractStep.StepResult<StepFullXblSession.FullXblSession>, XblXsts<StepFullXblSession.FullXblSession> {

        long expireTimeMs;
        String token;
        String userHash;
        StepFullXblSession.FullXblSession fullXblSession;

        @Override
        public StepFullXblSession.FullXblSession prevResult() {
            return this.fullXblSession;
        }

        @Override
        public JsonObject toJson() {
            final JsonObject json = new JsonObject();
            json.addProperty("expireTimeMs", this.expireTimeMs);
            json.addProperty("token", this.token);
            json.addProperty("userHash", this.userHash);
            if (this.fullXblSession != null) json.add("fullXblSession", this.fullXblSession.toJson());
            return json;
        }

        @Override
        public boolean isExpired() {
            return this.expireTimeMs <= System.currentTimeMillis();
        }

    }

    public interface XblXsts<P extends AbstractStep.StepResult<?>> extends AbstractStep.StepResult<P> {

        long getExpireTimeMs();

        String getToken();

        String getUserHash();

        StepFullXblSession.FullXblSession getFullXblSession();

        default StepInitialXblSession.InitialXblSession getInitialXblSession() {
            return this.getFullXblSession().getXblUserToken().getInitialXblSession();
        }

        @Override
        JsonObject toJson();

    }

}
