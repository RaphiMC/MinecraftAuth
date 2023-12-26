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
package net.raphimc.minecraftauth.step.msa;

import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.step.AbstractStep;
import org.apache.http.client.HttpClient;

import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StepLocalWebServerMsaCode extends MsaCodeStep<StepLocalWebServer.LocalWebServer> {

    private final int timeout;

    public StepLocalWebServerMsaCode(final AbstractStep<?, StepLocalWebServer.LocalWebServer> prevStep, final int timeout) {
        super(prevStep);

        this.timeout = timeout;
    }

    @Override
    public MsaCode applyStep(final HttpClient httpClient, final StepLocalWebServer.LocalWebServer localWebServer) throws Exception {
        MinecraftAuth.LOGGER.info("Waiting for MSA login via local webserver...");

        try (final ServerSocket localServer = new ServerSocket(localWebServer.getPort())) {
            localServer.setSoTimeout(this.timeout);
            try {
                try (final Socket client = localServer.accept()) {
                    final Scanner scanner = new Scanner(client.getInputStream());
                    final String get = scanner.nextLine();
                    final String response = "HTTP/1.1 200 OK\r\nConnection: Close\r\n\r\nYou have been logged in! You can close this window.";
                    client.getOutputStream().write(response.getBytes(StandardCharsets.UTF_8));

                    final Matcher m = Pattern.compile("code=([^&\\s]+)").matcher(get);
                    if (m.find()) {
                        final MsaCode msaCode = new MsaCode(m.group(1), localWebServer.getApplicationDetails());
                        MinecraftAuth.LOGGER.info("Got MSA Code");
                        return msaCode;
                    }
                    throw new RuntimeException("Could not find MSA code");
                }
            } catch (SocketTimeoutException e) {
                throw new RuntimeException("Failed to get MSA Code. Login timed out");
            }
        }
    }

}
