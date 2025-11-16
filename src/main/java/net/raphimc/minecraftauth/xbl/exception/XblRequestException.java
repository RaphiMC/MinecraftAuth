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
package net.raphimc.minecraftauth.xbl.exception;

import lombok.Getter;
import net.lenni0451.commons.httpclient.HttpResponse;
import net.raphimc.minecraftauth.util.http.exception.ApiHttpRequestException;

import java.util.HashMap;
import java.util.Map;

@Getter
public class XblRequestException extends ApiHttpRequestException {

    public static final Map<Long, String> ERRORS = new HashMap<>();
    public static final Map<Long, String> ERROR_MESSAGES = new HashMap<>();

    static {
        ERRORS.put(0x87DD0003L, "AM_E_XASD_UNEXPECTED");
        ERRORS.put(0x87DD0004L, "AM_E_XASU_UNEXPECTED");
        ERRORS.put(0x87DD0005L, "AM_E_XAST_UNEXPECTED");
        ERRORS.put(0x87DD0006L, "AM_E_XSTS_UNEXPECTED");
        ERRORS.put(0x87DD0007L, "AM_E_XDEVICE_UNEXPECTED");
        ERRORS.put(0x87DD0008L, "AM_E_DEVMODE_NOT_AUTHORIZED");
        ERRORS.put(0x87DD0009L, "AM_E_NOT_AUTHORIZED");
        ERRORS.put(0x87DD000AL, "AM_E_FORBIDDEN");
        ERRORS.put(0x87DD000BL, "AM_E_UNKNOWN_TARGET");
        ERRORS.put(0x87DD000CL, "AM_E_NSAL_READ_FAILED");
        ERRORS.put(0x87DD000DL, "AM_E_TITLE_NOT_AUTHENTICATED");
        ERRORS.put(0x87DD000EL, "AM_E_TITLE_NOT_AUTHORIZED");
        ERRORS.put(0x87DD000FL, "AM_E_DEVICE_NOT_AUTHENTICATED");
        ERRORS.put(0x87DD0010L, "AM_E_INVALID_USER_INDEX");
        ERRORS.put(0x8015DC00L, "XO_E_DEVMODE_NOT_AUTHORIZED");
        ERRORS.put(0x8015DC01L, "XO_E_SYSTEM_UPDATE_REQUIRED");
        ERRORS.put(0x8015DC02L, "XO_E_CONTENT_UPDATE_REQUIRED");
        ERRORS.put(0x8015DC03L, "XO_E_ENFORCEMENT_BAN");
        ERRORS.put(0x8015DC04L, "XO_E_THIRD_PARTY_BAN");
        ERRORS.put(0x8015DC05L, "XO_E_ACCOUNT_PARENTALLY_RESTRICTED");
        ERRORS.put(0x8015DC06L, "XO_E_DEVICE_SUBSCRIPTION_NOT_ACTIVATED");
        ERRORS.put(0x8015DC08L, "XO_E_ACCOUNT_BILLING_MAINTENANCE_REQUIRED");
        ERRORS.put(0x8015DC09L, "XO_E_ACCOUNT_CREATION_REQUIRED");
        ERRORS.put(0x8015DC0AL, "XO_E_ACCOUNT_TERMS_OF_USE_NOT_ACCEPTED");
        ERRORS.put(0x8015DC0BL, "XO_E_ACCOUNT_COUNTRY_NOT_AUTHORIZED");
        ERRORS.put(0x8015DC0CL, "XO_E_ACCOUNT_AGE_VERIFICATION_REQUIRED");
        ERRORS.put(0x8015DC0DL, "XO_E_ACCOUNT_CURFEW");
        ERRORS.put(0x8015DC0EL, "XO_E_ACCOUNT_ZEST_MAINTENANCE_REQUIRED");
        ERRORS.put(0x8015DC0FL, "XO_E_ACCOUNT_CSV_TRANSITION_REQUIRED");
        ERRORS.put(0x8015DC10L, "XO_E_ACCOUNT_MAINTENANCE_REQUIRED");
        ERRORS.put(0x8015DC11L, "XO_E_ACCOUNT_TYPE_NOT_ALLOWED");
        ERRORS.put(0x8015DC12L, "XO_E_CONTENT_ISOLATION");
        ERRORS.put(0x8015DC13L, "XO_E_ACCOUNT_NAME_CHANGE_REQUIRED");
        ERRORS.put(0x8015DC14L, "XO_E_DEVICE_CHALLENGE_REQUIRED");
        ERRORS.put(0x8015DC20L, "XO_E_EXPIRED_DEVICE_TOKEN");
        ERRORS.put(0x8015DC21L, "XO_E_EXPIRED_TITLE_TOKEN");
        ERRORS.put(0x8015DC22L, "XO_E_EXPIRED_USER_TOKEN");
        ERRORS.put(0x8015DC23L, "XO_E_INVALID_DEVICE_TOKEN");
        ERRORS.put(0x8015DC24L, "XO_E_INVALID_TITLE_TOKEN");
        ERRORS.put(0x8015DC25L, "XO_E_INVALID_USER_TOKEN");

        ERROR_MESSAGES.put(0x8015DC03L, "Your account was banned by Xbox for violating one or more Community Standards for Xbox.");
        ERROR_MESSAGES.put(0x8015DC05L, "Your account is currently restricted and your guardian has not given you permission to play online. Login to https://account.microsoft.com/family/ and have your guardian change your permissions.");
        ERROR_MESSAGES.put(0x8015DC09L, "Your account doesn't have an Xbox profile. Please create one at https://www.xbox.com/live");
        ERROR_MESSAGES.put(0x8015DC0AL, "Your account has not accepted Xbox's Terms of Service. Please login at https://www.xbox.com/live and accept them.");
        ERROR_MESSAGES.put(0x8015DC0BL, "Your account is from a country where Xbox Live is not available/banned.");
        ERROR_MESSAGES.put(0x8015DC0CL, "Your account requires proof of age. Please login to https://login.live.com/login.srf and provide proof of age.");
        ERROR_MESSAGES.put(0x8015DC0DL, "Your account has reached the its limit for playtime. Your account has been blocked from logging in.");
        ERROR_MESSAGES.put(0x8015DC0EL, "Your account is a child (under 18) and cannot proceed unless the account is added to a Family by an adult.");
    }

    private final long errorCode;

    public XblRequestException(final HttpResponse response, final long errorCode) {
        super(response, ERRORS.getOrDefault(errorCode, String.valueOf(errorCode)), ERROR_MESSAGES.getOrDefault(errorCode, "An unknown error occurred"));
        this.errorCode = errorCode;
    }

}
