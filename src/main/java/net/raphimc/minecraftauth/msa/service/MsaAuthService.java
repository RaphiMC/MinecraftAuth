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
package net.raphimc.minecraftauth.msa.service;

import lombok.Getter;
import lombok.SneakyThrows;
import net.lenni0451.commons.httpclient.HttpClient;
import net.raphimc.minecraftauth.msa.model.MsaApplicationConfig;
import net.raphimc.minecraftauth.msa.model.MsaToken;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

@Getter
public abstract class MsaAuthService {

    protected final HttpClient httpClient;
    protected final MsaApplicationConfig applicationConfig;

    public MsaAuthService(final HttpClient httpClient, final MsaApplicationConfig applicationConfig) {
        this.httpClient = httpClient;
        this.applicationConfig = applicationConfig;
    }

    public abstract MsaToken acquireToken() throws IOException, InterruptedException, TimeoutException;

    @SneakyThrows
    public MsaToken acquireTokenUnchecked() {
        return this.acquireToken();
    }

    public CompletableFuture<MsaToken> acquireTokenAsync() {
        return CompletableFuture.supplyAsync(this::acquireTokenUnchecked);
    }

}
