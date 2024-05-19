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
package net.raphimc.minecraftauth.util;

import net.lenni0451.commons.httpclient.HttpClient;
import net.lenni0451.commons.httpclient.HttpResponse;
import net.lenni0451.commons.httpclient.requests.impl.GetRequest;
import net.raphimc.minecraftauth.MinecraftAuth;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

public class TimeUtil {

    private static Duration CLIENT_TIME_OFFSET = null;

    /**
     * Gets the time offset between the client and the microsoft server. This is used to calculate the correct time for authentication and signatures.
     *
     * @return The time offset between the client and the microsoft server
     */
    public static synchronized Duration getClientTimeOffset() {
        if (CLIENT_TIME_OFFSET == null) {
            final HttpClient httpClient = MinecraftAuth.createHttpClient();
            httpClient.getRetryHandler().setMaxConnectRetries(3);
            try {
                final HttpResponse response = httpClient.execute(new GetRequest(OAuthEnvironment.LIVE.getBaseUrl()));

                final Instant clientTime = Instant.now();
                final Instant serverTime = response.getFirstHeader("Date").map(s -> DateTimeFormatter.RFC_1123_DATE_TIME.parse(s, Instant::from)).orElse(clientTime);
                CLIENT_TIME_OFFSET = Duration.between(clientTime, serverTime);

                if (CLIENT_TIME_OFFSET.abs().compareTo(Duration.ofMinutes(2)) > 0) {
                    MinecraftAuth.LOGGER.warn("Local clock is off by " + CLIENT_TIME_OFFSET.toMinutes() + " minutes");
                }
            } catch (Throwable e) {
                MinecraftAuth.LOGGER.error("Failed to get client time offset. This may cause issues with authentication");
                e.printStackTrace();
                CLIENT_TIME_OFFSET = Duration.ZERO;
            }
        }

        return CLIENT_TIME_OFFSET;
    }

}
