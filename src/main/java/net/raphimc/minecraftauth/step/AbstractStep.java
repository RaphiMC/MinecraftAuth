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
package net.raphimc.minecraftauth.step;

import com.google.gson.JsonObject;
import net.lenni0451.commons.httpclient.HttpClient;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.util.logging.ILogger;

public abstract class AbstractStep<I extends AbstractStep.StepResult<?>, O extends AbstractStep.StepResult<?>> {

    public final String name;
    protected final AbstractStep<?, I> prevStep;

    public AbstractStep(final String name, final AbstractStep<?, I> prevStep) {
        this.name = name;
        this.prevStep = prevStep;
    }

    public final O applyStep(final HttpClient httpClient, final I prevResult) throws Exception {
        return this.applyStep(MinecraftAuth.LOGGER, httpClient, prevResult);
    }

    public final O refresh(final HttpClient httpClient, final O result) throws Exception {
        return this.refresh(MinecraftAuth.LOGGER, httpClient, result);
    }

    public final O getFromInput(final HttpClient httpClient, final InitialInput input) throws Exception {
        return this.getFromInput(MinecraftAuth.LOGGER, httpClient, input);
    }

    public abstract O applyStep(final ILogger logger, final HttpClient httpClient, final I prevResult) throws Exception;

    public O refresh(final ILogger logger, final HttpClient httpClient, final O result) throws Exception {
        if (!result.isExpired()) {
            return result;
        }

        return this.applyStep(logger, httpClient, this.prevStep != null ? this.prevStep.refresh(logger, httpClient, (I) result.prevResult()) : null);
    }

    public O getFromInput(final ILogger logger, final HttpClient httpClient, final InitialInput input) throws Exception {
        return this.applyStep(logger, httpClient, this.prevStep != null ? this.prevStep.getFromInput(logger, httpClient, input) : (I) input);
    }

    public abstract O fromJson(final JsonObject json);

    public abstract JsonObject toJson(final O result);

    public abstract static class StepResult<P extends StepResult<?>> {

        protected abstract P prevResult();

        public boolean isExpired() {
            return true;
        }

    }

    public abstract static class FirstStepResult extends StepResult<StepResult<?>> {

        @Override
        protected final StepResult<?> prevResult() {
            return null;
        }

    }

    public abstract static class InitialInput extends StepResult<StepResult<?>> {

        @Override
        protected final StepResult<?> prevResult() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isExpired() {
            throw new UnsupportedOperationException();
        }

    }

}
