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
package net.raphimc.minecraftauth.java;

import com.google.gson.JsonObject;
import lombok.*;
import lombok.experimental.Accessors;
import net.lenni0451.commons.gson.elements.GsonObject;
import net.lenni0451.commons.httpclient.HttpClient;
import net.raphimc.minecraftauth.java.model.MinecraftPlayerCertificates;
import net.raphimc.minecraftauth.java.model.MinecraftProfile;
import net.raphimc.minecraftauth.java.model.MinecraftToken;
import net.raphimc.minecraftauth.java.request.MinecraftLauncherLoginRequest;
import net.raphimc.minecraftauth.java.request.MinecraftPlayerCertificatesRequest;
import net.raphimc.minecraftauth.java.request.MinecraftProfileRequest;
import net.raphimc.minecraftauth.msa.data.MsaConstants;
import net.raphimc.minecraftauth.msa.model.MsaApplicationConfig;
import net.raphimc.minecraftauth.msa.model.MsaToken;
import net.raphimc.minecraftauth.msa.request.MsaRefreshTokenRequest;
import net.raphimc.minecraftauth.msa.service.MsaAuthService;
import net.raphimc.minecraftauth.msa.service.util.MsaAuthServiceSupplier;
import net.raphimc.minecraftauth.msa.service.util.ParamMsaAuthServiceSupplier;
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
public class JavaAuthManager {

    public static JavaAuthManager fromJson(final HttpClient httpClient, final JsonObject json) {
        return fromJson(httpClient, new GsonObject(json));
    }

    public static JavaAuthManager fromJson(final HttpClient httpClient, final GsonObject json) {
        return new JavaAuthManager(
                httpClient,
                MsaApplicationConfig.fromJson(json.reqObject("msaApplicationConfig")),
                json.reqString("deviceType"),
                JsonUtil.decodeKeyPair(json.reqObject("deviceKeyPair")),
                UUID.fromString(json.reqString("deviceId")),
                MsaToken.fromJson(json.reqObject("msaToken")),
                json.optObject("xblDeviceToken").map(XblDeviceToken::fromJson).orElse(null),
                json.optObject("xblUserToken").map(XblUserToken::fromJson).orElse(null),
                json.optObject("xblTitleToken").map(XblTitleToken::fromJson).orElse(null),
                json.optObject("javaXstsToken").map(XblXstsToken::fromJson).orElse(null),
                json.optObject("minecraftToken").map(MinecraftToken::fromJson).orElse(null),
                json.optObject("minecraftProfile").map(MinecraftProfile::fromJson).orElse(null),
                json.optObject("minecraftPlayerCertificates").map(MinecraftPlayerCertificates::fromJson).orElse(null)
        );
    }

    public static JsonObject toJson(final JavaAuthManager authManager) {
        final JsonObject json = new JsonObject();
        json.addProperty("_saveVersion", 1);
        json.add("msaApplicationConfig", MsaApplicationConfig.toJson(authManager.msaApplicationConfig));
        json.addProperty("deviceType", authManager.deviceType);
        json.add("deviceKeyPair", JsonUtil.encodeKeyPair(authManager.deviceKeyPair));
        json.addProperty("deviceId", authManager.deviceId.toString());
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
        if (authManager.javaXstsToken.hasValue()) {
            json.add("javaXstsToken", XblXstsToken.toJson(authManager.javaXstsToken.getCached()));
        }
        if (authManager.minecraftToken.hasValue()) {
            json.add("minecraftToken", MinecraftToken.toJson(authManager.minecraftToken.getCached()));
        }
        if (authManager.minecraftProfile.hasValue()) {
            json.add("minecraftProfile", MinecraftProfile.toJson(authManager.minecraftProfile.getCached()));
        }
        if (authManager.minecraftPlayerCertificates.hasValue()) {
            json.add("minecraftPlayerCertificates", MinecraftPlayerCertificates.toJson(authManager.minecraftPlayerCertificates.getCached()));
        }
        return json;
    }

    public static Builder create(final HttpClient httpClient) {
        return new Builder(httpClient);
    }

    private final HttpClient httpClient;
    private final MsaApplicationConfig msaApplicationConfig;
    private final String deviceType;
    private final KeyPair deviceKeyPair;
    private final UUID deviceId;
    private final ChangeListeners changeListeners = new ChangeListeners();

    @Getter(AccessLevel.NONE)
    private final Object sisuTokenLock = new Object();

