/*
 * This file is part of MinecraftAuth - https://github.com/RaphiMC/MinecraftAuth
 * Copyright (C) 2023 RK_01/RaphiMC and contributors
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
import com.google.gson.JsonParser;
import lombok.SneakyThrows;
import net.raphimc.minecraftauth.responsehandler.RealmsResponseHandler;
import net.raphimc.minecraftauth.responsehandler.exception.RetryException;
import net.raphimc.minecraftauth.service.realms.model.RealmsWorld;
import net.raphimc.minecraftauth.step.java.StepMCProfile;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.AbstractHttpMessage;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class JavaRealmsService extends AbstractRealmsService {

    public static final String JOIN_WORLD_URL = "https://pc.realms.minecraft.net/worlds/v1/$ID/join/pc";
    public static final String AGREE_TOS_URL = "https://pc.realms.minecraft.net/mco/tos/agreed";

    private final boolean isSnapshot;

    public JavaRealmsService(final HttpClient httpClient, final String clientVersion, final StepMCProfile.MCProfile mcProfile) {
        super("pc.realms.minecraft.net", httpClient, HttpClientContext.create());
        this.isSnapshot = !clientVersion.matches("\\d+\\.\\d+(\\.\\d+)?");

        final BasicCookieStore cookieStore = new BasicCookieStore();
        cookieStore.addCookie(this.createCookie("sid", "token:" + mcProfile.getMcToken().getAccessToken() + ':' + mcProfile.getId().toString().replace("-", "")));
        cookieStore.addCookie(this.createCookie("user", mcProfile.getName()));
        cookieStore.addCookie(this.createCookie("version", clientVersion));
        this.context.setCookieStore(cookieStore);
    }

    @Override
    public CompletableFuture<String> joinWorld(final RealmsWorld realmsWorld) {
        return CompletableFuture.supplyAsync(new Supplier<String>() {
            @Override
            @SneakyThrows
            public String get() {
                final HttpGet httpGet = new HttpGet(JOIN_WORLD_URL.replace("$ID", String.valueOf(realmsWorld.getId())));
                JavaRealmsService.this.addRequestHeaders(httpGet);
                while (true) {
                    try {
                        final String response = JavaRealmsService.this.httpClient.execute(httpGet, new RealmsResponseHandler(), JavaRealmsService.this.context);
                        final JsonObject obj = JsonParser.parseString(response).getAsJsonObject();
                        return obj.get("address").getAsString();
                    } catch (RetryException e) {
                        Thread.sleep(e.getRetryAfterSeconds() * 1000L);
                    }
                }
            }
        });
    }

    public CompletableFuture<Void> acceptTos() {
        return CompletableFuture.runAsync(new Runnable() {
            @Override
            @SneakyThrows
            public void run() {
                final HttpPost httpPost = new HttpPost(AGREE_TOS_URL);
                JavaRealmsService.this.addRequestHeaders(httpPost);
                JavaRealmsService.this.httpClient.execute(httpPost, new RealmsResponseHandler(), JavaRealmsService.this.context);
            }
        });
    }

    @Override
    protected void addRequestHeaders(final AbstractHttpMessage httpMessage) {
        httpMessage.addHeader("Is-Prerelease", String.valueOf(this.isSnapshot));
    }

    private Cookie createCookie(final String name, final String value) {
        final BasicClientCookie cookie = new BasicClientCookie(name, value);
        cookie.setDomain(this.host);
        return cookie;
    }

}
