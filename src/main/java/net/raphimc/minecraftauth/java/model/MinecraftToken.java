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
package net.raphimc.minecraftauth.java.model;

import com.google.gson.JsonObject;
import lombok.Value;
import net.lenni0451.commons.gson.elements.GsonObject;
import net.raphimc.minecraftauth.util.Expirable;

@Value
public class MinecraftToken implements Expirable {

    public static MinecraftToken fromJson(final JsonObject json) {
        return fromJson(new GsonObject(json));
    }

    public static MinecraftToken fromJson(final GsonObject json) {
        return new MinecraftToken(
                json.reqLong("expireTimeMs"),
                json.reqString("type"),
                json.reqString("token")
        );
    }

    public static JsonObject toJson(final MinecraftToken token) {
        final JsonObject json = new JsonObject();
        json.addProperty("_saveVersion", 1);
        json.addProperty("expireTimeMs", token.expireTimeMs);
        json.addProperty("type", token.type);
        json.addProperty("token", token.token);
        return json;
    }

    long expireTimeMs;
    String type;
    String token;

    public String getAuthorizationHeader() {
        return this.type + ' ' + this.token;
    }

}
