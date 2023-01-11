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
import net.raphimc.mcauth.step.bedrock.StepMCChain;
import net.raphimc.mcauth.step.java.StepGameOwnership;
import net.raphimc.mcauth.step.java.StepMCProfile;
import net.raphimc.mcauth.step.java.StepMCToken;
import net.raphimc.mcauth.step.msa.*;
import net.raphimc.mcauth.step.xbl.StepXblDeviceToken;
import net.raphimc.mcauth.step.xbl.StepXblSisuAuthentication;
import net.raphimc.mcauth.step.xbl.session.StepInitialXblSession;
import net.raphimc.mcauth.util.MicrosoftConstants;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

public class MinecraftAuth {

    public static final Logger LOGGER = LoggerFactory.getLogger("MinecraftAuth");

    public static class Java {

        public static class Title {
            // MSA Code -> MSA Token
            public static final MsaCodeStep<AbstractStep.StepResult<?>> MSA_CODE = new MsaCodeStep<>(null, MicrosoftConstants.JAVA_TITLE_ID, MicrosoftConstants.SCOPE_TITLE_AUTH);
            public static final StepMsaToken MSA_TOKEN = new StepMsaToken(MSA_CODE);

            // Nothing -> XBL Device Token
            public static final StepXblDeviceToken XBL_DEVICE_TOKEN = new StepXblDeviceToken("Win32");

            // MSA Token + XBL Device Token => Initial XBL Session
            public static final StepInitialXblSession INITIAL_XBL_SESSION = new StepInitialXblSession(MSA_TOKEN, XBL_DEVICE_TOKEN);

            // Initial XBL Session -> XBL XSTS Token
            public static final StepXblSisuAuthentication XBL_SISU_AUTHENTICATION = new StepXblSisuAuthentication(INITIAL_XBL_SESSION, MicrosoftConstants.JAVA_XSTS_RELYING_PARTY);

            // XBL XSTS Token -> MC Profile
            public static final StepMCToken MC_TOKEN = new StepMCToken(XBL_SISU_AUTHENTICATION);
            public static final StepGameOwnership GAME_OWNERSHIP = new StepGameOwnership(MC_TOKEN);
            public static final StepMCProfile MC_PROFILE = new StepMCProfile(GAME_OWNERSHIP);
        }

        public static class DeviceCode {
            // Device Code -> MSA Code
            public static final StepMsaDeviceCode DEVICE_CODE = new StepMsaDeviceCode(MicrosoftConstants.JAVA_TITLE_ID, MicrosoftConstants.SCOPE_TITLE_AUTH);
            public static final StepMsaDeviceCodeMsaCode MSA_CODE = new StepMsaDeviceCodeMsaCode(DEVICE_CODE, MicrosoftConstants.JAVA_TITLE_ID, MicrosoftConstants.SCOPE_TITLE_AUTH, 60_000);
        }

        public static class Credentials {
            // Credentials -> MSA Code
            public static final StepCredentialsMsaCode MSA_CODE = new StepCredentialsMsaCode(MicrosoftConstants.JAVA_TITLE_ID, MicrosoftConstants.SCOPE_TITLE_AUTH, MicrosoftConstants.LIVE_OAUTH_DESKTOP_URL);
        }

    }

    public static class Bedrock {

        public static class Title {
            // MSA Code -> MSA Token
            public static final MsaCodeStep<AbstractStep.StepResult<?>> MSA_CODE = new MsaCodeStep<>(null, MicrosoftConstants.BEDROCK_NINTENDO_TITLE_ID, MicrosoftConstants.SCOPE_TITLE_AUTH);
            public static final StepMsaToken MSA_TOKEN = new StepMsaToken(MSA_CODE);

            // Nothing -> XBL Device Token
            public static final StepXblDeviceToken XBL_DEVICE_TOKEN = new StepXblDeviceToken("Nintendo");

            // MSA Token + XBL Device Token => Initial XBL Session
            public static final StepInitialXblSession INITIAL_XBL_SESSION = new StepInitialXblSession(MSA_TOKEN, XBL_DEVICE_TOKEN);

            // Initial XBL Session -> XBL XSTS Token
            public static final StepXblSisuAuthentication XBL_SISU_AUTHENTICATION = new StepXblSisuAuthentication(INITIAL_XBL_SESSION, MicrosoftConstants.BEDROCK_XSTS_RELYING_PARTY);

            // XBL XSTS Token -> MC Chain
            public static final StepMCChain MC_CHAIN = new StepMCChain(XBL_SISU_AUTHENTICATION);
        }

        public static class DeviceCode {
            // Device Code -> MSA Code
            public static final StepMsaDeviceCode DEVICE_CODE = new StepMsaDeviceCode(MicrosoftConstants.BEDROCK_NINTENDO_TITLE_ID, MicrosoftConstants.SCOPE_TITLE_AUTH);
            public static final StepMsaDeviceCodeMsaCode MSA_CODE = new StepMsaDeviceCodeMsaCode(DEVICE_CODE, MicrosoftConstants.BEDROCK_NINTENDO_TITLE_ID, MicrosoftConstants.SCOPE_TITLE_AUTH, 60_000);
        }

