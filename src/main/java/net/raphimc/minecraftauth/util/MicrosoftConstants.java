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
package net.raphimc.minecraftauth.util;

import net.raphimc.minecraftauth.MinecraftAuth;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.StandardHttpRequestRetryHandler;
import org.apache.http.message.BasicHeader;

import java.util.ArrayList;
import java.util.List;

public class MicrosoftConstants {

    public static final String JAVA_TITLE_ID = "00000000402b5328"; // Win32
    public static final String BEDROCK_NINTENDO_TITLE_ID = "00000000441cc96b"; // Nintendo
    public static final String BEDROCK_ANDROID_TITLE_ID = "0000000048183522"; // Android
    public static final String BEDROCK_IOS_TITLE_ID = "000000004c17c01a"; // iOS

    public static final String BEDROCK_PLAY_FAB_TITLE_ID = "20CA2";

    public static final String SCOPE1 = "XboxLive.signin XboxLive.offline_access";
    public static final String SCOPE2 = "XboxLive.signin offline_access";
    public static final String SCOPE3 = "offline_access XboxLive.signin XboxLive.offline_access";
    public static final String SCOPE_TITLE_AUTH = "service::user.auth.xboxlive.com::MBI_SSL";

    public static final String XBL_XSTS_RELYING_PARTY = "http://xboxlive.com";
    public static final String JAVA_XSTS_RELYING_PARTY = "rp://api.minecraftservices.com/";
    public static final String BEDROCK_XSTS_RELYING_PARTY = "https://multiplayer.minecraft.net/";
    public static final String BEDROCK_PLAY_FAB_XSTS_RELYING_PARTY = "https://b980a380.minecraft.playfabapi.com/";
    public static final String BEDROCK_REALMS_XSTS_RELYING_PARTY = "https://pocket.realms.minecraft.net/";

    public static CloseableHttpClient createHttpClient() {
        final int timeout = 5;
        final RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(timeout * 1000)
                .setConnectionRequestTimeout(timeout * 1000)
                .setSocketTimeout(timeout * 1000).build();

        final List<Header> headers = getDefaultHeaders();
        headers.add(new BasicHeader(HttpHeaders.USER_AGENT, "MinecraftAuth/" + MinecraftAuth.VERSION));

        return HttpClientBuilder.create()
                .setDefaultRequestConfig(requestConfig)
                .setDefaultHeaders(headers)
                .setRetryHandler(new StandardHttpRequestRetryHandler())
                .build();
    }

    public static List<Header> getDefaultHeaders() {
        final List<Header> headers = new ArrayList<>();
        headers.add(new BasicHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType()));
        headers.add(new BasicHeader(HttpHeaders.ACCEPT_LANGUAGE, "en-US,en"));
        return headers;
    }

}
