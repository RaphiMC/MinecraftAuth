package net.raphimc.mcauth.step.msa;

import net.raphimc.mcauth.MinecraftAuth;
import net.raphimc.mcauth.step.AbstractStep;
import org.apache.http.client.HttpClient;

import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StepExternalBrowserMsaCode extends MsaCodeStep<StepExternalBrowser.ExternalBrowser> {

    public static final String AUTHORIZE_URL = "https://login.live.com/oauth20_authorize.srf";

    private final int timeout;

    public StepExternalBrowserMsaCode(AbstractStep<?, StepExternalBrowser.ExternalBrowser> prevStep, String clientId, String scope, final int timeout) {
        super(prevStep, clientId, scope);

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
                        final MsaCode result = new MsaCode(m.group(1), this.clientId, this.scope, prevResult.redirectUri());
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
