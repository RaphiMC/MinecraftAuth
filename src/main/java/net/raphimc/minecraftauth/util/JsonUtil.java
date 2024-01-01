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

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class JsonUtil {

    public static final Gson GSON = new Gson();

    public static JsonElement parseString(final String json) {
        return GSON.fromJson(json, JsonElement.class);
    }

    public static String getStringOr(final JsonObject obj, final String key, final String defaultValue) {
        final JsonElement element = obj.get(key);
        if (element != null && !element.isJsonNull()) {
            return element.getAsString();
        } else {
            return defaultValue;
        }
    }

    public static int getIntOr(final JsonObject obj, final String key, final int defaultValue) {
        final JsonElement element = obj.get(key);
        if (element != null && !element.isJsonNull()) {
            return element.getAsInt();
        } else {
            return defaultValue;
        }
    }

    public static long getLongOr(final JsonObject obj, final String key, final long defaultValue) {
        final JsonElement element = obj.get(key);
        if (element != null && !element.isJsonNull()) {
            return element.getAsLong();
        } else {
            return defaultValue;
        }
    }

}