        public static class Credentials {
            // Credentials -> MSA Code
            public static final StepCredentialsMsaCode MSA_CODE = new StepCredentialsMsaCode(MicrosoftConstants.BEDROCK_NINTENDO_TITLE_ID, MicrosoftConstants.SCOPE_TITLE_AUTH, MicrosoftConstants.LIVE_OAUTH_DESKTOP_URL);
        }

    }

    public static StepMCProfile.MCProfile requestJavaLogin(final Consumer<StepMsaDeviceCode.MsaDeviceCode> msaDeviceCodeConsumer) throws Exception {
        try (final CloseableHttpClient httpClient = MicrosoftConstants.createHttpClient()) {
            final StepMsaDeviceCode.MsaDeviceCode msaDeviceCode = Java.DeviceCode.DEVICE_CODE.applyStep(httpClient, null);
            msaDeviceCodeConsumer.accept(msaDeviceCode);
            final MsaCodeStep.MsaCode msaCode = Java.DeviceCode.MSA_CODE.applyStep(httpClient, msaDeviceCode);
            return javaTitleLogin(httpClient, msaCode);
        }
    }

    public static StepMCProfile.MCProfile requestJavaLogin(final StepCredentialsMsaCode.MsaCredentials msaCredentials) throws Exception {
        try (final CloseableHttpClient httpClient = MicrosoftConstants.createHttpClient()) {
            final MsaCodeStep.MsaCode msaCode = Java.Credentials.MSA_CODE.applyStep(httpClient, msaCredentials);
            return javaTitleLogin(httpClient, msaCode);
        }
    }

    private static StepMCProfile.MCProfile javaTitleLogin(final CloseableHttpClient httpClient, final MsaCodeStep.MsaCode msaCode) throws Exception {
        final StepMsaToken.MsaToken msaToken = Java.Title.MSA_TOKEN.applyStep(httpClient, msaCode);
        final StepXblDeviceToken.XblDeviceToken xblDeviceToken = Java.Title.XBL_DEVICE_TOKEN.applyStep(httpClient, null);
        final StepInitialXblSession.InitialXblSession initialXblSession = Java.Title.INITIAL_XBL_SESSION.applyStep(httpClient, msaToken, xblDeviceToken);
        final StepXblSisuAuthentication.XblSisuTokens xblSisuTokens = Java.Title.XBL_SISU_AUTHENTICATION.applyStep(httpClient, initialXblSession);
        final StepMCToken.MCToken mcToken = Java.Title.MC_TOKEN.applyStep(httpClient, xblSisuTokens);
        final StepGameOwnership.GameOwnership gameOwnership = Java.Title.GAME_OWNERSHIP.applyStep(httpClient, mcToken);
        return Java.Title.MC_PROFILE.applyStep(httpClient, gameOwnership);
    }

    public static StepMCChain.MCChain requestBedrockLogin(final Consumer<StepMsaDeviceCode.MsaDeviceCode> msaDeviceCodeConsumer) throws Exception {
        try (final CloseableHttpClient httpClient = MicrosoftConstants.createHttpClient()) {
            final StepMsaDeviceCode.MsaDeviceCode msaDeviceCode = Bedrock.DeviceCode.DEVICE_CODE.applyStep(httpClient, null);
            msaDeviceCodeConsumer.accept(msaDeviceCode);
            final MsaCodeStep.MsaCode msaCode = Bedrock.DeviceCode.MSA_CODE.applyStep(httpClient, msaDeviceCode);
            return bedrockTitleLogin(httpClient, msaCode);
        }
    }

    public static StepMCChain.MCChain requestBedrockLogin(final StepCredentialsMsaCode.MsaCredentials msaCredentials) throws Exception {
        try (final CloseableHttpClient httpClient = MicrosoftConstants.createHttpClient()) {
            final MsaCodeStep.MsaCode msaCode = Bedrock.Credentials.MSA_CODE.applyStep(httpClient, msaCredentials);
            return bedrockTitleLogin(httpClient, msaCode);
        }
    }

    private static StepMCChain.MCChain bedrockTitleLogin(final CloseableHttpClient httpClient, final MsaCodeStep.MsaCode msaCode) throws Exception {
        final StepMsaToken.MsaToken msaToken = Bedrock.Title.MSA_TOKEN.applyStep(httpClient, msaCode);
        final StepXblDeviceToken.XblDeviceToken xblDeviceToken = Bedrock.Title.XBL_DEVICE_TOKEN.applyStep(httpClient, null);
        final StepInitialXblSession.InitialXblSession initialXblSession = Bedrock.Title.INITIAL_XBL_SESSION.applyStep(httpClient, msaToken, xblDeviceToken);
        final StepXblSisuAuthentication.XblSisuTokens xblSisuTokens = Bedrock.Title.XBL_SISU_AUTHENTICATION.applyStep(httpClient, initialXblSession);
        return Bedrock.Title.MC_CHAIN.applyStep(httpClient, xblSisuTokens);
    }

}
