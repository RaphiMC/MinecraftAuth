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
package net.raphimc.minecraftauth;

import lombok.SneakyThrows;
import net.lenni0451.commons.httpclient.HttpClient;
import net.lenni0451.commons.httpclient.RetryHandler;
import net.lenni0451.commons.httpclient.constants.ContentTypes;
import net.lenni0451.commons.httpclient.constants.Headers;
import net.raphimc.minecraftauth.step.AbstractStep;
import net.raphimc.minecraftauth.step.BiMergeStep;
import net.raphimc.minecraftauth.step.bedrock.StepMCChain;
import net.raphimc.minecraftauth.step.bedrock.StepPlayFabToken;
import net.raphimc.minecraftauth.step.bedrock.session.StepFullBedrockSession;
import net.raphimc.minecraftauth.step.edu.StepEduJWT;
import net.raphimc.minecraftauth.step.java.StepMCProfile;
import net.raphimc.minecraftauth.step.java.StepMCToken;
import net.raphimc.minecraftauth.step.java.StepPlayerCertificates;
import net.raphimc.minecraftauth.step.java.session.StepFullJavaSession;
import net.raphimc.minecraftauth.step.msa.*;
import net.raphimc.minecraftauth.step.xbl.*;
import net.raphimc.minecraftauth.step.xbl.adapter.StepXblXstsToFullXblSession;
import net.raphimc.minecraftauth.step.xbl.session.StepFullXblSession;
import net.raphimc.minecraftauth.step.xbl.session.StepInitialXblSession;
import net.raphimc.minecraftauth.util.MicrosoftConstants;
import net.raphimc.minecraftauth.util.OAuthEnvironment;
import net.raphimc.minecraftauth.util.logging.ILogger;
import net.raphimc.minecraftauth.util.logging.LazyLogger;
import net.raphimc.minecraftauth.util.logging.Slf4jConsoleLogger;
import org.jetbrains.annotations.ApiStatus;

import java.lang.reflect.Constructor;
import java.util.function.Function;

public class MinecraftAuth {

    public static final String VERSION = "${version}";
    public static final String IMPL_VERSION = "${impl_version}";

    public static ILogger LOGGER = new LazyLogger(Slf4jConsoleLogger::new);
    public static String USER_AGENT = "MinecraftAuth/" + VERSION;

    public static final AbstractStep<?, StepFullJavaSession.FullJavaSession> JAVA_DEVICE_CODE_LOGIN = builder()
            .withClientId(MicrosoftConstants.JAVA_TITLE_ID).withScope(MicrosoftConstants.SCOPE_TITLE_AUTH)
            .deviceCode()
            .withDeviceToken("Win32")
            .sisuTitleAuthentication(MicrosoftConstants.JAVA_XSTS_RELYING_PARTY)
            .buildMinecraftJavaProfileStep(true);

    @ApiStatus.Experimental
    public static final AbstractStep<?, StepFullJavaSession.FullJavaSession> ALT_JAVA_DEVICE_CODE_LOGIN = builder()
            .withClientId(MicrosoftConstants.JAVA_TITLE_ID).withScope(MicrosoftConstants.SCOPE_TITLE_AUTH)
            .deviceCode()
            .withoutDeviceToken()
            .regularAuthentication(MicrosoftConstants.JAVA_XSTS_RELYING_PARTY)
            .buildMinecraftJavaProfileStep(true);

    public static final AbstractStep<?, StepFullJavaSession.FullJavaSession> JAVA_CREDENTIALS_LOGIN = builder()
            .withClientId(MicrosoftConstants.JAVA_TITLE_ID).withScope(MicrosoftConstants.SCOPE_TITLE_AUTH)
            .credentials()
            .withDeviceToken("Win32")
            .sisuTitleAuthentication(MicrosoftConstants.JAVA_XSTS_RELYING_PARTY)
            .buildMinecraftJavaProfileStep(true);

    @ApiStatus.Experimental
    public static final AbstractStep<?, StepFullJavaSession.FullJavaSession> ALT_JAVA_CREDENTIALS_LOGIN = builder()
            .withClientId(MicrosoftConstants.JAVA_TITLE_ID).withScope(MicrosoftConstants.SCOPE_TITLE_AUTH)
            .credentials()
            .withoutDeviceToken()
            .regularAuthentication(MicrosoftConstants.JAVA_XSTS_RELYING_PARTY)
            .buildMinecraftJavaProfileStep(true);

