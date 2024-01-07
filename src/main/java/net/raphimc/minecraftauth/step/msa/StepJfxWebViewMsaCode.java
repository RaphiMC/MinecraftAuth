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

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.web.WebView;
import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;
import lombok.Value;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.responsehandler.exception.MsaResponseException;
import net.raphimc.minecraftauth.step.AbstractStep;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.client.utils.URLEncodedUtils;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public class StepJfxWebViewMsaCode extends MsaCodeStep<StepJfxWebViewMsaCode.JavaFxWebView> {

    private final ApplicationDetails applicationDetails;
    private final int timeout;

    public StepJfxWebViewMsaCode(final ApplicationDetails applicationDetails, final int timeout) {
        super(null);

        this.applicationDetails = applicationDetails;
        this.timeout = timeout;
    }

    @Override
    @SneakyThrows
    public MsaCode applyStep(final HttpClient httpClient, final JavaFxWebView javaFxWebViewCallback) throws Exception {
        MinecraftAuth.LOGGER.info("Opening JavaFX WebView window for MSA login...");

        final JFXPanel jfxPanel = new JFXPanel();
        final URI authenticationUrl = this.getAuthenticationUrl();
        final CompletableFuture<MsaCode> msaCodeFuture = new CompletableFuture<>();

        Platform.runLater(() -> {
            final JFrame window = new JFrame("MinecraftAuth - Microsoft Login");
            window.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            window.setSize(800, 600);
            window.setLocationRelativeTo(null);
            window.setResizable(false);
            window.setContentPane(jfxPanel);
            window.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    if (!msaCodeFuture.isDone()) {
                        msaCodeFuture.completeExceptionally(new UserClosedWindowException());
                    }
                }
            });

            final WebView webView = new WebView();
            webView.setContextMenuEnabled(false);
            webView.getEngine().setUserAgent("MinecraftAuth/" + MinecraftAuth.VERSION);
            webView.getEngine().load(authenticationUrl.toString());
            webView.getEngine().locationProperty().addListener((observable, oldValue, newValue) -> {
                try {
                    if (newValue.startsWith(this.applicationDetails.getRedirectUri())) {
                        final Map<String, String> parameters = URLEncodedUtils.parse(new URI(newValue), StandardCharsets.UTF_8).stream().collect(Collectors.toMap(NameValuePair::getName, NameValuePair::getValue));
                        if (parameters.containsKey("error") && parameters.containsKey("error_description")) {
                            throw new MsaResponseException(500, parameters.get("error"), parameters.get("error_description"));
                        }
                        if (!parameters.containsKey("code")) {
                            throw new IllegalStateException("Could not extract MSA Code from response url");
                        }

                        msaCodeFuture.complete(new MsaCode(parameters.get("code"), this.applicationDetails));
                        window.dispose();
                    }
                } catch (Throwable e) {
                    msaCodeFuture.completeExceptionally(e);
                    window.dispose();
                }
            });
            jfxPanel.setScene(new Scene(webView, window.getWidth(), window.getHeight()));

            if (javaFxWebViewCallback == null) {
                window.setVisible(true);
            } else {
                javaFxWebViewCallback.openCallback.accept(window, webView);
            }
        });

        try {
            final MsaCode msaCode = msaCodeFuture.get(this.timeout, TimeUnit.MILLISECONDS);
            MinecraftAuth.LOGGER.info("Got MSA Code");
            return msaCode;
        } catch (ExecutionException e) {
            if (e.getCause() != null) {
                throw e.getCause();
            } else {
                throw e;
            }
        }
    }

    private URI getAuthenticationUrl() throws URISyntaxException {
        return new URIBuilder(this.applicationDetails.getOAuthEnvironment().getAuthorizeUrl())
                .setParameter("client_id", this.applicationDetails.getClientId())
                .setParameter("redirect_uri", this.applicationDetails.getRedirectUri())
                .setParameter("scope", this.applicationDetails.getScope())
                .setParameter("response_type", "code")
                .setParameter("response_mode", "query")
                .build();
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class JavaFxWebView extends AbstractStep.InitialInput {

        BiConsumer<JFrame, WebView> openCallback = (window, webView) -> window.setVisible(true);

    }

    public static class UserClosedWindowException extends Exception {

        public UserClosedWindowException() {
            super("User closed login window");
        }

    }

}
