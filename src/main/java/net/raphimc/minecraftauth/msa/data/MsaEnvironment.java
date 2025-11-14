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
package net.raphimc.minecraftauth.msa.data;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MsaEnvironment {

    LIVE("https://login.live.com/", "oauth20_connect.srf", "oauth20_authorize.srf", "oauth20_token.srf", "oauth20_desktop.srf"),
    MICROSOFT_ONLINE_COMMON("https://login.microsoftonline.com/common/oauth2/", "v2.0/devicecode", "v2.0/authorize", "v2.0/token", "nativeclient"),
    MICROSOFT_ONLINE_CONSUMERS("https://login.microsoftonline.com/consumers/oauth2/", "v2.0/devicecode", "v2.0/authorize", "v2.0/token", "nativeclient"),
    ;

    private final String baseUrl;
    private final String deviceCodePath;
    private final String authorizePath;
    private final String tokenPath;
    private final String nativeClientPath;

    public String getDeviceCodeUrl() {
        return this.baseUrl + this.deviceCodePath;
    }

    public String getAuthorizeUrl() {
        return this.baseUrl + this.authorizePath;
    }

    public String getTokenUrl() {
        return this.baseUrl + this.tokenPath;
    }

    public String getNativeClientUrl() {
        return this.baseUrl + this.nativeClientPath;
    }

}
