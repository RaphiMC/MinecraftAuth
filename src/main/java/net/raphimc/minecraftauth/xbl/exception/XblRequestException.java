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
package net.raphimc.minecraftauth.xbl.exception;

import lombok.Getter;
import net.lenni0451.commons.httpclient.HttpResponse;
import net.raphimc.minecraftauth.util.http.exception.ApiHttpRequestException;

import java.util.HashMap;
import java.util.Map;

// Error codes from https://github.com/microsoft/xbox-live-api/blob/main/Source/Shared/xsapi_utils.cpp
@Getter
public class XblRequestException extends ApiHttpRequestException {

    public static final long AM_E_XASD_UNEXPECTED = 0x87DD0003L;
    public static final long AM_E_XASU_UNEXPECTED = 0x87DD0004L;
    public static final long AM_E_XAST_UNEXPECTED = 0x87DD0005L;
    public static final long AM_E_XSTS_UNEXPECTED = 0x87DD0006L;
    public static final long AM_E_XDEVICE_UNEXPECTED = 0x87DD0007L;
    public static final long AM_E_DEVMODE_NOT_AUTHORIZED = 0x87DD0008L;
    public static final long AM_E_NOT_AUTHORIZED = 0x87DD0009L;
    public static final long AM_E_FORBIDDEN = 0x87DD000AL;
    public static final long AM_E_UNKNOWN_TARGET = 0x87DD000BL;
    public static final long AM_E_INVALID_NSAL_DATA = 0x87DD000CL;
    public static final long AM_E_TITLE_NOT_AUTHENTICATED = 0x87DD000DL;
    public static final long AM_E_TITLE_NOT_AUTHORIZED = 0x87DD000EL;
    public static final long AM_E_DEVICE_NOT_AUTHENTICATED = 0x87DD000FL;
    public static final long AM_E_INVALID_USER_INDEX = 0x87DD0010L;

    public static final long XO_E_DEVMODE_NOT_AUTHORIZED = 0x8015DC00L;
    public static final long XO_E_SYSTEM_UPDATE_REQUIRED = 0x8015DC01L;
    public static final long XO_E_CONTENT_UPDATE_REQUIRED = 0x8015DC02L;
    public static final long XO_E_ENFORCEMENT_BAN = 0x8015DC03L;
    public static final long XO_E_THIRD_PARTY_BAN = 0x8015DC04L;
    public static final long XO_E_ACCOUNT_PARENTALLY_RESTRICTED = 0x8015DC05L;
    public static final long XO_E_DEVICE_SUBSCRIPTION_NOT_ACTIVATED = 0x8015DC06L;
    public static final long XO_E_ACCOUNT_BILLING_MAINTENANCE_REQUIRED = 0x8015DC08L;
    public static final long XO_E_ACCOUNT_CREATION_REQUIRED = 0x8015DC09L;
    public static final long XO_E_ACCOUNT_TERMS_OF_USE_NOT_ACCEPTED = 0x8015DC0AL;
    public static final long XO_E_ACCOUNT_COUNTRY_NOT_AUTHORIZED = 0x8015DC0BL;
    public static final long XO_E_ACCOUNT_AGE_VERIFICATION_REQUIRED = 0x8015DC0CL;
    public static final long XO_E_ACCOUNT_CURFEW = 0x8015DC0DL;
    public static final long XO_E_ACCOUNT_CHILD_NOT_IN_FAMILY = 0x8015DC0EL;
    public static final long XO_E_ACCOUNT_CSV_TRANSITION_REQUIRED = 0x8015DC0FL;
    public static final long XO_E_ACCOUNT_MAINTENANCE_REQUIRED = 0x8015DC10L;
    public static final long XO_E_ACCOUNT_TYPE_NOT_ALLOWED = 0x8015DC11L;
    public static final long XO_E_CONTENT_ISOLATION = 0x8015DC12L;
    public static final long XO_E_ACCOUNT_NAME_CHANGE_REQUIRED = 0x8015DC13L;
    public static final long XO_E_DEVICE_CHALLENGE_REQUIRED = 0x8015DC14L;
    public static final long XO_E_SIGNIN_COUNT_BY_DEVICE_TYPE_EXCEEDED = 0x8015DC16L;
    public static final long XO_E_PIN_CHALLENGE_REQUIRED = 0x8015DC17L;
    public static final long XO_E_RETAIL_ACCOUNT_NOT_ALLOWED = 0x8015DC18L;
    public static final long XO_E_SANDBOX_NOT_ALLOWED = 0x8015DC19L;
    public static final long XO_E_ACCOUNT_SERVICE_UNAVAILABLE_UNKNOWN_USER = 0x8015DC1AL;
    public static final long XO_E_GREEN_SIGNED_CONTENT_NOT_AUTHORIZED = 0x8015DC1BL;
    public static final long XO_E_CONTENT_NOT_AUTHORIZED = 0x8015DC1CL;
    public static final long XO_E_EXPIRED_DEVICE_TOKEN = 0x8015DC20L;
    public static final long XO_E_EXPIRED_TITLE_TOKEN = 0x8015DC21L;
    public static final long XO_E_EXPIRED_USER_TOKEN = 0x8015DC22L;
    public static final long XO_E_INVALID_DEVICE_TOKEN = 0x8015DC23L;
    public static final long XO_E_INVALID_TITLE_TOKEN = 0x8015DC24L;
    public static final long XO_E_INVALID_USER_TOKEN = 0x8015DC25L;

