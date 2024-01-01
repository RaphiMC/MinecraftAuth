/*
 * This file is part of MinecraftAuth - https://github.com/RaphiMC/MinecraftAuth
 * Copyright (C) 2022-2024 RK_01/RaphiMC and contributors
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

import java.util.UUID;

public class UuidUtil {

    private static final String UNDASHED_UUID_REGEX = "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)";
    private static final String DASHED_UUID_REGEX = "(\\p{XDigit}{8})-(\\p{XDigit}{4})-(\\p{XDigit}{4})-(\\p{XDigit}{4})-(\\p{XDigit}+)";

    public static UUID fromLenientString(final String s) {
        if (s == null) return null;

        return UUID.fromString(s.replaceFirst(UNDASHED_UUID_REGEX, "$1-$2-$3-$4-$5"));
    }

    public static boolean isUndashedUuid(final String s) {
        return s.matches(UNDASHED_UUID_REGEX);
    }

    public static boolean isDashedUuid(final String s) {
        return s.matches(DASHED_UUID_REGEX);
    }

}
