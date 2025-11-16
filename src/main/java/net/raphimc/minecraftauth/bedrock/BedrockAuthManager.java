/*
 * This file is part of MinecraftAuth - https://github.com/RaphiMC/MinecraftAuth
 * Copyright (C) 2022-2025 RK_01/RaphiMC and contributors
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
package net.raphimc.minecraftauth.bedrock;

import com.google.gson.JsonObject;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.lenni0451.commons.gson.elements.GsonObject;
import net.lenni0451.commons.httpclient.HttpClient;
import net.raphimc.minecraftauth.bedrock.model.MinecraftCertificateChain;
import net.raphimc.minecraftauth.bedrock.model.MinecraftMultiplayerToken;
import net.raphimc.minecraftauth.bedrock.model.MinecraftSession;
import net.raphimc.minecraftauth.bedrock.request.MinecraftAuthenticationRequest;
import net.raphimc.minecraftauth.bedrock.request.MinecraftMultiplayerSessionStartRequest;
import net.raphimc.minecraftauth.bedrock.request.MinecraftSessionStartRequest;
import net.raphimc.minecraftauth.msa.data.MsaConstants;
import net.raphimc.minecraftauth.msa.model.MsaApplicationConfig;
import net.raphimc.minecraftauth.msa.model.MsaToken;
import net.raphimc.minecraftauth.msa.request.MsaRefreshTokenRequest;
import net.raphimc.minecraftauth.msa.service.MsaAuthService;
import net.raphimc.minecraftauth.msa.service.util.MsaAuthServiceSupplier;
import net.raphimc.minecraftauth.msa.service.util.ParamMsaAuthServiceSupplier;
import net.raphimc.minecraftauth.playfab.data.PlayFabConstants;
import net.raphimc.minecraftauth.playfab.model.PlayFabToken;
import net.raphimc.minecraftauth.playfab.request.PlayFabLoginWithXboxRequest;
import net.raphimc.minecraftauth.util.CryptUtil;
import net.raphimc.minecraftauth.util.JsonUtil;
import net.raphimc.minecraftauth.util.holder.Holder;
import net.raphimc.minecraftauth.util.holder.listener.ChangeListeners;
import net.raphimc.minecraftauth.xbl.data.XblConstants;
import net.raphimc.minecraftauth.xbl.model.*;
import net.raphimc.minecraftauth.xbl.request.XblDeviceAuthenticateRequest;
import net.raphimc.minecraftauth.xbl.request.XblSisuAuthorizeRequest;
import net.raphimc.minecraftauth.xbl.request.XblUserAuthenticateRequest;
import net.raphimc.minecraftauth.xbl.request.XblXstsAuthorizeRequest;

import java.io.IOException;
import java.security.KeyPair;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

@Getter
public class BedrockAuthManager {

    public static BedrockAuthManager fromJson(final HttpClient httpClient, final String gameVersion, final JsonObject json) {
        return fromJson(httpClient, gameVersion, new GsonObject(json));
    }

    public static BedrockAuthManager fromJson(final HttpClient httpClient, final String gameVersion, final GsonObject json) {
        return new BedrockAuthManager(
                httpClient,
                gameVersion,
                MsaApplicationConfig.fromJson(json.reqObject("msaApplicationConfig")),
                json.reqString("deviceType"),
                JsonUtil.decodeKeyPair(json.reqObject("deviceKeyPair")),
                UUID.fromString(json.reqString("deviceId")),
                JsonUtil.decodeKeyPair(json.reqObject("sessionKeyPair")),
                MsaToken.fromJson(json.reqObject("msaToken")),
                json.optObject("xblDeviceToken").map(XblDeviceToken::fromJson).orElse(null),
                json.optObject("xblUserToken").map(XblUserToken::fromJson).orElse(null),
                json.optObject("xblTitleToken").map(XblTitleToken::fromJson).orElse(null),
                json.optObject("bedrockXstsToken").map(XblXstsToken::fromJson).orElse(null),
                json.optObject("playFabXstsToken").map(XblXstsToken::fromJson).orElse(null),
                json.optObject("realmsXstsToken").map(XblXstsToken::fromJson).orElse(null),
                json.optObject("playFabToken").map(PlayFabToken::fromJson).orElse(null),
                json.optObject("minecraftSession").map(MinecraftSession::fromJson).orElse(null),
                json.optObject("minecraftMultiplayerToken").map(MinecraftMultiplayerToken::fromJson).orElse(null),
                json.optObject("minecraftCertificateChain").map(MinecraftCertificateChain::fromJson).orElse(null)
        );
    }

    public static JsonObject toJson(final BedrockAuthManager authManager) {
        final JsonObject json = new JsonObject();
        json.addProperty("_saveVersion", 1);
        json.add("msaApplicationConfig", MsaApplicationConfig.toJson(authManager.msaApplicationConfig));
        json.addProperty("deviceType", authManager.deviceType);
        json.add("deviceKeyPair", JsonUtil.encodeKeyPair(authManager.deviceKeyPair));
        json.addProperty("deviceId", authManager.deviceId.toString());
        json.add("sessionKeyPair", JsonUtil.encodeKeyPair(authManager.sessionKeyPair));
        json.add("msaToken", MsaToken.toJson(authManager.msaToken.getCached()));
        if (authManager.xblDeviceToken.hasValue()) {
            json.add("xblDeviceToken", XblDeviceToken.toJson(authManager.xblDeviceToken.getCached()));
        }
        if (authManager.xblUserToken.hasValue()) {
            json.add("xblUserToken", XblUserToken.toJson(authManager.xblUserToken.getCached()));
        }
        if (authManager.xblTitleToken.hasValue()) {
            json.add("xblTitleToken", XblTitleToken.toJson(authManager.xblTitleToken.getCached()));
        }
        if (authManager.bedrockXstsToken.hasValue()) {
            json.add("bedrockXstsToken", XblXstsToken.toJson(authManager.bedrockXstsToken.getCached()));
        }
        if (authManager.playFabXstsToken.hasValue()) {
            json.add("playFabXstsToken", XblXstsToken.toJson(authManager.playFabXstsToken.getCached()));
        }
        if (authManager.realmsXstsToken.hasValue()) {
            json.add("realmsXstsToken", XblXstsToken.toJson(authManager.realmsXstsToken.getCached()));
        }
        if (authManager.playFabToken.hasValue()) {
            json.add("playFabToken", PlayFabToken.toJson(authManager.playFabToken.getCached()));
        }
        if (authManager.minecraftSession.hasValue()) {
            json.add("minecraftSession", MinecraftSession.toJson(authManager.minecraftSession.getCached()));
        }
        if (authManager.minecraftMultiplayerToken.hasValue()) {
            json.add("minecraftMultiplayerToken", MinecraftMultiplayerToken.toJson(authManager.minecraftMultiplayerToken.getCached()));
        }
        if (authManager.minecraftCertificateChain.hasValue()) {
            json.add("minecraftCertificateChain", MinecraftCertificateChain.toJson(authManager.minecraftCertificateChain.getCached()));
        }
        return json;
    }

    public static Builder create(final HttpClient httpClient, final String gameVersion) {
        return new Builder(httpClient, gameVersion);
    }

    private final HttpClient httpClient;
    private final String gameVersion;
    private final MsaApplicationConfig msaApplicationConfig;
    private final String deviceType;
    private final KeyPair deviceKeyPair;
    private final UUID deviceId;
    private final KeyPair sessionKeyPair;
    private final ChangeListeners changeListeners = new ChangeListeners();

    @Getter(AccessLevel.NONE)
    private final Object sisuTokenLock = new Object();

    private final Holder<MsaToken> msaToken = new Holder<>(this::refreshMsaToken);
    private final Holder<XblDeviceToken> xblDeviceToken = new Holder<>(this::refreshXblDeviceToken);
    private final Holder<XblUserToken> xblUserToken = new Holder<>(this::refreshXblUserToken, this.sisuTokenLock);
    private final Holder<XblTitleToken> xblTitleToken = new Holder<>(this::refreshXblTitleToken, this.sisuTokenLock);
    private final Holder<XblXstsToken> bedrockXstsToken = new Holder<>(this::refreshBedrockXstsToken, this.sisuTokenLock);
    private final Holder<XblXstsToken> playFabXstsToken = new Holder<>(this::refreshPlayFabXstsToken);
    private final Holder<XblXstsToken> realmsXstsToken = new Holder<>(this::refreshRealmsXstsToken);
    private final Holder<PlayFabToken> playFabToken = new Holder<>(this::refreshPlayFabToken);
    private final Holder<MinecraftSession> minecraftSession = new Holder<>(this::refreshMinecraftSession);
    private final Holder<MinecraftMultiplayerToken> minecraftMultiplayerToken = new Holder<>(this::refreshMinecraftMultiplayerToken);
    private final Holder<MinecraftCertificateChain> minecraftCertificateChain = new Holder<>(this::refreshMinecraftCertificateChain);

    private BedrockAuthManager(final HttpClient httpClient, final String gameVersion, final MsaApplicationConfig msaApplicationConfig, final String deviceType, final KeyPair deviceKeyPair, final UUID deviceId, final KeyPair sessionKeyPair, final MsaToken msaToken) {
        this.httpClient = httpClient;
        this.gameVersion = gameVersion;
        this.msaApplicationConfig = msaApplicationConfig;
        this.deviceType = deviceType;
        this.deviceKeyPair = deviceKeyPair;
        this.deviceId = deviceId;
        this.sessionKeyPair = sessionKeyPair;
        this.msaToken.set(msaToken);
        this.hookChangeListeners();
    }

    private BedrockAuthManager(final HttpClient httpClient, final String gameVersion, final MsaApplicationConfig msaApplicationConfig, final String deviceType, final KeyPair deviceKeyPair, final UUID deviceId, final KeyPair sessionKeyPair, final MsaToken msaToken, final XblDeviceToken xblDeviceToken, final XblUserToken xblUserToken, final XblTitleToken xblTitleToken, final XblXstsToken bedrockXstsToken, final XblXstsToken playFabXstsToken, final XblXstsToken realmsXstsToken, final PlayFabToken playFabToken, final MinecraftSession minecraftSession, final MinecraftMultiplayerToken minecraftMultiplayerToken, final MinecraftCertificateChain minecraftCertificateChain) {
        this.httpClient = httpClient;
        this.gameVersion = gameVersion;
        this.msaApplicationConfig = msaApplicationConfig;
        this.deviceType = deviceType;
        this.deviceKeyPair = deviceKeyPair;
        this.deviceId = deviceId;
        this.sessionKeyPair = sessionKeyPair;
        this.msaToken.set(msaToken);
        this.xblDeviceToken.set(xblDeviceToken);
        this.xblUserToken.set(xblUserToken);
        this.xblTitleToken.set(xblTitleToken);
        this.bedrockXstsToken.set(bedrockXstsToken);
        this.playFabXstsToken.set(playFabXstsToken);
        this.realmsXstsToken.set(realmsXstsToken);
        this.playFabToken.set(playFabToken);
        this.minecraftSession.set(minecraftSession);
        this.minecraftMultiplayerToken.set(minecraftMultiplayerToken);
        this.minecraftCertificateChain.set(minecraftCertificateChain);
        this.hookChangeListeners();
    }

    private MsaToken refreshMsaToken() throws IOException {
        if (this.msaToken.getCached().getRefreshToken() == null) {
            throw new IllegalStateException("Can't refresh MSA token, because it was created without a refresh token. The user has to sign in again.");
        }
        return this.httpClient.executeAndHandle(new MsaRefreshTokenRequest(this.msaApplicationConfig, this.msaToken.getCached()));
    }

    private XblDeviceToken refreshXblDeviceToken() throws IOException {
        return this.httpClient.executeAndHandle(new XblDeviceAuthenticateRequest(this.deviceType, this.deviceId, this.deviceKeyPair));
    }

    private XblUserToken refreshXblUserToken() throws IOException {
        if (this.msaApplicationConfig.isTitleClientId()) {
            this.refreshSisuTokens();
            return this.xblUserToken.getCached();
        } else {
            return this.httpClient.executeAndHandle(new XblUserAuthenticateRequest(this.msaApplicationConfig, this.msaToken.getUpToDate()));
        }
    }

    private XblTitleToken refreshXblTitleToken() throws IOException {
        if (!this.msaApplicationConfig.isTitleClientId()) {
            throw new UnsupportedOperationException("Can't refresh XBL title token, because the MSA application client ID is not a title client ID");
        }
        this.refreshSisuTokens();
        return this.xblTitleToken.getCached();
    }

    private XblXstsToken refreshBedrockXstsToken() throws IOException {
        if (this.msaApplicationConfig.isTitleClientId()) {
            this.refreshSisuTokens();
            return this.bedrockXstsToken.getCached();
        } else {
            return this.httpClient.executeAndHandle(new XblXstsAuthorizeRequest(this.xblDeviceToken.getUpToDate(), this.xblUserToken.getUpToDate(), null, XblConstants.BEDROCK_XSTS_RELYING_PARTY));
        }
    }

    private XblXstsToken refreshPlayFabXstsToken() throws IOException {
        final XblTitleToken titleToken = this.msaApplicationConfig.isTitleClientId() ? this.xblTitleToken.getUpToDate() : null;
        return this.httpClient.executeAndHandle(new XblXstsAuthorizeRequest(this.xblDeviceToken.getUpToDate(), this.xblUserToken.getUpToDate(), titleToken, XblConstants.BEDROCK_PLAY_FAB_XSTS_RELYING_PARTY));
    }

    private XblXstsToken refreshRealmsXstsToken() throws IOException {
        final XblTitleToken titleToken = this.msaApplicationConfig.isTitleClientId() ? this.xblTitleToken.getUpToDate() : null;
        return this.httpClient.executeAndHandle(new XblXstsAuthorizeRequest(this.xblDeviceToken.getUpToDate(), this.xblUserToken.getUpToDate(), titleToken, XblConstants.BEDROCK_REALMS_XSTS_RELYING_PARTY));
    }

    private PlayFabToken refreshPlayFabToken() throws IOException {
        return this.httpClient.executeAndHandle(new PlayFabLoginWithXboxRequest(this.playFabXstsToken.getUpToDate(), PlayFabConstants.BEDROCK_PLAY_FAB_TITLE_ID));
    }

    private MinecraftSession refreshMinecraftSession() throws IOException {
        return this.httpClient.executeAndHandle(new MinecraftSessionStartRequest(this.playFabToken.getUpToDate(), this.gameVersion, this.deviceId));
    }

    private MinecraftMultiplayerToken refreshMinecraftMultiplayerToken() throws IOException {
        return this.httpClient.executeAndHandle(new MinecraftMultiplayerSessionStartRequest(this.minecraftSession.getUpToDate(), this.sessionKeyPair));
    }

    private MinecraftCertificateChain refreshMinecraftCertificateChain() throws IOException {
        return this.httpClient.executeAndHandle(new MinecraftAuthenticationRequest(this.bedrockXstsToken.getUpToDate(), this.sessionKeyPair));
    }

    private void refreshSisuTokens() throws IOException {
        final XblSisuTokens sisuTokens = this.httpClient.executeAndHandle(new XblSisuAuthorizeRequest(this.msaApplicationConfig, this.msaToken.getUpToDate(), this.xblDeviceToken.getUpToDate(), this.deviceKeyPair, XblConstants.BEDROCK_XSTS_RELYING_PARTY));
        this.xblUserToken.set(sisuTokens.getUserToken());
        this.xblTitleToken.set(sisuTokens.getTitleToken());
        this.bedrockXstsToken.set(sisuTokens.getXstsToken());
    }

    private void hookChangeListeners() {
        this.msaToken.getChangeListeners().add(this.changeListeners::invoke);
        this.xblDeviceToken.getChangeListeners().add(this.changeListeners::invoke);
        this.xblUserToken.getChangeListeners().add(this.changeListeners::invoke);
        this.xblTitleToken.getChangeListeners().add(this.changeListeners::invoke);
        this.bedrockXstsToken.getChangeListeners().add(this.changeListeners::invoke);
        this.playFabXstsToken.getChangeListeners().add(this.changeListeners::invoke);
        this.realmsXstsToken.getChangeListeners().add(this.changeListeners::invoke);
        this.playFabToken.getChangeListeners().add(this.changeListeners::invoke);
        this.minecraftSession.getChangeListeners().add(this.changeListeners::invoke);
        this.minecraftMultiplayerToken.getChangeListeners().add(this.changeListeners::invoke);
        this.minecraftCertificateChain.getChangeListeners().add(this.changeListeners::invoke);
    }

    @Setter
    @Accessors(fluent = true)
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Builder {

        private final HttpClient httpClient;
        private final String gameVersion;
        private MsaApplicationConfig msaApplicationConfig = new MsaApplicationConfig(MsaConstants.BEDROCK_ANDROID_TITLE_ID, MsaConstants.SCOPE_TITLE_AUTH);
        private String deviceType = "Android";
        private KeyPair deviceKeyPair;
        private UUID deviceId;
        private KeyPair sessionKeyPair;

        /**
         * Login with the given {@link MsaAuthServiceSupplier}.
         *
         * @param msaAuthServiceSupplier The MSA auth service supplier (For example {@link net.raphimc.minecraftauth.msa.service.impl.JfxWebViewMsaAuthService}).
         * @return A logged in {@link BedrockAuthManager}.
         */
        public BedrockAuthManager login(final MsaAuthServiceSupplier msaAuthServiceSupplier) throws IOException, InterruptedException, TimeoutException {
            final MsaAuthService msaAuthService = msaAuthServiceSupplier.get(this.httpClient, this.msaApplicationConfig);
            return this.login(msaAuthService.acquireToken());
        }

        /**
         * Login with the given {@link ParamMsaAuthServiceSupplier} and parameter.
         *
         * @param msaAuthServiceSupplier The MSA auth service supplier (For example {@link net.raphimc.minecraftauth.msa.service.impl.DeviceCodeMsaAuthService} or {@link net.raphimc.minecraftauth.msa.service.impl.CredentialsMsaAuthService}).
         * @param param                  The parameter to pass to the supplier.
         * @param <T>                    The type of the parameter.
         * @return A logged in {@link BedrockAuthManager}.
         */
        public <T> BedrockAuthManager login(final ParamMsaAuthServiceSupplier<T> msaAuthServiceSupplier, final T param) throws IOException, InterruptedException, TimeoutException {
            final MsaAuthService msaAuthService = msaAuthServiceSupplier.get(this.httpClient, this.msaApplicationConfig, param);
            return this.login(msaAuthService.acquireToken());
        }

        /**
         * Login with the given refresh token.<br>
         * This is useful is you are migrating from another authentication library and want to migrate saved sessions.<br>
         * The application config must match the one used to obtain the refresh token.
         *
         * @param refreshToken The MSA refresh token.
         * @return A logged in {@link BedrockAuthManager}.
         */
        public BedrockAuthManager login(final String refreshToken) throws IOException {
            return this.login(this.httpClient.executeAndHandle(new MsaRefreshTokenRequest(this.msaApplicationConfig, refreshToken)));
        }

        /**
         * Login with the given {@link MsaToken}.<br>
         * This is useful is you have obtained the MSA token manually/externally.<br>
         * The application config must match the one used to obtain the token.
         *
         * @param msaToken The MSA token.
         * @return A logged in {@link BedrockAuthManager}.
         */
        public BedrockAuthManager login(final MsaToken msaToken) {
            return new BedrockAuthManager(
                    this.httpClient,
                    this.gameVersion,
                    this.msaApplicationConfig,
                    this.deviceType,
                    this.deviceKeyPair != null ? this.deviceKeyPair : CryptUtil.generateEcdsa256KeyPair(),
                    this.deviceId != null ? this.deviceId : UUID.randomUUID(),
                    this.sessionKeyPair != null ? this.sessionKeyPair : CryptUtil.generateEcdsa384KeyPair(),
                    msaToken
            );
        }

    }

}
