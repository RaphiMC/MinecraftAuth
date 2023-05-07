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
package net.raphimc.mcauth.step.msa;

import net.raphimc.mcauth.MinecraftAuth;
import net.raphimc.mcauth.step.AbstractStep;
import org.apache.http.client.HttpClient;

import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StepExternalBrowserMsaCode extends MsaCodeStep<StepExternalBrowser.ExternalBrowser> {

    private final int timeout;

    public StepExternalBrowserMsaCode(AbstractStep<?, StepExternalBrowser.ExternalBrowser> prevStep, String clientId, String scope, final int timeout) {
        this(prevStep, clientId, scope, null, timeout);
    }

    public StepExternalBrowserMsaCode(AbstractStep<?, StepExternalBrowser.ExternalBrowser> prevStep, String clientId, String scope, final String clientSecret, final int timeout) {
        super(prevStep, clientId, scope, clientSecret);

        this.timeout = timeout;
    }

    @Override
    public MsaCode applyStep(HttpClient httpClient, StepExternalBrowser.ExternalBrowser prevResult) throws Exception {
        MinecraftAuth.LOGGER.info("Waiting for MSA login via external browser...");

        try (final ServerSocket localServer = new ServerSocket(prevResult.port())) {
            localServer.setSoTimeout(this.timeout);
            try {
                try (final Socket client = localServer.accept()) {
                    final Scanner scanner = new Scanner(client.getInputStream());
                    final String get = scanner.nextLine();
                    final String response = "HTTP/1.1 200 OK\r\nConnection: Close\r\n\r\nYou have been logged in! You can close this window.";
                    client.getOutputStream().write(response.getBytes(StandardCharsets.UTF_8));

                    final Matcher m = Pattern.compile("code=([^&\\s]+)").matcher(get);
                    if (m.find()) {
                        final MsaCode result = new MsaCode(m.group(1), this.clientId, this.scope, this.clientSecret, prevResult.redirectUri());
                        MinecraftAuth.LOGGER.info("Got MSA Code");
                        return result;
                    }
                    throw new RuntimeException("Could not find MSA code");
                }
            } catch (SocketTimeoutException e) {
                throw new RuntimeException("Failed to get MSA Code. Login timed out");
            }
        }
    }

}
