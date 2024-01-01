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
package net.raphimc.minecraftauth.service.realms;

import com.google.gson.JsonObject;
import lombok.SneakyThrows;
import net.raphimc.minecraftauth.responsehandler.RealmsResponseHandler;
import net.raphimc.minecraftauth.responsehandler.exception.RetryException;
import net.raphimc.minecraftauth.service.realms.model.RealmsWorld;
import net.raphimc.minecraftauth.step.xbl.StepXblXstsToken;
import net.raphimc.minecraftauth.util.JsonUtil;
import org.apache.http.HttpHeaders;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.AbstractHttpMessage;

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
                final HttpGet httpGet = new HttpGet(JOIN_WORLD_URL.replace("$ID", String.valueOf(realmsWorld.getId())));
                BedrockRealmsService.this.addRequestHeaders(httpGet);
                while (true) {
                    try {
                        final String response = BedrockRealmsService.this.httpClient.execute(httpGet, new RealmsResponseHandler());
                        final JsonObject obj = JsonUtil.parseString(response).getAsJsonObject();
                        return obj.get("address").getAsString();
                    } catch (RetryException e) {
                        Thread.sleep(e.getRetryAfterSeconds() * 1000L);
                    }
                }
            }
        });
    }

    public CompletableFuture<RealmsWorld> acceptInvite(final String realmCode) {
        return CompletableFuture.supplyAsync(new Supplier<RealmsWorld>() {
            @Override
            @SneakyThrows
            public RealmsWorld get() {
                final HttpPost httpPost = new HttpPost(ACCEPT_INVITE_URL.replace("$CODE", realmCode));
                BedrockRealmsService.this.addRequestHeaders(httpPost);
                final String response = BedrockRealmsService.this.httpClient.execute(httpPost, new RealmsResponseHandler());
                final JsonObject obj = JsonUtil.parseString(response).getAsJsonObject();
                return RealmsWorld.fromJson(obj);
            }
        });
    }

    public CompletableFuture<Void> leaveInvitedRealm(final RealmsWorld realmsWorld) {
        return CompletableFuture.runAsync(new Runnable() {
            @Override
            @SneakyThrows
            public void run() {
                final HttpDelete httpDelete = new HttpDelete(DELETE_INVITE_URL.replace("$ID", String.valueOf(realmsWorld.getId())));
                BedrockRealmsService.this.addRequestHeaders(httpDelete);
                final String response = BedrockRealmsService.this.httpClient.execute(httpDelete, new RealmsResponseHandler());
                if (response != null) {
                    throw new IllegalStateException("Failed to delete invite: " + response);
                }
            }
        });
    }

    @Override
    protected void addRequestHeaders(final AbstractHttpMessage httpMessage) {
        httpMessage.addHeader(HttpHeaders.AUTHORIZATION, "XBL3.0 x=" + this.realmsXsts.getServiceToken());
        httpMessage.addHeader("Client-Version", this.clientVersion);
    }

}
