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
package net.raphimc.minecraftauth.step.msa;

import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.responsehandler.exception.MsaResponseException;
import net.raphimc.minecraftauth.step.AbstractStep;
import org.apache.http.NameValuePair;
import org.apache.http.RequestLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicLineParser;

import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Collectors;

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
                    try {
                        final RequestLine requestLine = BasicLineParser.parseRequestLine(scanner.nextLine(), BasicLineParser.INSTANCE);

                        final Map<String, String> parameters = URLEncodedUtils.parse(new URI(requestLine.getUri()), StandardCharsets.UTF_8).stream().collect(Collectors.toMap(NameValuePair::getName, NameValuePair::getValue));
                        if (parameters.containsKey("error") && parameters.containsKey("error_description")) {
                            throw new MsaResponseException(500, parameters.get("error"), parameters.get("error_description"));
                        }
                        if (!parameters.containsKey("code")) {
                            throw new IllegalStateException("Could not extract MSA Code from response url");
                        }

                        final String response = "HTTP/1.1 200 OK\r\nConnection: Close\r\n\r\nYou have been logged in! You can close this window.";
                        client.getOutputStream().write(response.getBytes(StandardCharsets.UTF_8));

                        final MsaCode msaCode = new MsaCode(parameters.get("code"), localWebServer.getApplicationDetails());
                        MinecraftAuth.LOGGER.info("Got MSA Code");
                        return msaCode;
                    } catch (Throwable e) {
                        final String response = "HTTP/1.1 500 Internal Server Error\r\nConnection: Close\r\n\r\nLogin failed. Error message: " + e.getMessage();
                        client.getOutputStream().write(response.getBytes(StandardCharsets.UTF_8));
                        throw e;
                    }
                }
            } catch (SocketTimeoutException e) {
                throw new RuntimeException("Failed to get MSA Code. Login timed out");
            }
        }
    }

}
