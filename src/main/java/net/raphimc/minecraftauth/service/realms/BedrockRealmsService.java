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
package net.raphimc.minecraftauth.service.realms;

import com.google.gson.JsonObject;
import lombok.SneakyThrows;
import net.lenni0451.commons.httpclient.HttpClient;
import net.lenni0451.commons.httpclient.constants.HttpHeaders;
import net.lenni0451.commons.httpclient.requests.HttpRequest;
import net.lenni0451.commons.httpclient.requests.impl.DeleteRequest;
import net.lenni0451.commons.httpclient.requests.impl.GetRequest;
import net.lenni0451.commons.httpclient.requests.impl.PostRequest;
import net.raphimc.minecraftauth.responsehandler.RealmsResponseHandler;
import net.raphimc.minecraftauth.service.realms.model.RealmsWorld;
import net.raphimc.minecraftauth.step.xbl.StepXblXstsToken;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class BedrockRealmsService extends AbstractRealmsService {

    public static final String JOIN_WORLD_URL = "https://pocket.realms.minecraft.net/worlds/$ID/join";
    public static final String ACCEPT_INVITE_URL = "https://pocket.realms.minecraft.net/invites/v1/link/accept/$CODE";
    public static final String DELETE_INVITE_URL = "https://pocket.realms.minecraft.net/invites/$ID";

    private final StepXblXstsToken.XblXsts<?> realmsXsts;
    private final String clientVersion;

    public BedrockRealmsService(final HttpClient httpClient, final String clientVersion, final StepXblXstsToken.XblXsts<?> realmsXsts) {
        super("pocket.realms.minecraft.net", httpClient, null);
        this.realmsXsts = realmsXsts;
        this.clientVersion = clientVersion;
    }

    @Override
    public CompletableFuture<String> joinWorld(final RealmsWorld realmsWorld) {
        return CompletableFuture.supplyAsync(new Supplier<String>() {
            @Override
            @SneakyThrows
            public String get() {
                final GetRequest getRequest = new GetRequest(JOIN_WORLD_URL.replace("$ID", String.valueOf(realmsWorld.getId())));
                BedrockRealmsService.this.addRequestHeaders(getRequest);
                final JsonObject obj = BedrockRealmsService.this.httpClient.execute(getRequest, new RealmsResponseHandler());
                return obj.get("address").getAsString();
            }
        });
    }

    public CompletableFuture<RealmsWorld> acceptInvite(final String realmCode) {
        return CompletableFuture.supplyAsync(new Supplier<RealmsWorld>() {
            @Override
            @SneakyThrows
            public RealmsWorld get() {
                final PostRequest postRequest = new PostRequest(ACCEPT_INVITE_URL.replace("$CODE", realmCode));
                BedrockRealmsService.this.addRequestHeaders(postRequest);
                final JsonObject obj = BedrockRealmsService.this.httpClient.execute(postRequest, new RealmsResponseHandler());
                return RealmsWorld.fromJson(obj);
            }
        });
    }

    public CompletableFuture<Void> leaveInvitedRealm(final RealmsWorld realmsWorld) {
        return CompletableFuture.runAsync(new Runnable() {
            @Override
            @SneakyThrows
            public void run() {
                final DeleteRequest deleteRequest = new DeleteRequest(DELETE_INVITE_URL.replace("$ID", String.valueOf(realmsWorld.getId())));
                BedrockRealmsService.this.addRequestHeaders(deleteRequest);
                final JsonObject obj = BedrockRealmsService.this.httpClient.execute(deleteRequest, new RealmsResponseHandler());
                if (obj != null) {
                    throw new IllegalStateException("Failed to delete invite: " + obj);
                }
            }
        });
    }

    @Override
    protected void addRequestHeaders(final HttpRequest httpRequest) {
        httpRequest.setHeader(HttpHeaders.AUTHORIZATION, "XBL3.0 x=" + this.realmsXsts.getServiceToken());
        httpRequest.setHeader("Client-Version", this.clientVersion);
    }

}
