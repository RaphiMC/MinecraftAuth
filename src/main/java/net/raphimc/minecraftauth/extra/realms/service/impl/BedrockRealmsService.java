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
package net.raphimc.minecraftauth.extra.realms.service.impl;

import lombok.SneakyThrows;
import net.lenni0451.commons.httpclient.HttpClient;
import net.lenni0451.commons.httpclient.constants.HttpHeaders;
import net.lenni0451.commons.httpclient.requests.HttpRequest;
import net.raphimc.minecraftauth.extra.realms.model.RealmsJoinInformation;
import net.raphimc.minecraftauth.extra.realms.model.RealmsServer;
import net.raphimc.minecraftauth.extra.realms.request.BedrockRealmsInviteDeleteRequest;
import net.raphimc.minecraftauth.extra.realms.request.BedrockRealmsInviteLinkAcceptRequest;
import net.raphimc.minecraftauth.extra.realms.request.BedrockRealmsWorldJoinRequest;
import net.raphimc.minecraftauth.extra.realms.service.RealmsService;
import net.raphimc.minecraftauth.util.holder.Holder;
import net.raphimc.minecraftauth.xbl.model.XblXstsToken;

import java.util.concurrent.CompletableFuture;

public class BedrockRealmsService extends RealmsService {

    private final Holder<XblXstsToken> xstsToken;
    private final String gameVersion;

    public BedrockRealmsService(final HttpClient httpClient, final String gameVersion, final Holder<XblXstsToken> xstsToken) {
        super(httpClient, "pocket.realms.minecraft.net");
        this.xstsToken = xstsToken;
        this.gameVersion = gameVersion;
    }

    @SneakyThrows
    public RealmsServer acceptInvite(final String code) {
        return this.httpClient.executeAndHandle(this.authorizeRequest(new BedrockRealmsInviteLinkAcceptRequest(code)));
    }

    public CompletableFuture<RealmsServer> acceptInviteAsync(final String code) {
        return CompletableFuture.supplyAsync(() -> this.acceptInvite(code));
    }

    @SneakyThrows
    public void leaveInvitedRealm(final RealmsServer server) {
        this.httpClient.executeAndHandle(this.authorizeRequest(new BedrockRealmsInviteDeleteRequest(server)));
    }

    public CompletableFuture<Void> leaveInvitedRealmAsync(final RealmsServer server) {
        return CompletableFuture.runAsync(() -> this.leaveInvitedRealm(server));
    }

    @Override
    @SneakyThrows
    public RealmsJoinInformation joinWorld(final RealmsServer server) {
        return this.httpClient.executeAndHandle(this.authorizeRequest(new BedrockRealmsWorldJoinRequest(server)));
    }

    @Override
    protected <T extends HttpRequest> T authorizeRequest(final T httpRequest) {
        httpRequest.setHeader(HttpHeaders.AUTHORIZATION, this.xstsToken.getUpToDate().getAuthorizationHeader());
        httpRequest.setHeader("Client-Version", this.gameVersion);
        return httpRequest;
    }

}
