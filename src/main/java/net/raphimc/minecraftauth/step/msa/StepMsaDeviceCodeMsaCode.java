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
package net.raphimc.minecraftauth.step.msa;

import com.google.gson.JsonObject;
import net.lenni0451.commons.httpclient.HttpClient;
import net.lenni0451.commons.httpclient.constants.StatusCodes;
import net.lenni0451.commons.httpclient.content.impl.URLEncodedFormContent;
import net.lenni0451.commons.httpclient.requests.impl.PostRequest;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.responsehandler.MsaResponseHandler;
import net.raphimc.minecraftauth.responsehandler.exception.MsaRequestException;
import net.raphimc.minecraftauth.step.AbstractStep;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

public class StepMsaDeviceCodeMsaCode extends MsaCodeStep<StepMsaDeviceCode.MsaDeviceCode> {

    private final int timeout;

    public StepMsaDeviceCodeMsaCode(final AbstractStep<?, StepMsaDeviceCode.MsaDeviceCode> prevStep, final int timeout) {
        super(prevStep);

        this.timeout = timeout;
    }

    @Override
    public MsaCode applyStep(final HttpClient httpClient, final StepMsaDeviceCode.MsaDeviceCode msaDeviceCode) throws Exception {
        MinecraftAuth.LOGGER.info("Waiting for MSA login via device code...");

        final long start = System.currentTimeMillis();
        while (!msaDeviceCode.isExpired() && System.currentTimeMillis() - start <= this.timeout) {
            final Map<String, String> postData = new HashMap<>();
            postData.put("client_id", msaDeviceCode.getApplicationDetails().getClientId());
            postData.put("device_code", msaDeviceCode.getDeviceCode());
            postData.put("grant_type", "device_code");

            final PostRequest postRequest = new PostRequest(msaDeviceCode.getApplicationDetails().getOAuthEnvironment().getTokenUrl());
            postRequest.setContent(new URLEncodedFormContent(postData));
            try {
                final JsonObject obj = httpClient.execute(postRequest, new MsaResponseHandler());

                final MsaCode msaCode = new MsaCode(obj.get("refresh_token").getAsString(), msaDeviceCode.getApplicationDetails().withRedirectUri(null));
                MinecraftAuth.LOGGER.info("Got MSA Code");
                return msaCode;
            } catch (MsaRequestException e) {
                if (e.getResponse().getStatusCode() == StatusCodes.BAD_REQUEST && e.getError().equals("authorization_pending")) {
                    Thread.sleep(msaDeviceCode.getIntervalMs());
                    continue;
                }
                throw e;
            }
        }

        throw new TimeoutException("Failed to get MSA Code. Login timed out");
    }

}
