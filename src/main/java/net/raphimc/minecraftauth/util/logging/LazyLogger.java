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

import net.raphimc.minecraftauth.step.AbstractStep;

import java.util.function.Supplier;

public class LazyLogger implements ILogger {

    private final Supplier<ILogger> loggerSupplier;
    private ILogger delegate;

    public LazyLogger(final Supplier<ILogger> loggerSupplier) {
        this.loggerSupplier = loggerSupplier;
    }

    @Override
    public void info(final AbstractStep<?, ?> step, final String message) {
        this.getDelegate().info(step, message);
    }

    @Override
    public void info(String message) {
        this.getDelegate().info(message);
    }

    @Override
    public void warn(final AbstractStep<?, ?> step, final String message) {
        this.getDelegate().warn(step, message);
    }

    @Override
    public void warn(String message) {
        this.getDelegate().warn(message);
    }

    @Override
    public void error(final AbstractStep<?, ?> step, final String message) {
        this.getDelegate().error(step, message);
    }

    @Override
    public void error(String message) {
        this.getDelegate().error(message);
    }

    private ILogger getDelegate() {
        if (this.delegate == null) {
            this.delegate = this.loggerSupplier.get();
        }
        return this.delegate;
    }

}
