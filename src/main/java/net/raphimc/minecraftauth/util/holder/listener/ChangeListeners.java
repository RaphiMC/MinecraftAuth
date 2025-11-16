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
package net.raphimc.minecraftauth.util.holder.listener;

import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ChangeListeners {

    private final List<ChangeListener> changeListeners = new ArrayList<>();

    public synchronized void add(final ChangeListener listener) {
        this.changeListeners.add(listener);
    }

    public synchronized void add(final BasicChangeListener listener) {
        this.changeListeners.add(listener);
    }

    public synchronized boolean remove(final ChangeListener listener) {
        return this.changeListeners.remove(listener);
    }

    public synchronized boolean remove(final BasicChangeListener listener) {
        return this.changeListeners.remove(listener);
    }

    @ApiStatus.Internal
    public synchronized <T> void invoke(final T oldValue, final T newValue) {
        if (!Objects.equals(oldValue, newValue)) {
            for (ChangeListener listener : this.changeListeners) {
                listener.onChange(oldValue, newValue);
            }
        }
    }

}
