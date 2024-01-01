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

import org.apache.http.client.HttpClient;

public abstract class BiMergeStep<I1 extends AbstractStep.StepResult<?>, I2 extends AbstractStep.StepResult<?>, O extends BiMergeStep.StepResult<I1, I2>> extends AbstractStep<I1, O> {

    protected final AbstractStep<?, I2> prevStep2;

    public BiMergeStep(final String name, final AbstractStep<?, I1> prevStep1, final AbstractStep<?, I2> prevStep2) {
        super(name, prevStep1);

        this.prevStep2 = prevStep2;
    }

    @Override
    public O applyStep(final HttpClient httpClient, final I1 prevResult) throws Exception {
        return this.applyStep(httpClient, prevResult, null);
    }

    public abstract O applyStep(final HttpClient httpClient, final I1 prevResult1, final I2 prevResult2) throws Exception;

    @Override
    public O refresh(final HttpClient httpClient, final O result) throws Exception {
        if (!result.isExpired()) {
            return result;
        }

        final I1 prevResult1 = this.prevStep.refresh(httpClient, result.prevResult());
        final I2 prevResult2 = this.prevStep2 != null ? this.prevStep2.refresh(httpClient, result.prevResult2()) : null;
        return this.applyStep(httpClient, prevResult1, prevResult2);
    }

    @Override
    public O getFromInput(final HttpClient httpClient, final Object input) throws Exception {
        final I1 prevResult1 = this.prevStep.getFromInput(httpClient, input);
        final I2 prevResult2 = this.prevStep2 != null ? this.prevStep2.getFromInput(httpClient, input) : null;
        return this.applyStep(httpClient, prevResult1, prevResult2);
    }

    public abstract static class StepResult<P1 extends AbstractStep.StepResult<?>, P2 extends AbstractStep.StepResult<?>> extends AbstractStep.StepResult<P1> {

        protected abstract P2 prevResult2();

        @Override
        public boolean isExpired() {
            return super.isExpired() || (this.prevResult2() != null && this.prevResult2().isExpired());
        }

    }

}
