/*
 * This file is part of MinecraftAuth - https://github.com/RaphiMC/MinecraftAuth
 * Copyright (C) 2023 RK_01/RaphiMC and contributors
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
import org.apache.http.client.HttpClient;

public abstract class AbstractStep<I extends AbstractStep.StepResult<?>, O extends AbstractStep.StepResult<?>> {

    protected final AbstractStep<?, I> prevStep;

    public AbstractStep(final AbstractStep<?, I> prevStep) {
        this.prevStep = prevStep;
    }

    public abstract O applyStep(final HttpClient httpClient, final I prevResult) throws Exception;

    public O refresh(final HttpClient httpClient, final O result) throws Exception {
        return this.applyStep(httpClient, this.prevStep != null ? this.prevStep.refresh(httpClient, (I) result.prevResult()) : null);
    }

    public O getFromInput(final HttpClient httpClient, final Object input) throws Exception {
        return this.applyStep(httpClient, this.prevStep != null ? this.prevStep.getFromInput(httpClient, input) : (I) input);
    }

    public abstract O fromJson(final JsonObject json);

    public abstract JsonObject toJson(final O result);

    public abstract static class StepResult<P extends StepResult<?>> {

        protected abstract P prevResult();

        public boolean isExpired() {
            return true;
        }

    }

    public abstract static class InitialInput extends StepResult<StepResult<?>> {

        @Override
        protected StepResult<?> prevResult() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isExpired() {
            throw new UnsupportedOperationException();
        }

    }

}
