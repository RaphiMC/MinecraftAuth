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
package net.raphimc.minecraftauth.util;

import com.google.gson.JsonObject;
import net.lenni0451.commons.gson.elements.GsonElement;
import net.lenni0451.commons.gson.elements.GsonObject;
import net.raphimc.minecraftauth.msa.data.MsaConstants;
import net.raphimc.minecraftauth.msa.model.MsaApplicationConfig;
import net.raphimc.minecraftauth.msa.model.MsaToken;

import java.util.Map;
import java.util.UUID;

/**
 * A utility class to migrate MinecraftAuth 4.x.x save data to 5.x.x format.<br>
 * Starting with MinecraftAuth 5.x.x, the save data format has changed significantly and supports automatic migration from that point on.
 */
public class MinecraftAuth4To5Migrator {

    /**
     * Migrate a Java Edition token chain from MinecraftAuth 4.x.x to the 5.x.x format.
     *
     * @param oldSaveData The old save data.
     * @return The migrated save data.
     */
    public static JsonObject migrateJavaSave(final JsonObject oldSaveData) {
        final String refreshToken = findRefreshToken(new GsonObject(oldSaveData));
        if (refreshToken == null) {
            throw new IllegalArgumentException("Failed to find refresh token in the provided save data");
        }

        final JsonObject newJson = new JsonObject();
        newJson.addProperty("_saveVersion", 1);
        newJson.add("msaApplicationConfig", MsaApplicationConfig.toJson(new MsaApplicationConfig(MsaConstants.JAVA_TITLE_ID, MsaConstants.SCOPE_TITLE_AUTH)));
        newJson.addProperty("deviceType", "Win32");
        newJson.add("deviceKeyPair", JsonUtil.encodeKeyPair(CryptUtil.generateEcdsa256KeyPair()));
        newJson.addProperty("deviceId", UUID.randomUUID().toString());
        newJson.add("msaToken", MsaToken.toJson(new MsaToken(0L, "", refreshToken)));
        return newJson;
    }

    /**
     * Migrate a Bedrock Edition token chain from MinecraftAuth 4.x.x to the 5.x.x format.
     *
     * @param oldSaveData The old save data.
     * @return The migrated save data.
     */
    public static JsonObject migrateBedrockSave(final JsonObject oldSaveData) {
        final String refreshToken = findRefreshToken(new GsonObject(oldSaveData));
        if (refreshToken == null) {
            throw new IllegalArgumentException("Failed to find refresh token in the provided save data");
        }

        final JsonObject newJson = new JsonObject();
        newJson.addProperty("_saveVersion", 1);
        newJson.add("msaApplicationConfig", MsaApplicationConfig.toJson(new MsaApplicationConfig(MsaConstants.BEDROCK_ANDROID_TITLE_ID, MsaConstants.SCOPE_TITLE_AUTH)));
        newJson.addProperty("deviceType", "Android");
        newJson.add("deviceKeyPair", JsonUtil.encodeKeyPair(CryptUtil.generateEcdsa256KeyPair()));
        newJson.addProperty("deviceId", UUID.randomUUID().toString());
        newJson.add("sessionKeyPair", JsonUtil.encodeKeyPair(CryptUtil.generateEcdsa384KeyPair()));
        newJson.add("msaToken", MsaToken.toJson(new MsaToken(0L, "", refreshToken)));
        return newJson;
    }

    public static String findRefreshToken(final GsonObject json) {
        if (json.hasString("refreshToken")) {
            return json.reqString("refreshToken");
        } else {
            for (Map.Entry<String, GsonElement> entry : json.entrySet()) {
                if (entry.getValue().isObject()) {
                    final String refreshToken = findRefreshToken(entry.getValue().asObject());
                    if (refreshToken != null) {
                        return refreshToken;
                    }
                }
            }
        }
        return null;
    }

}
