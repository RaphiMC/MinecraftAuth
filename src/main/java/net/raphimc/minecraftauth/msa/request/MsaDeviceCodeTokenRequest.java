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
package net.raphimc.minecraftauth.msa.request;

import net.lenni0451.commons.gson.elements.GsonObject;
import net.lenni0451.commons.httpclient.HttpResponse;
import net.lenni0451.commons.httpclient.content.impl.URLEncodedFormContent;
import net.lenni0451.commons.httpclient.requests.impl.PostRequest;
import net.raphimc.minecraftauth.msa.model.MsaApplicationConfig;
import net.raphimc.minecraftauth.msa.model.MsaDeviceCode;
import net.raphimc.minecraftauth.msa.model.MsaToken;
import net.raphimc.minecraftauth.msa.responsehandler.MsaResponseHandler;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

public class MsaDeviceCodeTokenRequest extends PostRequest implements MsaResponseHandler<MsaToken> {

    public MsaDeviceCodeTokenRequest(final MsaApplicationConfig applicationConfig, final MsaDeviceCode deviceCode) throws MalformedURLException {
        this(applicationConfig, deviceCode.getDeviceCode());
    }

    public MsaDeviceCodeTokenRequest(final MsaApplicationConfig applicationConfig, final String deviceCode) throws MalformedURLException {
        super(applicationConfig.getEnvironment().getTokenUrl());

        final Map<String, String> postData = new HashMap<>();
        postData.put("client_id", applicationConfig.getClientId());
        postData.put("grant_type", "device_code");
        postData.put("device_code", deviceCode);
        this.setContent(new URLEncodedFormContent(postData));
    }

    @Override
    public MsaToken handle(final HttpResponse response, final GsonObject json) {
        return new MsaToken(
                System.currentTimeMillis() + json.reqInt("expires_in") * 1000L,
                json.reqString("access_token"),
                json.getString("refresh_token", null)
        );
    }

}
