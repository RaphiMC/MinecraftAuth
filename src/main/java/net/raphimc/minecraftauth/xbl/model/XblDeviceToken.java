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
package net.raphimc.minecraftauth.xbl.model;

import com.google.gson.JsonObject;
import lombok.Value;
import net.lenni0451.commons.gson.elements.GsonObject;
import net.raphimc.minecraftauth.util.Expirable;

@Value
public class XblDeviceToken implements Expirable {

    public static XblDeviceToken fromJson(final JsonObject json) {
        return fromJson(new GsonObject(json));
    }

    public static XblDeviceToken fromJson(final GsonObject json) {
        return new XblDeviceToken(
                json.reqLong("expireTimeMs"),
                json.reqString("token"),
                json.reqString("deviceId")
        );
    }

    public static JsonObject toJson(final XblDeviceToken deviceToken) {
        final JsonObject json = new JsonObject();
        json.addProperty("_saveVersion", 1);
        json.addProperty("expireTimeMs", deviceToken.expireTimeMs);
        json.addProperty("token", deviceToken.token);
        json.addProperty("deviceId", deviceToken.deviceId);
        return json;
    }

    long expireTimeMs;
    String token;
    String deviceId;

}
