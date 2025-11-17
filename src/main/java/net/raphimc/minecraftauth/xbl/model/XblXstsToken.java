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
import org.jetbrains.annotations.ApiStatus;

import java.time.Instant;

@Value
public class XblXstsToken implements Expirable {

    public static XblXstsToken fromJson(final JsonObject json) {
        return fromJson(new GsonObject(json));
    }

    public static XblXstsToken fromJson(final GsonObject json) {
        return new XblXstsToken(
                json.reqLong("expireTimeMs"),
                json.reqString("token"),
                json.reqString("userHash")
        );
    }

    public static JsonObject toJson(final XblXstsToken xstsToken) {
        final JsonObject json = new JsonObject();
        json.addProperty("_saveVersion", 1);
        json.addProperty("expireTimeMs", xstsToken.expireTimeMs);
        json.addProperty("token", xstsToken.token);
        json.addProperty("userHash", xstsToken.userHash);
        return json;
    }

    @ApiStatus.Internal
    public static XblXstsToken fromApiJson(final GsonObject json) {
        return new XblXstsToken(
                Instant.parse(json.reqString("NotAfter")).toEpochMilli(),
                json.reqString("Token"),
                json.reqObject("DisplayClaims").reqArray("xui").get(0).asObject().reqString("uhs")
        );
    }

    long expireTimeMs;
    String token;
    String userHash;

    public String getAuthorizationHeader() {
        return "XBL3.0 x=" + this.userHash + ';' + this.token;
    }

}
