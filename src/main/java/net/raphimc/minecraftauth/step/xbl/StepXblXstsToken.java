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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import lombok.EqualsAndHashCode;
import lombok.Value;
import net.lenni0451.commons.httpclient.HttpClient;
import net.lenni0451.commons.httpclient.requests.impl.PostRequest;
import net.raphimc.minecraftauth.responsehandler.XblResponseHandler;
import net.raphimc.minecraftauth.step.AbstractStep;
import net.raphimc.minecraftauth.step.xbl.session.StepFullXblSession;
import net.raphimc.minecraftauth.step.xbl.session.StepInitialXblSession;
import net.raphimc.minecraftauth.util.CryptUtil;
import net.raphimc.minecraftauth.util.JsonContent;
import net.raphimc.minecraftauth.util.JsonUtil;
import net.raphimc.minecraftauth.util.logging.ILogger;
import org.jetbrains.annotations.ApiStatus;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

public class StepXblXstsToken extends AbstractStep<StepFullXblSession.FullXblSession, StepXblXstsToken.XblXstsToken> {

    public static final String XBL_XSTS_URL = "https://xsts.auth.xboxlive.com/xsts/authorize";

    private final String relyingParty;

    public StepXblXstsToken(final AbstractStep<?, StepFullXblSession.FullXblSession> prevStep, final String relyingParty) {
        this("xblXstsToken", prevStep, relyingParty);
    }

    public StepXblXstsToken(final String name, final AbstractStep<?, StepFullXblSession.FullXblSession> prevStep, final String relyingParty) {
        super(name, prevStep);

        this.relyingParty = relyingParty;
    }

    @Override
    public XblXstsToken applyStep(final ILogger logger, final HttpClient httpClient, final StepFullXblSession.FullXblSession fullXblSession) throws Exception {
        logger.info("Requesting XSTS Token...");

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

        final PostRequest postRequest = new PostRequest(XBL_XSTS_URL);
        postRequest.setContent(new JsonContent(postData));
        postRequest.setHeader("x-xbl-contract-version", "1");
        if (fullXblSession.getXblTitleToken() != null) {
            postRequest.setHeader(CryptUtil.getSignatureHeader(postRequest, fullXblSession.getXblTitleToken().getInitialXblSession().getXblDeviceToken().getPrivateKey()));
        }
        final JsonObject obj = httpClient.execute(postRequest, new XblResponseHandler());

        final XblXstsToken xblXstsToken = XblXstsToken.fromMicrosoftJson(obj, fullXblSession);
        logger.info("Got XSTS Token, expires: " + Instant.ofEpochMilli(xblXstsToken.getExpireTimeMs()).atZone(ZoneId.systemDefault()));
        return xblXstsToken;
    }

    @Override
    public XblXstsToken fromJson(final JsonObject json) {
        final StepFullXblSession.FullXblSession fullXblSession = this.prevStep != null ? this.prevStep.fromJson(json.getAsJsonObject(this.prevStep.name)) : null;
        return XblXstsToken.fromJson(json, fullXblSession);
    }

    @Override
    public JsonObject toJson(final StepXblXstsToken.XblXstsToken xblXstsToken) {
        final JsonObject json = XblXstsToken.toJson(xblXstsToken);
        if (this.prevStep != null) json.add(this.prevStep.name, this.prevStep.toJson(xblXstsToken.fullXblSession));
        return json;
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class XblXstsToken extends XblXsts<StepFullXblSession.FullXblSession> {

        long expireTimeMs;
        String token;
        String userHash;
        Map<String, String> displayClaims;
        StepFullXblSession.FullXblSession fullXblSession;

        @ApiStatus.Internal
        public static XblXstsToken fromMicrosoftJson(final JsonObject obj, final StepFullXblSession.FullXblSession fullXblSession) {
            final JsonObject displayClaims = obj.getAsJsonObject("DisplayClaims").getAsJsonArray("xui").get(0).getAsJsonObject();
            final Map<String, String> displayClaimsMap = displayClaims.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getAsString()));
            return new XblXstsToken(
                    Instant.parse(obj.get("NotAfter").getAsString()).toEpochMilli(),
                    obj.get("Token").getAsString(),
                    displayClaims.get("uhs").getAsString(),
                    Collections.unmodifiableMap(displayClaimsMap),
                    fullXblSession
            );
        }

        @ApiStatus.Internal
        public static XblXstsToken fromJson(final JsonObject json, final StepFullXblSession.FullXblSession fullXblSession) {
            final JsonObject displayClaims = JsonUtil.getObjectNonNull(json, "displayClaims");
            return new XblXstsToken(
                    json.get("expireTimeMs").getAsLong(),
                    json.get("token").getAsString(),
                    json.get("userHash").getAsString(),
                    Collections.unmodifiableMap(displayClaims.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getAsString()))),
                    fullXblSession
            );
        }

        @ApiStatus.Internal
        public static JsonObject toJson(final XblXstsToken xblXstsToken) {
            final JsonObject json = new JsonObject();
            json.addProperty("expireTimeMs", xblXstsToken.expireTimeMs);
            json.addProperty("token", xblXstsToken.token);
            json.addProperty("userHash", xblXstsToken.userHash);
            json.add("displayClaims", JsonUtil.GSON.toJsonTree(xblXstsToken.displayClaims));
            return json;
        }

        @Override
        protected StepFullXblSession.FullXblSession prevResult() {
            return this.fullXblSession;
        }

        @Override
        public boolean isExpired() {
            return this.expireTimeMs <= System.currentTimeMillis();
        }

    }

    public abstract static class XblXsts<P extends AbstractStep.StepResult<?>> extends AbstractStep.StepResult<P> {

        public abstract long getExpireTimeMs();

        public abstract String getToken();

        public abstract String getUserHash();

        public abstract Map<String, String> getDisplayClaims();

        public String getServiceToken() {
            return this.getUserHash() + ';' + this.getToken();
        }

        public abstract StepFullXblSession.FullXblSession getFullXblSession();

        public StepInitialXblSession.InitialXblSession getInitialXblSession() {
            return this.getFullXblSession().getXblUserToken().getInitialXblSession();
        }

    }

}
