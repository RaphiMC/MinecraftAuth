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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.SneakyThrows;
import net.lenni0451.commons.httpclient.HttpClient;
import net.lenni0451.commons.httpclient.constants.ContentTypes;
import net.lenni0451.commons.httpclient.constants.HttpHeaders;
import net.lenni0451.commons.httpclient.handler.ThrowingResponseHandler;
import net.lenni0451.commons.httpclient.requests.HttpRequest;
import net.lenni0451.commons.httpclient.requests.impl.GetRequest;
import net.raphimc.minecraftauth.responsehandler.RealmsResponseHandler;
import net.raphimc.minecraftauth.service.realms.model.RealmsWorld;

import java.net.CookieManager;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public abstract class AbstractRealmsService {

    public static final String CLIENT_COMPATIBLE_URL = "https://$HOST/mco/client/compatible";
    public static final String WORLDS_URL = "https://$HOST/worlds";

    protected final String host;
    protected final HttpClient httpClient;
    protected final CookieManager cookieManager;

    public AbstractRealmsService(final String host, final HttpClient httpClient, final CookieManager cookieManager) {
        this.host = host;
        this.httpClient = httpClient;
        this.cookieManager = cookieManager;
    }

    public CompletableFuture<Boolean> isAvailable() {
        return CompletableFuture.supplyAsync(new Supplier<Boolean>() {
            @Override
            @SneakyThrows
            public Boolean get() {
                final GetRequest getRequest = new GetRequest(CLIENT_COMPATIBLE_URL.replace("$HOST", AbstractRealmsService.this.host));
                getRequest.setCookieManager(AbstractRealmsService.this.cookieManager);
                getRequest.setHeader(HttpHeaders.ACCEPT, ContentTypes.TEXT_PLAIN.getMimeType());
                AbstractRealmsService.this.addRequestHeaders(getRequest);
                final String response = AbstractRealmsService.this.httpClient.execute(getRequest, new ThrowingResponseHandler()).getContentAsString();
                return response.equals("COMPATIBLE");
            }
        });
    }

    public CompletableFuture<List<RealmsWorld>> getWorlds() {
        return CompletableFuture.supplyAsync(new Supplier<List<RealmsWorld>>() {
            @Override
            @SneakyThrows
            public List<RealmsWorld> get() {
                final GetRequest getRequest = new GetRequest(WORLDS_URL.replace("$HOST", AbstractRealmsService.this.host));
                getRequest.setCookieManager(AbstractRealmsService.this.cookieManager);
                AbstractRealmsService.this.addRequestHeaders(getRequest);
                final JsonObject obj = AbstractRealmsService.this.httpClient.execute(getRequest, new RealmsResponseHandler());

                final List<RealmsWorld> realmsWorlds = new ArrayList<>();
                for (JsonElement server : obj.getAsJsonArray("servers")) {
                    realmsWorlds.add(RealmsWorld.fromJson(server.getAsJsonObject()));
                }
                return realmsWorlds;
            }
        });
    }

    public abstract CompletableFuture<String> joinWorld(final RealmsWorld realmsWorld);

    protected abstract void addRequestHeaders(final HttpRequest httpRequest);

}
