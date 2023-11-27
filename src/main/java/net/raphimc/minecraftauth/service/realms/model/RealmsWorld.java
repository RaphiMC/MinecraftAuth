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
package net.raphimc.minecraftauth.service.realms.model;

import com.google.gson.JsonObject;
import lombok.Value;
import net.raphimc.minecraftauth.util.JsonUtil;

@Value
public class RealmsWorld {

    long id;
    String ownerName;
    String ownerUuidOrXuid;
    String name;
    String motd;
    String state;
    boolean expired;
    String worldType;
    int maxPlayers;
    boolean compatible;
    String activeVersion;
    JsonObject rawResponse;

    public static RealmsWorld fromJson(final JsonObject json) {
        return new RealmsWorld(
                json.get("id").getAsLong(),
                JsonUtil.getStringOr(json, "owner", ""),
                json.get("ownerUUID").getAsString(),
                json.get("name").getAsString(),
                json.get("motd").getAsString(),
                json.get("state").getAsString(),
                json.get("expired").getAsBoolean(),
                json.get("worldType").getAsString(),
                json.get("maxPlayers").getAsInt(),
                JsonUtil.getStringOr(json, "compatibility", "COMPATIBLE").equals("COMPATIBLE"),
                JsonUtil.getStringOr(json, "activeVersion", ""),
                json
        );
    }

}
