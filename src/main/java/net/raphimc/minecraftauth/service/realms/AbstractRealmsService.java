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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.SneakyThrows;
import net.raphimc.minecraftauth.responsehandler.RealmsResponseHandler;
import net.raphimc.minecraftauth.service.realms.model.RealmsWorld;
import net.raphimc.minecraftauth.util.JsonUtil;
import org.apache.http.HttpHeaders;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.message.AbstractHttpMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public abstract class AbstractRealmsService {

    public static final String CLIENT_COMPATIBLE_URL = "https://$HOST/mco/client/compatible";
    public static final String WORLDS_URL = "https://$HOST/worlds";

    protected final String host;
    protected final HttpClient httpClient;
    protected final HttpClientContext context;

    public AbstractRealmsService(final String host, final HttpClient httpClient, final HttpClientContext context) {
        this.host = host;
        this.httpClient = httpClient;
        this.context = context;
    }

    public CompletableFuture<Boolean> isAvailable() {
        return CompletableFuture.supplyAsync(new Supplier<Boolean>() {
            @Override
            @SneakyThrows
            public Boolean get() {
                final HttpGet httpGet = new HttpGet(CLIENT_COMPATIBLE_URL.replace("$HOST", AbstractRealmsService.this.host));
                httpGet.addHeader(HttpHeaders.ACCEPT, ContentType.TEXT_PLAIN.getMimeType());
                AbstractRealmsService.this.addRequestHeaders(httpGet);
                final String response = AbstractRealmsService.this.httpClient.execute(httpGet, new BasicResponseHandler(), AbstractRealmsService.this.context);
                return response.equals("COMPATIBLE");
            }
        });
    }

    public CompletableFuture<List<RealmsWorld>> getWorlds() {
        return CompletableFuture.supplyAsync(new Supplier<List<RealmsWorld>>() {
            @Override
            @SneakyThrows
            public List<RealmsWorld> get() {
                final HttpGet httpGet = new HttpGet(WORLDS_URL.replace("$HOST", AbstractRealmsService.this.host));
                AbstractRealmsService.this.addRequestHeaders(httpGet);
                final String response = AbstractRealmsService.this.httpClient.execute(httpGet, new RealmsResponseHandler(), AbstractRealmsService.this.context);
                final JsonObject obj = JsonUtil.parseString(response).getAsJsonObject();

                final List<RealmsWorld> realmsWorlds = new ArrayList<>();
                for (JsonElement server : obj.getAsJsonArray("servers")) {
                    realmsWorlds.add(RealmsWorld.fromJson(server.getAsJsonObject()));
                }
                return realmsWorlds;
            }
        });
    }

    public abstract CompletableFuture<String> joinWorld(final RealmsWorld realmsWorld);

    protected abstract void addRequestHeaders(final AbstractHttpMessage httpMessage);

}
