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

import java.util.UUID;

@Value
public class MinecraftCertificateChain implements Expirable {

    public static MinecraftCertificateChain fromJson(final JsonObject json) {
        return fromJson(new GsonObject(json));
    }

    public static MinecraftCertificateChain fromJson(final GsonObject json) {
        return new MinecraftCertificateChain(
                json.reqString("mojangJwt"),
                json.reqString("identityJwt")
        );
    }

    public static JsonObject toJson(final MinecraftCertificateChain certificateChain) {
        final JsonObject json = new JsonObject();
        json.addProperty("_saveVersion", 1);
        json.addProperty("mojangJwt", certificateChain.mojangJwt);
        json.addProperty("identityJwt", certificateChain.identityJwt);
        return json;
    }

    String mojangJwt;
    String identityJwt;

    @Getter(lazy = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    Jwt parsedMojangJwt = Jwt.parse(this.mojangJwt);

    @Getter(lazy = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    Jwt parsedIdentityJwt = Jwt.parse(this.identityJwt);

    @Override
    public long getExpireTimeMs() {
        return Math.min(this.getParsedMojangJwt().getExpireTimeMs(), this.getParsedIdentityJwt().getExpireTimeMs());
    }

    public String getIdentityDisplayName() {
        return this.getParsedIdentityJwt().getPayload().reqObject("extraData").reqString("displayName");
    }

    public String getIdentityXuid() {
        return this.getParsedIdentityJwt().getPayload().reqObject("extraData").reqString("XUID");
    }

    public UUID getIdentityUuid() {
        return UUID.fromString(this.getParsedIdentityJwt().getPayload().reqObject("extraData").reqString("identity"));
    }

}
