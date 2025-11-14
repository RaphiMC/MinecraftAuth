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
package net.raphimc.minecraftauth.util.holder;

import lombok.Getter;
import net.raphimc.minecraftauth.util.Expirable;
import net.raphimc.minecraftauth.util.holder.listener.ChangeListeners;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.Supplier;

/**
 * A thread-safe holder for an expirable value that can be refreshed as needed.
 *
 * @param <T> The type of the held value.
 */
public class Holder<T extends Expirable> {

    @Getter
    private final ChangeListeners changeListeners = new ChangeListeners();
    private final Supplier<T> supplier;
    private final Object lock;

    private T value;

    public Holder(final Supplier<T> supplier) {
        this(supplier, new Object());
    }

    public Holder(final Supplier<T> supplier, final Object lock) {
        this.supplier = supplier;
        this.lock = lock;
    }

    /**
     * Returns the currently cached value.<br>
     * This method does not perform any validation or refresh. It simply returns the last known value.<br>
     * This is suitable for scenarios where you want to access the value without triggering any network requests (Like in a UI).
     *
     * @return The cached value, which may be null or expired.
     */
    public T getCached() {
        return this.value;
    }

    /**
     * Returns the up-to-date value, refreshing it if necessary.<br>
     * If the cached value is null or expired, this method may send network requests to obtain a fresh or valid value.<br>
     * Use this method when you need to ensure that the value is current and valid (For example, before using it for authentication).
     *
     * @return The up-to-date value.
     */
    public T getUpToDate() {
        this.refreshIfExpired();
        return this.value;
    }

    /**
     * Checks if the holder currently has a non-null value.
     *
     * @return True if the holder has a non-null value, false otherwise.
     */
    public boolean hasValue() {
        return this.value != null;
    }

    /**
     * Refreshes the value if it is null or expired.
     *
     * @return True if a refresh was performed, false otherwise.
     */
    public boolean refreshIfExpired() {
        synchronized (this.lock) {
            if (this.value == null || this.value.isExpired()) {
                this.refresh();
                return true;
            }
            return false;
        }
    }

    /**
     * Forces a refresh of the value, regardless of its current state.
     *
     * @return The refreshed value.
     */
    public T refresh() {
        synchronized (this.lock) {
            this.set(this.supplier.get());
            return this.value;
        }
    }

    @ApiStatus.Internal
    public void set(final T value) {
        synchronized (this.lock) {
            final T oldValue = this.value;
            this.value = value;
            this.changeListeners.invoke(oldValue, value);
        }
    }

}
