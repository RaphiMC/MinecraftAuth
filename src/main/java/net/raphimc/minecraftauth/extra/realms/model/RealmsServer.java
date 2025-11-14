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
package net.raphimc.minecraftauth.extra.realms.model;

import com.google.gson.JsonObject;
import lombok.Value;
import net.lenni0451.commons.gson.elements.GsonObject;
import net.raphimc.minecraftauth.util.StringUtil;

@Value
public class RealmsServer {

    public static RealmsServer fromApiJson(final GsonObject json) {
        return new RealmsServer(
                json.getLong("id", -1L),
                StringUtil.emptyToNull(json.getString("name", null)),
                StringUtil.emptyToNull(json.getString("motd", null)),
                StringUtil.emptyToNull(json.getString("owner", null)),
                StringUtil.emptyToNull(json.getString("ownerUUID", null)),
                json.getString("state", "CLOSED"),
                json.getBoolean("expired", false),
                json.getInt("daysLeft", 0),
                json.getString("worldType", "NORMAL"),
                json.getInt("maxPlayers", 0),
                json.getString("compatibility", "COMPATIBLE").equals("COMPATIBLE"),
                StringUtil.emptyToNull(json.getString("activeVersion", null)),
                json.getJsonObject()
        );
    }

    long id;
    String name;
    String motd;
    String ownerName;
    String ownerUid;
    String state;
    boolean expired;
    int daysLeft;
    String worldType;
    int maxPlayers;
    boolean compatible;
    String activeVersion;
    JsonObject rawResponse;

    public String getNameOr(final String fallback) {
        return this.name == null ? fallback : this.name;
    }

    public String getMotdOr(final String fallback) {
        return this.motd == null ? fallback : this.motd;
    }

    public String getOwnerNameOr(final String fallback) {
        return this.ownerName == null ? fallback : this.ownerName;
    }

    public String getOwnerUidOr(final String fallback) {
        return this.ownerUid == null ? fallback : this.ownerUid;
    }

    public String getActiveVersionOr(final String fallback) {
        return this.activeVersion == null ? fallback : this.activeVersion;
    }

}
