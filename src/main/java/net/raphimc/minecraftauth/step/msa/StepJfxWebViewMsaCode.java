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
import net.lenni0451.commons.httpclient.HttpClient;
import net.lenni0451.commons.httpclient.HttpResponse;
import net.lenni0451.commons.httpclient.constants.Headers;
import net.lenni0451.commons.httpclient.utils.URLWrapper;
import net.raphimc.minecraftauth.responsehandler.exception.MsaRequestException;
import net.raphimc.minecraftauth.step.AbstractStep;
import net.raphimc.minecraftauth.util.logging.ILogger;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;

public class StepJfxWebViewMsaCode extends MsaCodeStep<StepJfxWebViewMsaCode.JavaFxWebView> {

    private final int timeout;

    public StepJfxWebViewMsaCode(final ApplicationDetails applicationDetails, final int timeout) {
        super(applicationDetails);

        this.timeout = timeout;
    }

    @Override
    @SneakyThrows
    public MsaCode execute(final ILogger logger, final HttpClient httpClient, final JavaFxWebView javaFxWebViewCallback) throws Exception {
        logger.info("Opening JavaFX WebView window for MSA login...");

        final JFXPanel jfxPanel = new JFXPanel();
        final URL authenticationUrl = new URLWrapper(this.applicationDetails.getOAuthEnvironment().getAuthorizeUrl()).wrapQuery().addQueries(this.applicationDetails.getOAuthParameters()).apply().toURL();
        final CompletableFuture<MsaCode> msaCodeFuture = new CompletableFuture<>();

        final JFrame window = new JFrame("MinecraftAuth - Microsoft Login");
        window.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
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

        Platform.runLater(() -> {
            final WebView webView = new WebView();
            webView.setContextMenuEnabled(false);
            httpClient.getFirstHeader(Headers.USER_AGENT).ifPresent(s -> webView.getEngine().setUserAgent(s));
            webView.getEngine().load(authenticationUrl.toString());
            webView.getEngine().locationProperty().addListener((observable, oldValue, newValue) -> {
                try {
                    if (newValue.startsWith(this.applicationDetails.getRedirectUri())) {
                        final Map<String, String> parameters = new URLWrapper(newValue).wrapQuery().getQueries();
                        if (parameters.containsKey("error") && parameters.containsKey("error_description")) {
                            final HttpResponse fakeResponse = new HttpResponse(null, 500, new byte[0], Collections.emptyMap());
                            throw new MsaRequestException(fakeResponse, parameters.get("error"), parameters.get("error_description"));
                        }
                        if (!parameters.containsKey("code")) {
                            throw new IllegalStateException("Could not extract MSA Code from response url");
                        }

                        msaCodeFuture.complete(new MsaCode(parameters.get("code")));
                    }
                } catch (Throwable e) {
                    msaCodeFuture.completeExceptionally(e);
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
            window.dispose();
            logger.info("Got MSA Code");
            return msaCode;
        } catch (TimeoutException e) {
            window.dispose();
            throw new TimeoutException("MSA login timed out");
        } catch (ExecutionException e) {
            window.dispose();
            if (e.getCause() != null) {
                throw e.getCause();
            } else {
                throw e;
            }
        }
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
