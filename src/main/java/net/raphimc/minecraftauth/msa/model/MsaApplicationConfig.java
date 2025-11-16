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
package net.raphimc.minecraftauth.msa.model;

import com.google.gson.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.With;
import net.lenni0451.commons.gson.elements.GsonObject;
import net.raphimc.minecraftauth.msa.data.MsaEnvironment;
import net.raphimc.minecraftauth.util.UuidUtil;

import java.util.HashMap;
import java.util.Map;

@Value
@With
@AllArgsConstructor
public class MsaApplicationConfig {

    public static MsaApplicationConfig fromJson(final JsonObject json) {
        return fromJson(new GsonObject(json));
    }

    public static MsaApplicationConfig fromJson(final GsonObject json) {
        return new MsaApplicationConfig(
                json.reqString("clientId"),
                json.reqString("scope"),
                json.getString("clientSecret", null),
                json.getString("redirectUri", null),
                MsaEnvironment.valueOf(json.getString("environment", MsaEnvironment.LIVE.name()))
        );
    }

    public static JsonObject toJson(final MsaApplicationConfig applicationConfig) {
        final JsonObject json = new JsonObject();
        json.addProperty("_saveVersion", 1);
        json.addProperty("clientId", applicationConfig.clientId);
        json.addProperty("scope", applicationConfig.scope);
        json.addProperty("clientSecret", applicationConfig.clientSecret);
        json.addProperty("redirectUri", applicationConfig.redirectUri);
        json.addProperty("environment", applicationConfig.environment.name());
        return json;
    }

    String clientId;
    String scope;
    String clientSecret;
    String redirectUri;
    MsaEnvironment environment;

    public MsaApplicationConfig(final String clientId, final String scope) {
        this(clientId, scope, null, null, MsaEnvironment.LIVE);
    }

    public boolean isTitleClientId() {
        return !UuidUtil.isDashedUuid(this.clientId);
    }

    public Map<String, String> getAuthCodeParameters() {
        final Map<String, String> parameters = new HashMap<>();
        parameters.put("client_id", this.clientId);
        parameters.put("scope", this.scope);
        if (this.redirectUri != null) {
            parameters.put("redirect_uri", this.redirectUri);
        }
        parameters.put("response_type", "code");
        parameters.put("response_mode", "query");
        return parameters;
    }

}
