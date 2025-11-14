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
package net.raphimc.minecraftauth;

import net.lenni0451.commons.httpclient.HttpClient;
import net.lenni0451.commons.httpclient.constants.ContentTypes;
import net.lenni0451.commons.httpclient.constants.HttpHeaders;
import net.lenni0451.commons.httpclient.retry.RetryConfig;

public class MinecraftAuth {

    public static final String VERSION = "${version}";
    public static final String IMPL_VERSION = "${version}+${commit_hash}";

    /**
     * Create a pre-configured {@link HttpClient} for MinecraftAuth using the default user agent.<br>
     * It's not recommended to use this method, because requests could be blocked if too many applications use the default user agent.
     *
     * @return A pre-configured {@link HttpClient}.
     */
    public static HttpClient createHttpClient() {
        return createHttpClient("MinecraftAuth/" + VERSION);
    }

    /**
     * Create a pre-configured {@link HttpClient} for MinecraftAuth.
     *
     * @param userAgent The user agent.
     * @return A pre-configured {@link HttpClient}.
     */
    public static HttpClient createHttpClient(final String userAgent) {
        return new HttpClient()
                .setConnectTimeout(5_000)
                .setReadTimeout(30_000)
                .setCookieManager(null)
                .setFollowRedirects(false)
                .setRetryHandler(new RetryConfig(0, 50))
                .setHeader(HttpHeaders.ACCEPT, ContentTypes.APPLICATION_JSON.toString())
                .setHeader(HttpHeaders.ACCEPT_LANGUAGE, "en-US,en")
                .setHeader(HttpHeaders.USER_AGENT, userAgent);
    }

}
