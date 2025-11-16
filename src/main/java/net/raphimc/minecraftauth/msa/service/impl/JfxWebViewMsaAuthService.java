/*
 * This file is part of MinecraftAuth - https://github.com/RaphiMC/MinecraftAuth
 * Copyright (C) 2022-2025 RK_01/RaphiMC and contributors
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
package net.raphimc.minecraftauth.msa.service.impl;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.web.WebView;
import net.lenni0451.commons.httpclient.HttpClient;
import net.lenni0451.commons.httpclient.HttpResponse;
import net.lenni0451.commons.httpclient.constants.HttpHeaders;
import net.lenni0451.commons.httpclient.constants.StatusCodes;
import net.lenni0451.commons.httpclient.utils.URLWrapper;
import net.raphimc.minecraftauth.msa.exception.MsaRequestException;
import net.raphimc.minecraftauth.msa.model.MsaApplicationConfig;
import net.raphimc.minecraftauth.msa.model.MsaToken;
import net.raphimc.minecraftauth.msa.request.MsaAuthCodeTokenRequest;
import net.raphimc.minecraftauth.msa.service.MsaAuthService;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

public class JfxWebViewMsaAuthService extends MsaAuthService {

    private final Consumer<JFrame> openCallback;
    private final Consumer<JFrame> closeCallback;
    private final int timeoutMs;

    public JfxWebViewMsaAuthService(final HttpClient httpClient, final MsaApplicationConfig applicationConfig) {
        this(httpClient, applicationConfig, window -> window.setVisible(true), JFrame::dispose);
    }

    public JfxWebViewMsaAuthService(final HttpClient httpClient, final MsaApplicationConfig applicationConfig, final Consumer<JFrame> openCallback, final Consumer<JFrame> closeCallback) {
        this(httpClient, applicationConfig, openCallback, closeCallback, 300_000);
    }

    public JfxWebViewMsaAuthService(final HttpClient httpClient, final MsaApplicationConfig applicationConfig, final Consumer<JFrame> openCallback, final Consumer<JFrame> closeCallback, final int timeoutMs) {
        super(httpClient, applicationConfig);
        this.openCallback = openCallback;
        this.closeCallback = closeCallback;
        this.timeoutMs = timeoutMs;
    }

    @Override
    public MsaToken acquireToken() throws IOException, InterruptedException, TimeoutException {
        final URL authenticationUrl = URLWrapper.ofURL(this.applicationConfig.getEnvironment().getAuthorizeUrl()).wrapQueryParameters().addParameters(this.applicationConfig.getAuthCodeParameters()).apply().toURL();
        final CompletableFuture<String> authCodeFuture = new CompletableFuture<>();

        final JFXPanel jfxPanel = new JFXPanel();
        final JFrame window = new JFrame("MinecraftAuth - Microsoft Login");
        window.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        window.setSize(800, 600);
        window.setLocationRelativeTo(null);
        window.setResizable(false);
        window.setContentPane(jfxPanel);
        window.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(final WindowEvent e) {
                if (!authCodeFuture.isDone()) {
                    authCodeFuture.completeExceptionally(new UserClosedWindowException());
                }
            }
        });

        Platform.runLater(() -> {
            final WebView webView = new WebView();
            webView.setContextMenuEnabled(false);
            httpClient.getFirstHeader(HttpHeaders.USER_AGENT).ifPresent(webView.getEngine()::setUserAgent);
            webView.getEngine().load(authenticationUrl.toString());
            webView.getEngine().locationProperty().addListener((observable, oldValue, newValue) -> {
                try {
                    final URLWrapper.QueryParametersWrapper parameters = URLWrapper.ofURI(newValue).wrapQueryParameters();
                    final Optional<String> error = parameters.getFirstValue("error");
                    final Optional<String> errorDescription = parameters.getFirstValue("error_description");
                    if (error.isPresent() && errorDescription.isPresent()) {
                        final HttpResponse fakeResponse = new HttpResponse(null, StatusCodes.INTERNAL_SERVER_ERROR, new byte[0], Collections.emptyMap());
                        throw new MsaRequestException(fakeResponse, error.get(), errorDescription.get());
                    }
                    parameters.getFirstValue("code").ifPresent(authCodeFuture::complete);
                } catch (Throwable e) {
                    authCodeFuture.completeExceptionally(e);
                }
            });
            jfxPanel.setScene(new Scene(webView, window.getWidth(), window.getHeight()));
            this.openCallback.accept(window);
        });

        try {
            final String authCode = authCodeFuture.get(this.timeoutMs, TimeUnit.MILLISECONDS);
            return this.httpClient.executeAndHandle(new MsaAuthCodeTokenRequest(this.applicationConfig, authCode));
        } catch (TimeoutException e) {
            throw new TimeoutException("Login timed out");
        } catch (ExecutionException e) {
            if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            } else {
                throw new RuntimeException(e);
            }
        } finally {
            this.closeCallback.accept(window);
        }
    }

    public static class UserClosedWindowException extends RuntimeException {

        public UserClosedWindowException() {
            super("User closed the login window");
        }

    }

}
