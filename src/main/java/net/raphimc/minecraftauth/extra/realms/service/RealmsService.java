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
package net.raphimc.minecraftauth.extra.realms.service;

import lombok.Getter;
import lombok.SneakyThrows;
import net.lenni0451.commons.httpclient.HttpClient;
import net.lenni0451.commons.httpclient.requests.HttpRequest;
import net.raphimc.minecraftauth.extra.realms.model.RealmsJoinInformation;
import net.raphimc.minecraftauth.extra.realms.model.RealmsServer;
import net.raphimc.minecraftauth.extra.realms.request.RealmsClientCompatibleRequest;
import net.raphimc.minecraftauth.extra.realms.request.RealmsWorldsRequest;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Getter
public abstract class RealmsService {

    protected final HttpClient httpClient;
    protected final String host;

    public RealmsService(final HttpClient httpClient, final String host) {
        this.httpClient = httpClient;
        this.host = host;
    }

    public boolean isCompatible() throws IOException {
        final String response = this.httpClient.executeAndHandle(this.authorizeRequest(new RealmsClientCompatibleRequest(this.host)));
        return response.equals("COMPATIBLE");
    }

    @SneakyThrows
    public boolean isCompatibleUnchecked() {
        return this.isCompatible();
    }

    public CompletableFuture<Boolean> isCompatibleAsync() {
        return CompletableFuture.supplyAsync(this::isCompatibleUnchecked);
    }

    public List<RealmsServer> getWorlds() throws IOException {
        return this.httpClient.executeAndHandle(this.authorizeRequest(new RealmsWorldsRequest(this.host)));
    }

    @SneakyThrows
    public List<RealmsServer> getWorldsUnchecked() {
        return this.getWorlds();
    }

    public CompletableFuture<List<RealmsServer>> getWorldsAsync() {
        return CompletableFuture.supplyAsync(this::getWorldsUnchecked);
    }

    public abstract RealmsJoinInformation joinWorld(final RealmsServer server) throws IOException;

    @SneakyThrows
    public RealmsJoinInformation joinWorldUnchecked(final RealmsServer server) {
        return this.joinWorld(server);
    }

    public CompletableFuture<RealmsJoinInformation> joinWorldAsync(final RealmsServer server) {
        return CompletableFuture.supplyAsync(() -> this.joinWorldUnchecked(server));
    }

    protected abstract <T extends HttpRequest> T authorizeRequest(final T httpRequest) throws IOException;

}