    private final Holder<MsaToken> msaToken = new Holder<>(this::refreshMsaToken);
    private final Holder<XblDeviceToken> xblDeviceToken = new Holder<>(this::refreshXblDeviceToken);
    private final Holder<XblUserToken> xblUserToken = new Holder<>(this::refreshXblUserToken, this.sisuTokenLock);
    private final Holder<XblTitleToken> xblTitleToken = new Holder<>(this::refreshXblTitleToken, this.sisuTokenLock);
    private final Holder<XblXstsToken> javaXstsToken = new Holder<>(this::refreshJavaXstsToken, this.sisuTokenLock);
    private final Holder<MinecraftToken> minecraftToken = new Holder<>(this::refreshMinecraftToken);
    private final Holder<MinecraftProfile> minecraftProfile = new Holder<>(this::refreshMinecraftProfile);
    private final Holder<MinecraftPlayerCertificates> minecraftPlayerCertificates = new Holder<>(this::refreshMinecraftPlayerCertificates);

    private JavaAuthManager(final HttpClient httpClient, final MsaApplicationConfig msaApplicationConfig, final String deviceType, final KeyPair deviceKeyPair, final UUID deviceId, final MsaToken msaToken) {
        this.httpClient = httpClient;
        this.msaApplicationConfig = msaApplicationConfig;
        this.deviceType = deviceType;
        this.deviceKeyPair = deviceKeyPair;
        this.deviceId = deviceId;
        this.msaToken.set(msaToken);
        this.hookChangeListeners();
    }

    private JavaAuthManager(final HttpClient httpClient, final MsaApplicationConfig msaApplicationConfig, final String deviceType, final KeyPair deviceKeyPair, final UUID deviceId, final MsaToken msaToken, final XblDeviceToken xblDeviceToken, final XblUserToken xblUserToken, final XblTitleToken xblTitleToken, final XblXstsToken javaXstsToken, final MinecraftToken minecraftToken, final MinecraftProfile minecraftProfile, final MinecraftPlayerCertificates minecraftPlayerCertificates) {
        this.httpClient = httpClient;
        this.msaApplicationConfig = msaApplicationConfig;
        this.deviceType = deviceType;
        this.deviceKeyPair = deviceKeyPair;
        this.deviceId = deviceId;
        this.msaToken.set(msaToken);
        this.xblDeviceToken.set(xblDeviceToken);
        this.xblUserToken.set(xblUserToken);
        this.xblTitleToken.set(xblTitleToken);
        this.javaXstsToken.set(javaXstsToken);
        this.minecraftToken.set(minecraftToken);
        this.minecraftProfile.set(minecraftProfile);
        this.minecraftPlayerCertificates.set(minecraftPlayerCertificates);
        this.hookChangeListeners();
    }

    @SneakyThrows
    private MsaToken refreshMsaToken() {
        if (this.msaToken.getCached().getRefreshToken() == null) {
            throw new IllegalStateException("Can't refresh MSA token, because it was created without a refresh token. The user has to sign in again.");
        }
        return this.httpClient.executeAndHandle(new MsaRefreshTokenRequest(this.msaApplicationConfig, this.msaToken.getCached()));
    }

    @SneakyThrows
    private XblDeviceToken refreshXblDeviceToken() {
        return this.httpClient.executeAndHandle(new XblDeviceAuthenticateRequest(this.deviceType, this.deviceId, this.deviceKeyPair));
    }

    @SneakyThrows
    private XblUserToken refreshXblUserToken() {
        if (this.msaApplicationConfig.isTitleClientId()) {
            this.refreshSisuTokens();
            return this.xblUserToken.getCached();
        } else {
            return this.httpClient.executeAndHandle(new XblUserAuthenticateRequest(this.msaApplicationConfig, this.msaToken.getUpToDate()));
        }
    }

    @SneakyThrows
    private XblTitleToken refreshXblTitleToken() {
        if (!this.msaApplicationConfig.isTitleClientId()) {
            throw new UnsupportedOperationException("Can't refresh XBL title token, because the MSA application client ID is not a title client ID");
        }
        this.refreshSisuTokens();
        return this.xblTitleToken.getCached();
    }

    @SneakyThrows
    private XblXstsToken refreshJavaXstsToken() {
        if (this.msaApplicationConfig.isTitleClientId()) {
            this.refreshSisuTokens();
            return this.javaXstsToken.getCached();
        } else {
            return this.httpClient.executeAndHandle(new XblXstsAuthorizeRequest(this.xblDeviceToken.getUpToDate(), this.xblUserToken.getUpToDate(), null, XblConstants.JAVA_XSTS_RELYING_PARTY));
        }
    }

    @SneakyThrows
    private MinecraftToken refreshMinecraftToken() {
        return this.httpClient.executeAndHandle(new MinecraftLauncherLoginRequest(this.javaXstsToken.getUpToDate()));
    }

    @SneakyThrows
    private MinecraftProfile refreshMinecraftProfile() {
        return this.httpClient.executeAndHandle(new MinecraftProfileRequest(this.minecraftToken.getUpToDate()));
    }