    public static final AbstractStep<?, StepFullBedrockSession.FullBedrockSession> BEDROCK_DEVICE_CODE_LOGIN = builder()
            .withClientId(MicrosoftConstants.BEDROCK_ANDROID_TITLE_ID).withScope(MicrosoftConstants.SCOPE_TITLE_AUTH)
            .deviceCode()
            .withDeviceToken("Android")
            .sisuTitleAuthentication(MicrosoftConstants.BEDROCK_XSTS_RELYING_PARTY)
            .buildMinecraftBedrockChainStep(true, false);

    public static final AbstractStep<?, StepFullBedrockSession.FullBedrockSession> BEDROCK_CREDENTIALS_LOGIN = builder()
            .withClientId(MicrosoftConstants.BEDROCK_ANDROID_TITLE_ID).withScope(MicrosoftConstants.SCOPE_TITLE_AUTH)
            .credentials()
            .withDeviceToken("Android")
            .sisuTitleAuthentication(MicrosoftConstants.BEDROCK_XSTS_RELYING_PARTY)
            .buildMinecraftBedrockChainStep(true, false);

    @ApiStatus.Experimental
    public static final AbstractStep<?, StepEduJWT.EduJWT> EDU_DEVICE_CODE_LOGIN = new StepEduJWT(builder()
            .withClientId(MicrosoftConstants.EDU_CLIENT_ID).withScope("https://meeservices.minecraft.net/.default offline_access").withOAuthEnvironment(OAuthEnvironment.MICROSOFT_ONLINE_COMMON)
            .deviceCode()
            .msaTokenStep, "1.20.13", 594);

    @ApiStatus.Experimental
    @SuppressWarnings("unchecked")
    public static final AbstractStep<?, StepXblSisuAuthentication.XblSisuTokens> BEDROCK_XBL_DEVICE_CODE_LOGIN = (AbstractStep<?, StepXblSisuAuthentication.XblSisuTokens>) builder()
            .withClientId(MicrosoftConstants.BEDROCK_ANDROID_TITLE_ID).withScope(MicrosoftConstants.SCOPE_TITLE_AUTH)
            .deviceCode()
            .withDeviceToken("Android")
            .sisuTitleAuthentication(MicrosoftConstants.XBL_XSTS_RELYING_PARTY)
            .xblXstsTokenStep;

    public static MsaTokenBuilder builder() {
        return new MsaTokenBuilder();
    }

    public static HttpClient createHttpClient() {
        final int timeout = 5000;

        return new HttpClient()
                .setConnectTimeout(timeout)
                .setReadTimeout(timeout * 2)
                .setCookieManager(null)
                .setFollowRedirects(false)
                .setRetryHandler(new RetryHandler(0, 50))
                .setHeader(Headers.ACCEPT, ContentTypes.APPLICATION_JSON.toString())
                .setHeader(Headers.ACCEPT_LANGUAGE, "en-US,en")
                .setHeader(Headers.USER_AGENT, USER_AGENT);
    }

    public static class MsaTokenBuilder {

        private AbstractStep.ApplicationDetails applicationDetails = new AbstractStep.ApplicationDetails(MicrosoftConstants.JAVA_TITLE_ID, MicrosoftConstants.SCOPE1, null, null, OAuthEnvironment.LIVE);
        private int timeout = 120;

        private AbstractStep<?, MsaCodeStep.MsaCode> msaCodeStep;

        /**
         * Sets the client id of the application
         *
         * @param clientId The client id
         * @return The builder
         */
        public MsaTokenBuilder withClientId(final String clientId) {
            this.applicationDetails = this.applicationDetails.withClientId(clientId);

            return this;
        }

        /**
         * Sets the scope of the application
         *
         * @param scope The scope
         * @return The builder
         */
        public MsaTokenBuilder withScope(final String scope) {
            this.applicationDetails = this.applicationDetails.withScope(scope);

            return this;
        }

        /**
         * Sets the client secret of the application
         *
         * @param clientSecret The client secret
         * @return The builder
         */
        public MsaTokenBuilder withClientSecret(final String clientSecret) {
            this.applicationDetails = this.applicationDetails.withClientSecret(clientSecret);

            return this;
        }

        /**
         * Sets the redirect uri to use for the local webserver or credentials auth flow
         *
         * @param redirectUri The redirect uri
         * @return The builder
         */
        public MsaTokenBuilder withRedirectUri(final String redirectUri) {
            this.applicationDetails = this.applicationDetails.withRedirectUri(redirectUri);

            return this;
        }

        /**
         * Sets the OAuth environment of the application
         *
         * @param oAuthEnvironment The OAuth environment
         * @return The builder
         */
        public MsaTokenBuilder withOAuthEnvironment(final OAuthEnvironment oAuthEnvironment) {
            this.applicationDetails = this.applicationDetails.withOAuthEnvironment(oAuthEnvironment);

            return this;
        }

