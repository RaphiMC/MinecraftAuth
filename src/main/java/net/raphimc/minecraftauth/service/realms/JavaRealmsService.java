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
import net.lenni0451.commons.httpclient.requests.HttpRequest;
import net.lenni0451.commons.httpclient.requests.impl.GetRequest;
import net.lenni0451.commons.httpclient.requests.impl.PostRequest;
import net.raphimc.minecraftauth.responsehandler.RealmsResponseHandler;
import net.raphimc.minecraftauth.service.realms.model.RealmsWorld;
import net.raphimc.minecraftauth.step.java.StepMCProfile;

import java.net.CookieManager;
import java.net.HttpCookie;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class JavaRealmsService extends AbstractRealmsService {

    public static final String JOIN_WORLD_URL = "https://pc.realms.minecraft.net/worlds/v1/$ID/join/pc";
    public static final String AGREE_TOS_URL = "https://pc.realms.minecraft.net/mco/tos/agreed";

    private final boolean isSnapshot;

    public JavaRealmsService(final HttpClient httpClient, final String clientVersion, final StepMCProfile.MCProfile mcProfile) {
        super("pc.realms.minecraft.net", httpClient, new CookieManager());
        this.isSnapshot = !clientVersion.matches("\\d+\\.\\d+(\\.\\d+)?");

        this.cookieManager.getCookieStore().add(null, this.createCookie("sid", "token:" + mcProfile.getMcToken().getAccessToken() + ':' + mcProfile.getId().toString().replace("-", "")));
        this.cookieManager.getCookieStore().add(null, this.createCookie("user", mcProfile.getName()));
        this.cookieManager.getCookieStore().add(null, this.createCookie("version", clientVersion));
    }

    @Override
    public CompletableFuture<String> joinWorld(final RealmsWorld realmsWorld) {
        return CompletableFuture.supplyAsync(new Supplier<String>() {
            @Override
            @SneakyThrows
            public String get() {
                final GetRequest getRequest = new GetRequest(JOIN_WORLD_URL.replace("$ID", String.valueOf(realmsWorld.getId())));
                getRequest.setCookieManager(JavaRealmsService.this.cookieManager);
                JavaRealmsService.this.addRequestHeaders(getRequest);
                final JsonObject obj = JavaRealmsService.this.httpClient.execute(getRequest, new RealmsResponseHandler());
                return obj.get("address").getAsString();
            }
        });
    }

    public CompletableFuture<Void> acceptTos() {
        return CompletableFuture.runAsync(new Runnable() {
            @Override
            @SneakyThrows
            public void run() {
                final PostRequest postRequest = new PostRequest(AGREE_TOS_URL);
                postRequest.setCookieManager(JavaRealmsService.this.cookieManager);
                JavaRealmsService.this.addRequestHeaders(postRequest);
                JavaRealmsService.this.httpClient.execute(postRequest, new RealmsResponseHandler());
            }
        });
    }

    @Override
    protected void addRequestHeaders(final HttpRequest httpRequest) {
        httpRequest.setHeader("Is-Prerelease", String.valueOf(this.isSnapshot));
    }

    private HttpCookie createCookie(final String name, final String value) {
        final HttpCookie cookie = new HttpCookie(name, value);
        cookie.setDomain(this.host);
        cookie.setPath("/");
        return cookie;
    }

}
