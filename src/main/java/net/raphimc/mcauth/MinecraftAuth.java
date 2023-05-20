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
package net.raphimc.mcauth;

import net.raphimc.mcauth.step.AbstractStep;
import net.raphimc.mcauth.step.OptionalMergeStep;
import net.raphimc.mcauth.step.bedrock.StepMCChain;
import net.raphimc.mcauth.step.java.StepGameOwnership;
import net.raphimc.mcauth.step.java.StepMCProfile;
import net.raphimc.mcauth.step.java.StepMCToken;
import net.raphimc.mcauth.step.msa.*;
import net.raphimc.mcauth.step.xbl.*;
import net.raphimc.mcauth.step.xbl.session.StepFullXblSession;
import net.raphimc.mcauth.step.xbl.session.StepInitialXblSession;
import net.raphimc.mcauth.util.MicrosoftConstants;
import net.raphimc.mcauth.util.logging.ConsoleLogger;
import net.raphimc.mcauth.util.logging.ILogger;
import org.apache.http.client.HttpClient;

public class MinecraftAuth {

    public static ILogger LOGGER = new ConsoleLogger();

    public static final AbstractStep<?, StepMCProfile.MCProfile> JAVA_DEVICE_CODE_LOGIN = builder()
            .withClientId(MicrosoftConstants.JAVA_TITLE_ID).withScope(MicrosoftConstants.SCOPE_TITLE_AUTH)
            .deviceCode()
            .withDeviceToken("Win32")
            .sisuTitleAuthentication(MicrosoftConstants.JAVA_XSTS_RELYING_PARTY)
            .buildMinecraftJavaProfileStep();

    public static final AbstractStep<?, StepMCProfile.MCProfile> JAVA_CREDENTIALS_LOGIN = builder()
            .withClientId(MicrosoftConstants.JAVA_TITLE_ID).withScope(MicrosoftConstants.SCOPE_TITLE_AUTH)
            .credentials()
            .withDeviceToken("Win32")
            .sisuTitleAuthentication(MicrosoftConstants.JAVA_XSTS_RELYING_PARTY)
            .buildMinecraftJavaProfileStep();

    public static final AbstractStep<?, StepMCChain.MCChain> BEDROCK_DEVICE_CODE_LOGIN = builder()
            .withClientId(MicrosoftConstants.BEDROCK_NINTENDO_TITLE_ID).withScope(MicrosoftConstants.SCOPE_TITLE_AUTH)
            .deviceCode()
            .withDeviceToken("Nintendo")
            .sisuTitleAuthentication(MicrosoftConstants.BEDROCK_XSTS_RELYING_PARTY)
            .buildMinecraftBedrockChainStep();

    public static final AbstractStep<?, StepMCChain.MCChain> BEDROCK_CREDENTIALS_LOGIN = builder()
            .withClientId(MicrosoftConstants.BEDROCK_NINTENDO_TITLE_ID).withScope(MicrosoftConstants.SCOPE_TITLE_AUTH)
            .credentials()
            .withDeviceToken("Nintendo")
            .sisuTitleAuthentication(MicrosoftConstants.BEDROCK_XSTS_RELYING_PARTY)
            .buildMinecraftBedrockChainStep();

    public static MsaTokenBuilder builder() {
        return new MsaTokenBuilder();
    }

    public static class MsaTokenBuilder {

        private String clientId = MicrosoftConstants.JAVA_TITLE_ID;
        private String scope = MicrosoftConstants.SCOPE1;
        private String clientSecret = null;
        private int timeout = 60;
        private String redirectUri = null;

        private AbstractStep<?, MsaCodeStep.MsaCode> msaCodeStep;

        /**
         * Sets the client id of the application
         *
         * @param clientId The client id
         * @return The builder
         */
        public MsaTokenBuilder withClientId(final String clientId) {
            this.clientId = clientId;

            return this;
        }

        /**
         * Sets the scope of the application
         *
         * @param scope The scope
         * @return The builder
         */
        public MsaTokenBuilder withScope(final String scope) {
            this.scope = scope;

            return this;
        }

        /**
         * Sets the client secret of the application
         *
         * @param clientSecret The client secret
         * @return The builder
         */
        public MsaTokenBuilder withClientSecret(final String clientSecret) {
            this.clientSecret = clientSecret;

            return this;
        }

