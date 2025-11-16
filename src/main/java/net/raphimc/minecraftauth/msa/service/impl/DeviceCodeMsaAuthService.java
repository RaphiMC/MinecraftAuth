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

import net.lenni0451.commons.httpclient.HttpClient;
import net.lenni0451.commons.httpclient.constants.StatusCodes;
import net.raphimc.minecraftauth.msa.exception.MsaRequestException;
import net.raphimc.minecraftauth.msa.model.MsaApplicationConfig;
import net.raphimc.minecraftauth.msa.model.MsaDeviceCode;
import net.raphimc.minecraftauth.msa.model.MsaToken;
import net.raphimc.minecraftauth.msa.request.MsaDeviceCodeRequest;
import net.raphimc.minecraftauth.msa.request.MsaDeviceCodeTokenRequest;
import net.raphimc.minecraftauth.msa.service.MsaAuthService;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

public class DeviceCodeMsaAuthService extends MsaAuthService {

    private final Consumer<MsaDeviceCode> callback;
    private final int timeoutMs;

    public DeviceCodeMsaAuthService(final HttpClient httpClient, final MsaApplicationConfig applicationConfig, final Consumer<MsaDeviceCode> callback) {
        this(httpClient, applicationConfig, callback, 300_000);
    }

    public DeviceCodeMsaAuthService(final HttpClient httpClient, final MsaApplicationConfig applicationConfig, final Consumer<MsaDeviceCode> callback, final int timeoutMs) {
        super(httpClient, applicationConfig);
        this.callback = callback;
        this.timeoutMs = timeoutMs;
    }

    @Override
    public MsaToken acquireToken() throws IOException, InterruptedException, TimeoutException {
        final MsaDeviceCode deviceCode = this.requestDeviceCode();
        this.callback.accept(deviceCode);
        return this.getToken(deviceCode);
    }

    public MsaDeviceCode requestDeviceCode() throws IOException {
        return this.httpClient.executeAndHandle(new MsaDeviceCodeRequest(this.applicationConfig));
    }

    public MsaToken getToken(final MsaDeviceCode deviceCode) throws IOException, InterruptedException, TimeoutException {
        final long start = System.currentTimeMillis();
        while (!deviceCode.isExpired() && System.currentTimeMillis() - start <= this.timeoutMs) {
            try {
                return this.httpClient.executeAndHandle(new MsaDeviceCodeTokenRequest(this.applicationConfig, deviceCode));
            } catch (MsaRequestException e) {
                if (e.getResponse().getStatusCode() == StatusCodes.BAD_REQUEST && e.getError().equals("authorization_pending")) {
                    Thread.sleep(deviceCode.getIntervalMs());
                } else {
                    throw e;
                }
            }
        }
        throw new TimeoutException("Login timed out");
    }

}
