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
import org.apache.http.client.HttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MinecraftAuth {

    public static final Logger LOGGER = LoggerFactory.getLogger("MinecraftAuth");

    public static final AbstractStep<?, StepMCProfile.MCProfile> JAVA_DEVICE_CODE_LOGIN = builder()
            .deviceCode(MicrosoftConstants.JAVA_TITLE_ID, MicrosoftConstants.SCOPE_TITLE_AUTH)
            .withDeviceToken("Win32")
            .sisuTitleAuthentication(MicrosoftConstants.JAVA_XSTS_RELYING_PARTY)
            .buildMinecraftJavaProfileStep();

    public static final AbstractStep<?, StepMCProfile.MCProfile> JAVA_CREDENTIALS_LOGIN = builder()
            .credentials(MicrosoftConstants.JAVA_TITLE_ID, MicrosoftConstants.SCOPE_TITLE_AUTH)
            .withDeviceToken("Win32")
            .sisuTitleAuthentication(MicrosoftConstants.JAVA_XSTS_RELYING_PARTY)
            .buildMinecraftJavaProfileStep();

    public static final AbstractStep<?, StepMCChain.MCChain> BEDROCK_DEVICE_CODE_LOGIN = builder()
            .deviceCode(MicrosoftConstants.BEDROCK_NINTENDO_TITLE_ID, MicrosoftConstants.SCOPE_TITLE_AUTH)
            .withDeviceToken("Nintendo")
            .sisuTitleAuthentication(MicrosoftConstants.BEDROCK_XSTS_RELYING_PARTY)
            .buildMinecraftBedrockChainStep();

    public static final AbstractStep<?, StepMCChain.MCChain> BEDROCK_CREDENTIALS_LOGIN = builder()
            .credentials(MicrosoftConstants.JAVA_TITLE_ID, MicrosoftConstants.SCOPE_TITLE_AUTH)
            .withDeviceToken("Nintendo")
            .sisuTitleAuthentication(MicrosoftConstants.BEDROCK_XSTS_RELYING_PARTY)
            .buildMinecraftBedrockChainStep();

    public static MsaTokenBuilder builder() {
        return new MsaTokenBuilder();
    }

    public static class MsaTokenBuilder {

        private AbstractStep<?, MsaCodeStep.MsaCode> msaCodeStep;

        /**
         * Uses the device code flow to get an MSA token. <b>This is the recommended way to get an MSA token.</b>
         * Needs instance of {@link net.raphimc.mcauth.step.msa.StepMsaDeviceCode.MsaDeviceCodeCallback} as input when calling {@link AbstractStep#getFromInput(HttpClient, AbstractStep.InitialInput)}.
         *
         * @param clientId The client id of the application
         * @param scope    The scope of the application
         * @return The builder
         */
        public InitialXblSessionBuilder deviceCode(final String clientId, final String scope) {
            return this.deviceCode(clientId, scope, 60);
        }

        /**
         * Uses the device code flow to get an MSA token. <b>This is the recommended way to get an MSA token.</b>
         * Needs instance of {@link net.raphimc.mcauth.step.msa.StepMsaDeviceCode.MsaDeviceCodeCallback} as input when calling {@link AbstractStep#getFromInput(HttpClient, AbstractStep.InitialInput)}.
         *
         * @param clientId The client id of the application
         * @param scope    The scope of the application
         * @param timeout  The timeout in seconds
         * @return The builder
         */
        public InitialXblSessionBuilder deviceCode(final String clientId, final String scope, final int timeout) {
            this.msaCodeStep = new StepMsaDeviceCodeMsaCode(new StepMsaDeviceCode(clientId, scope), clientId, scope, timeout * 1000);

            return new InitialXblSessionBuilder(this);
        }

        /**
         * Generates a URL to open in the browser to get an MSA token. The browser redirects to a localhost URL with the token as a parameter when the user logged in.
         * Needs instance of {@link net.raphimc.mcauth.step.msa.StepExternalBrowser.ExternalBrowserCallback} as input when calling {@link AbstractStep#getFromInput(HttpClient, AbstractStep.InitialInput)}.
         *
         * @param clientId The client id of the application
         * @param scope    The scope of the application
         * @return The builder
         */
        public InitialXblSessionBuilder externalBrowser(final String clientId, final String scope) {
            return this.externalBrowser(clientId, scope, 60);
        }

        /**
         * Generates a URL to open in the browser to get an MSA token. The browser redirects to a localhost URL with the token as a parameter when the user logged in.
         * Needs instance of {@link net.raphimc.mcauth.step.msa.StepExternalBrowser.ExternalBrowserCallback} as input when calling {@link AbstractStep#getFromInput(HttpClient, AbstractStep.InitialInput)}.
         *
         * @param clientId The client id of the application
         * @param scope    The scope of the application
         * @param timeout  The timeout in seconds
         * @return The builder
         */
        public InitialXblSessionBuilder externalBrowser(final String clientId, final String scope, final int timeout) {
            this.msaCodeStep = new StepExternalBrowserMsaCode(new StepExternalBrowser(clientId, scope), clientId, scope, timeout * 1000);

            return new InitialXblSessionBuilder(this);
        }

        /**
         * Logs in with a Microsoft account and gets an MSA token.
         * Needs instance of {@link net.raphimc.mcauth.step.msa.StepCredentialsMsaCode.MsaCredentials} as input when calling {@link AbstractStep#getFromInput(HttpClient, AbstractStep.InitialInput)}.
         *
         * @param clientId The client id of the application
         * @param scope    The scope of the application
         * @return The builder
         */
        public InitialXblSessionBuilder credentials(final String clientId, final String scope) {
            this.msaCodeStep = new StepCredentialsMsaCode(clientId, scope, MicrosoftConstants.LIVE_OAUTH_DESKTOP_URL);

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
