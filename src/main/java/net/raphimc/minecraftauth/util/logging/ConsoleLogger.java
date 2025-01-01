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
package net.raphimc.minecraftauth.util.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Deprecated
public class ConsoleLogger implements ILogger {

    @Deprecated
    public static final Logger LOGGER = LoggerFactory.getLogger("MinecraftAuth");

    @Deprecated
    @Override
    public void info(String message) {
        LOGGER.info(message);
    }

    @Deprecated
    @Override
    public void warn(String message) {
        LOGGER.warn(message);
    }

    @Deprecated
    @Override
    public void error(String message) {
        LOGGER.error(message);
    }

}
