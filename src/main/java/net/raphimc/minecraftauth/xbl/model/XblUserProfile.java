/*
 * This file is part of MinecraftAuth - https://github.com/RaphiMC/MinecraftAuth
 * Copyright (C) 2022-2026 RK_01/RaphiMC and contributors
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
import net.raphimc.minecraftauth.util.JsonUtil;

import java.util.Map;

@Value
public class XblUserProfile implements Expirable {

    public static XblUserProfile fromJson(final JsonObject json) {
        return fromJson(new GsonObject(json));
    }

    public static XblUserProfile fromJson(final GsonObject json) {
        return new XblUserProfile(
                json.reqString("id"),
                JsonUtil.decodeMap(json.reqObject("settings"))
        );
    }

    public static JsonObject toJson(final XblUserProfile profile) {
        final JsonObject json = new JsonObject();
        json.addProperty("_saveVersion", 1);
        json.addProperty("id", profile.id);
        json.add("settings", JsonUtil.encodeMap(profile.settings));
        return json;
    }

    String id;
    Map<String, String> settings;

    @Override
    public long getExpireTimeMs() {
        return Long.MAX_VALUE;
    }

}
