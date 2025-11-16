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
import net.lenni0451.commons.httpclient.requests.HttpRequest;
import net.raphimc.minecraftauth.extra.realms.model.RealmsJoinInformation;
import net.raphimc.minecraftauth.extra.realms.model.RealmsServer;
import net.raphimc.minecraftauth.extra.realms.request.JavaRealmsTosAgreedRequest;
import net.raphimc.minecraftauth.extra.realms.request.JavaRealmsWorldJoinRequest;
import net.raphimc.minecraftauth.extra.realms.service.RealmsService;
import net.raphimc.minecraftauth.java.model.MinecraftProfile;
import net.raphimc.minecraftauth.java.model.MinecraftToken;
import net.raphimc.minecraftauth.util.UuidUtil;
import net.raphimc.minecraftauth.util.holder.Holder;

import java.io.IOException;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.util.concurrent.CompletableFuture;

public class JavaRealmsService extends RealmsService {

    private final Holder<MinecraftToken> token;
    private final Holder<MinecraftProfile> profile;
    private final String gameVersion;

    public JavaRealmsService(final HttpClient httpClient, final String gameVersion, final Holder<MinecraftToken> token, final Holder<MinecraftProfile> profile) {
        super(httpClient, "pc.realms.minecraft.net");
        this.token = token;
        this.profile = profile;
        this.gameVersion = gameVersion;
    }

    @Override
    public RealmsJoinInformation joinWorld(final RealmsServer server) throws IOException {
        return this.httpClient.executeAndHandle(this.authorizeRequest(new JavaRealmsWorldJoinRequest(server)));
    }

    public void acceptTos() throws IOException {
        this.httpClient.executeAndHandle(this.authorizeRequest(new JavaRealmsTosAgreedRequest()));
    }

    @SneakyThrows
    public void acceptTosUnchecked() {
        this.acceptTos();
    }

    public CompletableFuture<Void> acceptTosAsync() {
        return CompletableFuture.runAsync(this::acceptTosUnchecked);
    }

    @Override
    protected <T extends HttpRequest> T authorizeRequest(final T httpRequest) throws IOException {
        final CookieManager cookieManager = new CookieManager();
        final MinecraftProfile profile = this.profile.getUpToDate();
        cookieManager.getCookieStore().add(null, this.createCookie("sid", "token:" + this.token.getUpToDate().getToken() + ':' + UuidUtil.toUndashedString(profile.getId())));
        cookieManager.getCookieStore().add(null, this.createCookie("user", profile.getName()));
        cookieManager.getCookieStore().add(null, this.createCookie("version", this.gameVersion));

        httpRequest.setCookieManager(cookieManager);
        httpRequest.setHeader("Is-Prerelease", String.valueOf(!this.gameVersion.matches("\\d+\\.\\d+(\\.\\d+)?")));
        return httpRequest;
    }

    private HttpCookie createCookie(final String name, final String value) {
        final HttpCookie cookie = new HttpCookie(name, value);
        cookie.setDomain(this.host);
        cookie.setPath("/");
        return cookie;
    }

}