        /**
         * Sets the timeout of the device code or external browser auth flow
         *
         * @param timeout The timeout in seconds
         * @return The builder
         */
        public MsaTokenBuilder withTimeout(final int timeout) {
            this.timeout = timeout;

            return this;
        }

        /**
         * Sets the redirect uri to use for the external browser or credentials auth flow
         *
         * @param redirectUri The redirect uri
         * @return The builder
         */
        public MsaTokenBuilder withRedirectUri(final String redirectUri) {
            this.redirectUri = redirectUri;

            return this;
        }

        /**
         * Uses the device code flow to get an MSA token. <b>This is the recommended way to get an MSA token.</b>
         * Needs instance of {@link net.raphimc.mcauth.step.msa.StepMsaDeviceCode.MsaDeviceCodeCallback} as input when calling {@link AbstractStep#getFromInput(HttpClient, AbstractStep.InitialInput)}.
         *
         * @return The builder
         */
        public InitialXblSessionBuilder deviceCode() {
            this.msaCodeStep = new StepMsaDeviceCodeMsaCode(new StepMsaDeviceCode(this.clientId, this.scope), this.clientId, this.scope, this.clientSecret, this.timeout * 1000);

            return new InitialXblSessionBuilder(this);
        }

        /**
         * Generates a URL to open in the browser to get an MSA token. The browser redirects to a localhost URL with the token as a parameter when the user logged in.
         * Needs instance of {@link net.raphimc.mcauth.step.msa.StepExternalBrowser.ExternalBrowserCallback} as input when calling {@link AbstractStep#getFromInput(HttpClient, AbstractStep.InitialInput)}.
         *
         * @return The builder
         */
        public InitialXblSessionBuilder externalBrowser() {
            if (this.redirectUri == null) {
                this.redirectUri = "http://localhost";
            }

            this.msaCodeStep = new StepExternalBrowserMsaCode(new StepExternalBrowser(this.clientId, this.scope, this.redirectUri), this.clientId, this.scope, this.clientSecret, this.timeout * 1000);

            return new InitialXblSessionBuilder(this);
        }

        /**
         * Logs in with a Microsoft account's credentials and gets an MSA token.
         * Needs instance of {@link net.raphimc.mcauth.step.msa.StepCredentialsMsaCode.MsaCredentials} as input when calling {@link AbstractStep#getFromInput(HttpClient, AbstractStep.InitialInput)}.
         *
         * @return The builder
         */
        public InitialXblSessionBuilder credentials() {
            if (this.redirectUri == null) {
                this.redirectUri = MicrosoftConstants.LIVE_OAUTH_DESKTOP_URL;
            }

            this.msaCodeStep = new StepCredentialsMsaCode(this.clientId, this.scope, this.clientSecret, this.redirectUri);

            return new InitialXblSessionBuilder(this);
        }

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
        private OptionalMergeStep<StepMsaToken.MsaToken, StepXblDeviceToken.XblDeviceToken, StepInitialXblSession.InitialXblSession> initialXblSessionStep;

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

        public OptionalMergeStep<StepMsaToken.MsaToken, StepXblDeviceToken.XblDeviceToken, StepInitialXblSession.InitialXblSession> build() {
            return this.initialXblSessionStep;
        }

    }

    public static class XblXstsTokenBuilder {

        private final OptionalMergeStep<StepMsaToken.MsaToken, StepXblDeviceToken.XblDeviceToken, StepInitialXblSession.InitialXblSession> initialXblSessionStep;
        private AbstractStep<?, StepXblXstsToken.XblXsts<?>> xblXstsTokenStep;

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

        public AbstractStep<?, StepXblXstsToken.XblXsts<?>> build() {
            return this.xblXstsTokenStep;
        }

    }

    public static class MinecraftBuilder {

        private final AbstractStep<?, StepXblXstsToken.XblXsts<?>> xblXstsTokenStep;

        private MinecraftBuilder(final XblXstsTokenBuilder parent) {
            this.xblXstsTokenStep = parent.build();
        }

        public AbstractStep<StepGameOwnership.GameOwnership, StepMCProfile.MCProfile> buildMinecraftJavaProfileStep() {
            return new StepMCProfile(new StepGameOwnership(new StepMCToken(this.xblXstsTokenStep)));
        }

        public AbstractStep<StepXblXstsToken.XblXsts<?>, StepMCChain.MCChain> buildMinecraftBedrockChainStep() {
            return new StepMCChain(this.xblXstsTokenStep);
        }

    }

}
