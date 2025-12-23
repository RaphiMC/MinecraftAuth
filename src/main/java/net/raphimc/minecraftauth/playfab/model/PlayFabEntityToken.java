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
package net.raphimc.minecraftauth.playfab.model;

import com.google.gson.JsonObject;
import lombok.Value;
import net.lenni0451.commons.gson.elements.GsonObject;
import net.raphimc.minecraftauth.util.Expirable;
import org.jetbrains.annotations.ApiStatus;

import java.time.Instant;

@Value
public class PlayFabEntityToken implements Expirable {

    public static PlayFabEntityToken fromJson(final JsonObject json) {
        return fromJson(new GsonObject(json));
    }

    public static PlayFabEntityToken fromJson(final GsonObject json) {
        return new PlayFabEntityToken(
                json.reqLong("expireTimeMs"),
                json.reqString("token"),
                json.reqString("entityId"),
                json.reqString("entityType")
        );
    }

    public static JsonObject toJson(final PlayFabEntityToken entityToken) {
        final JsonObject json = new JsonObject();
        json.addProperty("_saveVersion", 1);
        json.addProperty("expireTimeMs", entityToken.expireTimeMs);
        json.addProperty("token", entityToken.token);
        json.addProperty("entityId", entityToken.entityId);
        json.addProperty("entityType", entityToken.entityType);
        return json;
    }

    @ApiStatus.Internal
    public static PlayFabEntityToken fromApiJson(final GsonObject json) {
        final GsonObject entity = json.reqObject("Entity");
        return new PlayFabEntityToken(
                Instant.parse(json.reqString("TokenExpiration")).toEpochMilli(),
                json.reqString("EntityToken"),
                entity.reqString("Id"),
                entity.reqString("Type")
        );
    }

    long expireTimeMs;
    String token;
    String entityId;
    String entityType;

}