    private static final Map<Long, String> ERRORS = new HashMap<>();
    private static final Map<Long, String> ERROR_MESSAGES = new HashMap<>();

    static {
        ERRORS.put(AM_E_XASD_UNEXPECTED, "AM_E_XASD_UNEXPECTED");
        ERRORS.put(AM_E_XASU_UNEXPECTED, "AM_E_XASU_UNEXPECTED");
        ERRORS.put(AM_E_XAST_UNEXPECTED, "AM_E_XAST_UNEXPECTED");
        ERRORS.put(AM_E_XSTS_UNEXPECTED, "AM_E_XSTS_UNEXPECTED");
        ERRORS.put(AM_E_XDEVICE_UNEXPECTED, "AM_E_XDEVICE_UNEXPECTED");
        ERRORS.put(AM_E_DEVMODE_NOT_AUTHORIZED, "AM_E_DEVMODE_NOT_AUTHORIZED");
        ERRORS.put(AM_E_NOT_AUTHORIZED, "AM_E_NOT_AUTHORIZED");
        ERRORS.put(AM_E_FORBIDDEN, "AM_E_FORBIDDEN");
        ERRORS.put(AM_E_UNKNOWN_TARGET, "AM_E_UNKNOWN_TARGET");
        ERRORS.put(AM_E_INVALID_NSAL_DATA, "AM_E_INVALID_NSAL_DATA");
        ERRORS.put(AM_E_TITLE_NOT_AUTHENTICATED, "AM_E_TITLE_NOT_AUTHENTICATED");
        ERRORS.put(AM_E_TITLE_NOT_AUTHORIZED, "AM_E_TITLE_NOT_AUTHORIZED");
        ERRORS.put(AM_E_DEVICE_NOT_AUTHENTICATED, "AM_E_DEVICE_NOT_AUTHENTICATED");
        ERRORS.put(AM_E_INVALID_USER_INDEX, "AM_E_INVALID_USER_INDEX");

        ERRORS.put(XO_E_DEVMODE_NOT_AUTHORIZED, "XO_E_DEVMODE_NOT_AUTHORIZED");
        ERRORS.put(XO_E_SYSTEM_UPDATE_REQUIRED, "XO_E_SYSTEM_UPDATE_REQUIRED");
        ERRORS.put(XO_E_CONTENT_UPDATE_REQUIRED, "XO_E_CONTENT_UPDATE_REQUIRED");
        ERRORS.put(XO_E_ENFORCEMENT_BAN, "XO_E_ENFORCEMENT_BAN");
        ERRORS.put(XO_E_THIRD_PARTY_BAN, "XO_E_THIRD_PARTY_BAN");
        ERRORS.put(XO_E_ACCOUNT_PARENTALLY_RESTRICTED, "XO_E_ACCOUNT_PARENTALLY_RESTRICTED");
        ERRORS.put(XO_E_DEVICE_SUBSCRIPTION_NOT_ACTIVATED, "XO_E_DEVICE_SUBSCRIPTION_NOT_ACTIVATED");
        ERRORS.put(XO_E_ACCOUNT_BILLING_MAINTENANCE_REQUIRED, "XO_E_ACCOUNT_BILLING_MAINTENANCE_REQUIRED");
        ERRORS.put(XO_E_ACCOUNT_CREATION_REQUIRED, "XO_E_ACCOUNT_CREATION_REQUIRED");
        ERRORS.put(XO_E_ACCOUNT_TERMS_OF_USE_NOT_ACCEPTED, "XO_E_ACCOUNT_TERMS_OF_USE_NOT_ACCEPTED");
        ERRORS.put(XO_E_ACCOUNT_COUNTRY_NOT_AUTHORIZED, "XO_E_ACCOUNT_COUNTRY_NOT_AUTHORIZED");
        ERRORS.put(XO_E_ACCOUNT_AGE_VERIFICATION_REQUIRED, "XO_E_ACCOUNT_AGE_VERIFICATION_REQUIRED");
        ERRORS.put(XO_E_ACCOUNT_CURFEW, "XO_E_ACCOUNT_CURFEW");
        ERRORS.put(XO_E_ACCOUNT_CHILD_NOT_IN_FAMILY, "XO_E_ACCOUNT_CHILD_NOT_IN_FAMILY");
        ERRORS.put(XO_E_ACCOUNT_CSV_TRANSITION_REQUIRED, "XO_E_ACCOUNT_CSV_TRANSITION_REQUIRED");
        ERRORS.put(XO_E_ACCOUNT_MAINTENANCE_REQUIRED, "XO_E_ACCOUNT_MAINTENANCE_REQUIRED");
        ERRORS.put(XO_E_ACCOUNT_TYPE_NOT_ALLOWED, "XO_E_ACCOUNT_TYPE_NOT_ALLOWED");
        ERRORS.put(XO_E_CONTENT_ISOLATION, "XO_E_CONTENT_ISOLATION");
        ERRORS.put(XO_E_ACCOUNT_NAME_CHANGE_REQUIRED, "XO_E_ACCOUNT_NAME_CHANGE_REQUIRED");
        ERRORS.put(XO_E_DEVICE_CHALLENGE_REQUIRED, "XO_E_DEVICE_CHALLENGE_REQUIRED");
        ERRORS.put(XO_E_SIGNIN_COUNT_BY_DEVICE_TYPE_EXCEEDED, "XO_E_SIGNIN_COUNT_BY_DEVICE_TYPE_EXCEEDED");
        ERRORS.put(XO_E_PIN_CHALLENGE_REQUIRED, "XO_E_PIN_CHALLENGE_REQUIRED");
        ERRORS.put(XO_E_RETAIL_ACCOUNT_NOT_ALLOWED, "XO_E_RETAIL_ACCOUNT_NOT_ALLOWED");
        ERRORS.put(XO_E_SANDBOX_NOT_ALLOWED, "XO_E_SANDBOX_NOT_ALLOWED");
        ERRORS.put(XO_E_ACCOUNT_SERVICE_UNAVAILABLE_UNKNOWN_USER, "XO_E_ACCOUNT_SERVICE_UNAVAILABLE_UNKNOWN_USER");
        ERRORS.put(XO_E_GREEN_SIGNED_CONTENT_NOT_AUTHORIZED, "XO_E_GREEN_SIGNED_CONTENT_NOT_AUTHORIZED");
        ERRORS.put(XO_E_CONTENT_NOT_AUTHORIZED, "XO_E_CONTENT_NOT_AUTHORIZED");
        ERRORS.put(XO_E_EXPIRED_DEVICE_TOKEN, "XO_E_EXPIRED_DEVICE_TOKEN");
        ERRORS.put(XO_E_EXPIRED_TITLE_TOKEN, "XO_E_EXPIRED_TITLE_TOKEN");
        ERRORS.put(XO_E_EXPIRED_USER_TOKEN, "XO_E_EXPIRED_USER_TOKEN");
        ERRORS.put(XO_E_INVALID_DEVICE_TOKEN, "XO_E_INVALID_DEVICE_TOKEN");
        ERRORS.put(XO_E_INVALID_TITLE_TOKEN, "XO_E_INVALID_TITLE_TOKEN");
        ERRORS.put(XO_E_INVALID_USER_TOKEN, "XO_E_INVALID_USER_TOKEN");

        ERROR_MESSAGES.put(XO_E_ENFORCEMENT_BAN, "Your account was banned by Xbox for violating one or more Community Standards for Xbox.");
        ERROR_MESSAGES.put(XO_E_ACCOUNT_PARENTALLY_RESTRICTED, "Your account is currently restricted and your guardian has not given you permission to play online. Login to https://account.microsoft.com/family/ and have your guardian change your permissions.");
        ERROR_MESSAGES.put(XO_E_ACCOUNT_CREATION_REQUIRED, "Your account doesn't have an Xbox profile. Please create one at https://www.xbox.com/live");
        ERROR_MESSAGES.put(XO_E_ACCOUNT_TERMS_OF_USE_NOT_ACCEPTED, "Your account has not accepted Xbox's Terms of Service. Please login at https://www.xbox.com/live and accept them.");
        ERROR_MESSAGES.put(XO_E_ACCOUNT_COUNTRY_NOT_AUTHORIZED, "Your account is from a country where Xbox Live is not available/banned.");
        ERROR_MESSAGES.put(XO_E_ACCOUNT_AGE_VERIFICATION_REQUIRED, "Your account requires proof of age. Please login to https://login.live.com/login.srf and provide proof of age.");
        ERROR_MESSAGES.put(XO_E_ACCOUNT_CURFEW, "Your account has reached the its limit for playtime. Your account has been blocked from logging in.");
        ERROR_MESSAGES.put(XO_E_ACCOUNT_CHILD_NOT_IN_FAMILY, "Your account is a child (under 18) and cannot proceed unless the account is added to a Family by an adult.");
    }

    private final long errorCode;

    public XblRequestException(final HttpResponse response, final long errorCode) {
        super(response, ERRORS.getOrDefault(errorCode, String.valueOf(errorCode)), ERROR_MESSAGES.getOrDefault(errorCode, "An unknown error occurred"));
        this.errorCode = errorCode;
    }

}
