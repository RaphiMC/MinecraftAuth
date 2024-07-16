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
import net.raphimc.minecraftauth.responsehandler.XblResponseHandler;
import net.raphimc.minecraftauth.step.AbstractStep;
import net.raphimc.minecraftauth.step.xbl.session.StepFullXblSession;
import net.raphimc.minecraftauth.step.xbl.session.StepInitialXblSession;
import net.raphimc.minecraftauth.util.CryptUtil;
import net.raphimc.minecraftauth.util.JsonContent;
import net.raphimc.minecraftauth.util.logging.ILogger;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;

public class StepXblSisuAuthentication extends AbstractStep<StepInitialXblSession.InitialXblSession, StepXblSisuAuthentication.XblSisuTokens> {

    public static final String XBL_SISU_URL = "https://sisu.xboxlive.com/authorize";

    private final String relyingParty;

    public StepXblSisuAuthentication(final AbstractStep<?, StepInitialXblSession.InitialXblSession> prevStep, final String relyingParty) {
        super("xblSisuAuthentication", prevStep);

        this.relyingParty = relyingParty;
    }

    @Override
    protected StepXblSisuAuthentication.XblSisuTokens execute(final ILogger logger, final HttpClient httpClient, final StepInitialXblSession.InitialXblSession initialXblSession) throws Exception {
        logger.info(this, "Authenticating with Xbox Live using SISU...");

        if (initialXblSession.getXblDeviceToken() == null) {
            throw new IllegalStateException("An XBL Device Token is needed for SISU authentication");
        }
        if (!this.applicationDetails.isTitleClientId()) {
            throw new IllegalStateException("A Title Client ID is needed for SISU authentication");
        }

        final JsonObject postData = new JsonObject();
        postData.addProperty("AccessToken", "t=" + initialXblSession.getMsaToken().getAccessToken());
        postData.addProperty("DeviceToken", initialXblSession.getXblDeviceToken().getToken());
        postData.addProperty("AppId", this.applicationDetails.getClientId());
        postData.add("ProofKey", CryptUtil.getProofKey(initialXblSession.getXblDeviceToken().getPublicKey()));
        postData.addProperty("SiteName", "user.auth.xboxlive.com");
        postData.addProperty("RelyingParty", this.relyingParty);
        postData.addProperty("Sandbox", "RETAIL");
        postData.addProperty("UseModernGamertag", true);

        final PostRequest postRequest = new PostRequest(XBL_SISU_URL);
        postRequest.setContent(new JsonContent(postData));
        postRequest.setHeader(CryptUtil.getSignatureHeader(postRequest, initialXblSession.getXblDeviceToken().getPrivateKey()));
        final JsonObject obj = httpClient.execute(postRequest, new XblResponseHandler());

        final StepXblTitleToken.XblTitleToken xblTitleToken = StepXblTitleToken.XblTitleToken.fromMicrosoftJson(obj.getAsJsonObject("TitleToken"), initialXblSession);
        final StepXblUserToken.XblUserToken xblUserToken = StepXblUserToken.XblUserToken.fromMicrosoftJson(obj.getAsJsonObject("UserToken"), initialXblSession);
        final StepFullXblSession.FullXblSession fullXblSession = new StepFullXblSession.FullXblSession(xblUserToken, xblTitleToken);
        final StepXblXstsToken.XblXstsToken xblXstsToken = StepXblXstsToken.XblXstsToken.fromMicrosoftJson(obj.getAsJsonObject("AuthorizationToken"), fullXblSession);
        final XblSisuTokens xblSisuTokens = new XblSisuTokens(
                xblUserToken,
                xblTitleToken,
                xblXstsToken,
                initialXblSession
        );
        logger.info(this, "Got XBL User+Title+XSTS Token, expires: " + Instant.ofEpochMilli(xblSisuTokens.getExpireTimeMs()).atZone(ZoneId.systemDefault()));
        return xblSisuTokens;
    }

    @Override
    public StepXblSisuAuthentication.XblSisuTokens fromJson(final JsonObject json) {
        final StepInitialXblSession.InitialXblSession initialXblSession = this.prevStep != null ? this.prevStep.fromJson(json.getAsJsonObject(this.prevStep.name)) : null;
        final StepXblUserToken.XblUserToken xblUserToken = StepXblUserToken.XblUserToken.fromJson(json.getAsJsonObject("userToken"), initialXblSession);
        final StepXblTitleToken.XblTitleToken xblTitleToken = StepXblTitleToken.XblTitleToken.fromJson(json.getAsJsonObject("titleToken"), initialXblSession);
        final StepFullXblSession.FullXblSession fullXblSession = new StepFullXblSession.FullXblSession(xblUserToken, xblTitleToken);
        final StepXblXstsToken.XblXstsToken xblXstsToken = StepXblXstsToken.XblXstsToken.fromJson(json.getAsJsonObject("xstsToken"), fullXblSession);
        return new StepXblSisuAuthentication.XblSisuTokens(
                xblUserToken,
                xblTitleToken,
                xblXstsToken,
                initialXblSession
        );
    }

    @Override
    public JsonObject toJson(final StepXblSisuAuthentication.XblSisuTokens xblSisuTokens) {
        final JsonObject json = new JsonObject();
        json.add("userToken", StepXblUserToken.XblUserToken.toJson(xblSisuTokens.userToken));
        json.add("titleToken", StepXblTitleToken.XblTitleToken.toJson(xblSisuTokens.titleToken));
        json.add("xstsToken", StepXblXstsToken.XblXstsToken.toJson(xblSisuTokens.xstsToken));
        if (this.prevStep != null) json.add(this.prevStep.name, this.prevStep.toJson(xblSisuTokens.initialXblSession));
        return json;
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class XblSisuTokens extends StepXblXstsToken.XblXsts<StepInitialXblSession.InitialXblSession> {

        StepXblUserToken.XblUserToken userToken;
        StepXblTitleToken.XblTitleToken titleToken;
        StepXblXstsToken.XblXstsToken xstsToken;
        StepInitialXblSession.InitialXblSession initialXblSession;

        @Override
        public long getExpireTimeMs() {
            return Math.min(Math.min(this.userToken.getExpireTimeMs(), this.titleToken.getExpireTimeMs()), this.xstsToken.getExpireTimeMs());
        }

        @Override
        public String getToken() {
            return this.xstsToken.getToken();
        }

        @Override
        public String getUserHash() {
            return this.xstsToken.getUserHash();
        }

        @Override
        public Map<String, String> getDisplayClaims() {
            return this.xstsToken.getDisplayClaims();
        }

        @Override
        public StepFullXblSession.FullXblSession getFullXblSession() {
            return this.xstsToken.getFullXblSession();
        }

        @Override
        protected StepInitialXblSession.InitialXblSession prevResult() {
            return this.initialXblSession;
        }

        @Override
        public boolean isExpired() {
            return this.userToken.isExpired() || this.titleToken.isExpired() || this.xstsToken.isExpired();
        }

        @Override
        public boolean isExpiredOrOutdated() {
            return this.userToken.isExpiredOrOutdated() || this.titleToken.isExpiredOrOutdated() || this.xstsToken.isExpiredOrOutdated();
        }

    }

}