    @SneakyThrows
    private MinecraftPlayerCertificates refreshMinecraftPlayerCertificates() {
        return this.httpClient.executeAndHandle(new MinecraftPlayerCertificatesRequest(this.minecraftToken.getUpToDate()));
    }

    private void refreshSisuTokens() throws IOException {
        final XblSisuTokens sisuTokens = this.httpClient.executeAndHandle(new XblSisuAuthorizeRequest(this.msaApplicationConfig, this.msaToken.getUpToDate(), this.xblDeviceToken.getUpToDate(), this.deviceKeyPair, XblConstants.JAVA_XSTS_RELYING_PARTY));
        this.xblUserToken.set(sisuTokens.getUserToken());
        this.xblTitleToken.set(sisuTokens.getTitleToken());
        this.javaXstsToken.set(sisuTokens.getXstsToken());
    }

    private void hookChangeListeners() {
        this.msaToken.getChangeListeners().add(this.changeListeners::invoke);
        this.xblDeviceToken.getChangeListeners().add(this.changeListeners::invoke);
        this.xblUserToken.getChangeListeners().add(this.changeListeners::invoke);
        this.xblTitleToken.getChangeListeners().add(this.changeListeners::invoke);
        this.javaXstsToken.getChangeListeners().add(this.changeListeners::invoke);
        this.minecraftToken.getChangeListeners().add(this.changeListeners::invoke);
        this.minecraftProfile.getChangeListeners().add(this.changeListeners::invoke);
        this.minecraftPlayerCertificates.getChangeListeners().add(this.changeListeners::invoke);
    }

    @Setter
    @Accessors(fluent = true)
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Builder {

        private final HttpClient httpClient;
        private MsaApplicationConfig msaApplicationConfig = new MsaApplicationConfig(MsaConstants.JAVA_TITLE_ID, MsaConstants.SCOPE_TITLE_AUTH);
        private String deviceType = "Win32";
        private KeyPair deviceKeyPair;
        private UUID deviceId;

        /**
         * Login with the given {@link MsaAuthServiceSupplier}.
         *
         * @param msaAuthServiceSupplier The MSA auth service supplier (For example {@link net.raphimc.minecraftauth.msa.service.impl.JfxWebViewMsaAuthService}).
         * @return A logged in {@link JavaAuthManager}.
         */
        public JavaAuthManager login(final MsaAuthServiceSupplier msaAuthServiceSupplier) throws IOException, InterruptedException, TimeoutException {
            final MsaAuthService msaAuthService = msaAuthServiceSupplier.get(this.httpClient, this.msaApplicationConfig);
            return this.login(msaAuthService.acquireToken());
        }

        /**
         * Login with the given {@link ParamMsaAuthServiceSupplier} and parameter.
         *
         * @param msaAuthServiceSupplier The MSA auth service supplier (For example {@link net.raphimc.minecraftauth.msa.service.impl.DeviceCodeMsaAuthService} or {@link net.raphimc.minecraftauth.msa.service.impl.CredentialsMsaAuthService}).
         * @param param                  The parameter to pass to the supplier.
         * @param <T>                    The type of the parameter.
         * @return A logged in {@link JavaAuthManager}.
         */
        public <T> JavaAuthManager login(final ParamMsaAuthServiceSupplier<T> msaAuthServiceSupplier, final T param) throws IOException, InterruptedException, TimeoutException {
            final MsaAuthService msaAuthService = msaAuthServiceSupplier.get(this.httpClient, this.msaApplicationConfig, param);
            return this.login(msaAuthService.acquireToken());
        }

        /**
         * Login with the given refresh token.<br>
         * This is useful is you are migrating from another authentication library and want to migrate saved sessions.<br>
         * The application config must match the one used to obtain the refresh token.
         *
         * @param refreshToken The MSA refresh token.
         * @return A logged in {@link JavaAuthManager}.
         */
        public JavaAuthManager login(final String refreshToken) throws IOException {
            return this.login(this.httpClient.executeAndHandle(new MsaRefreshTokenRequest(this.msaApplicationConfig, refreshToken)));
        }

        /**
         * Login with the given {@link MsaToken}.<br>
         * This is useful is you have obtained the MSA token manually/externally.<br>
         * The application config must match the one used to obtain the token.
         *
         * @param msaToken The MSA token.
         * @return A logged in {@link JavaAuthManager}.
         */
        public JavaAuthManager login(final MsaToken msaToken) {
            return new JavaAuthManager(
                    this.httpClient,
                    this.msaApplicationConfig,
                    this.deviceType,
                    this.deviceKeyPair != null ? this.deviceKeyPair : CryptUtil.generateEcdsa256KeyPair(),
                    this.deviceId != null ? this.deviceId : UUID.randomUUID(),
                    msaToken
            );
        }

    }

}
