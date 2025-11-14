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
package net.raphimc.minecraftauth.bedrock.model;

import com.google.gson.JsonObject;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.Value;
import net.lenni0451.commons.gson.elements.GsonObject;
import net.raphimc.minecraftauth.util.Expirable;
import net.raphimc.minecraftauth.util.jwt.Jwt;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Value
public class MinecraftMultiplayerToken implements Expirable {

    public static MinecraftMultiplayerToken fromJson(final JsonObject json) {
        return fromJson(new GsonObject(json));
    }

    public static MinecraftMultiplayerToken fromJson(final GsonObject json) {
        return new MinecraftMultiplayerToken(
                json.reqLong("expireTimeMs"),
                json.reqString("token")
        );
    }

    public static JsonObject toJson(final MinecraftMultiplayerToken multiplayerToken) {
        final JsonObject json = new JsonObject();
        json.addProperty("_saveVersion", 1);
        json.addProperty("expireTimeMs", multiplayerToken.expireTimeMs);
        json.addProperty("token", multiplayerToken.token);
        return json;
    }

    long expireTimeMs;
    String token;

    @Getter(lazy = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    Jwt parsedToken = Jwt.parse(this.token);

    public String getDisplayName() {
        return this.getParsedToken().getPayload().reqString("xname");
    }

    public String getXuid() {
        return this.getParsedToken().getPayload().reqString("xid");
    }

    public UUID getUuid() {
        return UUID.nameUUIDFromBytes(("pocket-auth-1-xuid:" + this.getXuid()).getBytes(StandardCharsets.UTF_8));
    }

}