        /**
         * Sets the application details
         *
         * @param applicationDetails The application details
         * @return The builder
         */
        public MsaTokenBuilder withApplicationDetails(final AbstractStep.ApplicationDetails applicationDetails) {
            this.applicationDetails = applicationDetails;

            return this;
        }

        /**
         * Sets the timeout of the device code or local webserver auth flow
         *
         * @param timeout The timeout in seconds
         * @return The builder
         */
        public MsaTokenBuilder withTimeout(final int timeout) {
            this.timeout = timeout;

            return this;
        }

        /**
         * Uses the device code flow to get an MSA token. <b>This is the recommended way to get an MSA token.</b><br>
         * Needs instance of {@link StepMsaDeviceCode.MsaDeviceCodeCallback} as input when calling {@link AbstractStep#getFromInput(HttpClient, AbstractStep.InitialInput)}.
         *
         * @return The builder
         */
        public InitialXblSessionBuilder deviceCode() {
            this.msaCodeStep = new StepMsaDeviceCodeMsaCode(new StepMsaDeviceCode(this.applicationDetails), this.timeout * 1000);

            return new InitialXblSessionBuilder(this);
        }

        /**
         * Logs in with a Microsoft account's credentials and gets an MSA token.<br>
         * Needs instance of {@link StepCredentialsMsaCode.MsaCredentials} as input when calling {@link AbstractStep#getFromInput(HttpClient, AbstractStep.InitialInput)}.
         *
         * @return The builder
         */
        public InitialXblSessionBuilder credentials() {
            if (this.applicationDetails.getRedirectUri() == null) {
                this.applicationDetails = this.applicationDetails.withRedirectUri(this.applicationDetails.getOAuthEnvironment().getNativeClientUrl());
            }

            this.msaCodeStep = new StepCredentialsMsaCode(this.applicationDetails);

            return new InitialXblSessionBuilder(this);
        }

        /**
         * Opens a JavaFX WebView window to get an MSA token. The window closes when the user logged in.<br>
         * Optionally accepts a {@link StepJfxWebViewMsaCode.JavaFxWebView} as input when calling {@link AbstractStep#getFromInput(HttpClient, AbstractStep.InitialInput)}.
         *
         * @return The builder
         */
        @SneakyThrows
        public InitialXblSessionBuilder javaFxWebView() {
            if (this.applicationDetails.getRedirectUri() == null) {
                this.applicationDetails = this.applicationDetails.withRedirectUri(this.applicationDetails.getOAuthEnvironment().getNativeClientUrl());
            }

            // Don't reference the constructor directly to prevent Spigot from loading JavaFX classes when not needed
            // Spigot's class remapper is crappy and loads classes even when the method isn't ever called
            final Constructor<?> constructor = StepJfxWebViewMsaCode.class.getConstructor(AbstractStep.ApplicationDetails.class, int.class);
            this.msaCodeStep = (AbstractStep<?, MsaCodeStep.MsaCode>) constructor.newInstance(this.applicationDetails, this.timeout * 1000);

            return new InitialXblSessionBuilder(this);
        }

        /**
         * Generates a URL to open in the browser to get an MSA token. The browser redirects to a localhost URL with the token as a parameter when the user logged in.<br>
         * Needs instance of {@link StepLocalWebServer.LocalWebServerCallback} as input when calling {@link AbstractStep#getFromInput(HttpClient, AbstractStep.InitialInput)}.
         *
         * @return The builder
         */
        public InitialXblSessionBuilder localWebServer() {
            if (this.applicationDetails.getRedirectUri() == null) {
                this.applicationDetails = this.applicationDetails.withRedirectUri("http://localhost");
            }

            this.msaCodeStep = new StepLocalWebServerMsaCode(new StepLocalWebServer(this.applicationDetails), this.timeout * 1000);

            return new InitialXblSessionBuilder(this);
        }

        /**
         * Uses the specified custom MSA code step to get an MSA token.
         *
         * @param msaCodeStepProvider The custom MSA code step provider
         * @return The builder
         */
        public InitialXblSessionBuilder customMsaCodeStep(final Function<AbstractStep.ApplicationDetails, AbstractStep<?, MsaCodeStep.MsaCode>> msaCodeStepProvider) {
            this.msaCodeStep = msaCodeStepProvider.apply(this.applicationDetails);

            return new InitialXblSessionBuilder(this);
        }

        @Deprecated
        public InitialXblSessionBuilder customMsaCodeStep(final AbstractStep<?, MsaCodeStep.MsaCode> msaCodeStep) {
            this.msaCodeStep = msaCodeStep;

            return new InitialXblSessionBuilder(this);
        }

        public AbstractStep<MsaCodeStep.MsaCode, StepMsaToken.MsaToken> build() {
            return new StepMsaToken(this.msaCodeStep);
        }

    }

    public static class InitialXblSessionBuilder {

        private final AbstractStep<MsaCodeStep.MsaCode, StepMsaToken.MsaToken> msaTokenStep;
        private BiMergeStep<StepMsaToken.MsaToken, StepXblDeviceToken.XblDeviceToken, StepInitialXblSession.InitialXblSession> initialXblSessionStep;

        private InitialXblSessionBuilder(final MsaTokenBuilder parent) {
            this.msaTokenStep = parent.build();
        }

        public XblXstsTokenBuilder withDeviceToken(final String deviceType) {
            this.initialXblSessionStep = new StepInitialXblSession(this.msaTokenStep, new StepXblDeviceToken(deviceType));

            return new XblXstsTokenBuilder(this);
        }

        public XblXstsTokenBuilder withoutDeviceToken() {
            this.initialXblSessionStep = new StepInitialXblSession(this.msaTokenStep, null);

            return new XblXstsTokenBuilder(this);
        }

        public BiMergeStep<StepMsaToken.MsaToken, StepXblDeviceToken.XblDeviceToken, StepInitialXblSession.InitialXblSession> build() {
            return this.initialXblSessionStep;
        }

    }

    public static class XblXstsTokenBuilder {

        private final BiMergeStep<StepMsaToken.MsaToken, StepXblDeviceToken.XblDeviceToken, StepInitialXblSession.InitialXblSession> initialXblSessionStep;
        private AbstractStep<?, ? extends StepXblXstsToken.XblXsts<?>> xblXstsTokenStep;

        private XblXstsTokenBuilder(final InitialXblSessionBuilder parent) {
            this.initialXblSessionStep = parent.build();
        }

        public MinecraftBuilder sisuTitleAuthentication(final String relyingParty) {
            this.xblXstsTokenStep = new StepXblSisuAuthentication(this.initialXblSessionStep, relyingParty);

            return new MinecraftBuilder(this);
        }

        public MinecraftBuilder titleAuthentication(final String relyingParty) {
            this.xblXstsTokenStep = new StepXblXstsToken(new StepFullXblSession(new StepXblUserToken(this.initialXblSessionStep), new StepXblTitleToken(this.initialXblSessionStep)), relyingParty);

            return new MinecraftBuilder(this);
        }

        public MinecraftBuilder regularAuthentication(final String relyingParty) {
            this.xblXstsTokenStep = new StepXblXstsToken(new StepFullXblSession(new StepXblUserToken(this.initialXblSessionStep), null), relyingParty);

            return new MinecraftBuilder(this);
        }

        public AbstractStep<?, ? extends StepXblXstsToken.XblXsts<?>> build() {
            return this.xblXstsTokenStep;
        }

    }

    public static class MinecraftBuilder {

        private final AbstractStep<?, ? extends StepXblXstsToken.XblXsts<?>> xblXstsTokenStep;

        private MinecraftBuilder(final XblXstsTokenBuilder parent) {
            this.xblXstsTokenStep = parent.build();
        }

        public StepFullJavaSession buildMinecraftJavaProfileStep(final boolean playerCertificates) {
            final StepMCToken mcTokenStep = new StepMCToken(this.xblXstsTokenStep);
            final StepPlayerCertificates playerCertificatesStep = playerCertificates ? new StepPlayerCertificates(mcTokenStep) : null;
            return new StepFullJavaSession(new StepMCProfile(mcTokenStep), playerCertificatesStep);
        }

        public StepFullBedrockSession buildMinecraftBedrockChainStep(final boolean playFabToken, final boolean realmsXsts) {
            final StepPlayFabToken playFabTokenStep = new StepPlayFabToken(new StepXblXstsToken(new StepXblXstsToFullXblSession(this.xblXstsTokenStep), MicrosoftConstants.BEDROCK_PLAY_FAB_XSTS_RELYING_PARTY));
            final StepXblXstsToken realmsXstsStep = new StepXblXstsToken("realmsXsts", new StepXblXstsToFullXblSession(this.xblXstsTokenStep), MicrosoftConstants.BEDROCK_REALMS_XSTS_RELYING_PARTY);
            return new StepFullBedrockSession(new StepMCChain(this.xblXstsTokenStep), playFabToken ? playFabTokenStep : null, realmsXsts ? realmsXstsStep : null);
        }

    }

}
