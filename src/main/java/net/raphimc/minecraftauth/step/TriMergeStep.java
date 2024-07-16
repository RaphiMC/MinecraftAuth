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

import net.lenni0451.commons.httpclient.HttpClient;
import net.raphimc.minecraftauth.util.logging.ILogger;

public abstract class TriMergeStep<I1 extends AbstractStep.StepResult<?>, I2 extends AbstractStep.StepResult<?>, I3 extends AbstractStep.StepResult<?>, O extends TriMergeStep.StepResult<I1, I2, I3>> extends BiMergeStep<I1, I2, O> {

    protected final AbstractStep<?, I3> prevStep3;

    public TriMergeStep(final String name, final AbstractStep<?, I1> prevStep1, final AbstractStep<?, I2> prevStep2, final AbstractStep<?, I3> prevStep3) {
        super(name, prevStep1, prevStep2);

        this.prevStep3 = prevStep3;
    }

    @Override
    public final O execute(final ILogger logger, final HttpClient httpClient, final I1 prevResult1, final I2 prevResult2) throws Exception {
        return this.execute(logger, httpClient, prevResult1, prevResult2, null);
    }

    public abstract O execute(final ILogger logger, final HttpClient httpClient, final I1 prevResult1, final I2 prevResult2, final I3 prevResult3) throws Exception;

    @Override
    public O refresh(final ILogger logger, final HttpClient httpClient, final O result) throws Exception {
        if (!result.isExpiredOrOutdated()) {
            return result;
        }

        final I1 prevResult1 = this.prevStep.refresh(logger, httpClient, result.prevResult());
        final I2 prevResult2 = this.prevStep2 != null ? this.prevStep2.refresh(logger, httpClient, result.prevResult2()) : null;
        final I3 prevResult3 = this.prevStep3 != null ? this.prevStep3.refresh(logger, httpClient, result.prevResult3()) : null;
        return this.execute(logger, httpClient, prevResult1, prevResult2, prevResult3);
    }

    @Override
    public O getFromInput(final ILogger logger, final HttpClient httpClient, final InitialInput input) throws Exception {
        final I1 prevResult1 = this.prevStep.getFromInput(logger, httpClient, input);
        final I2 prevResult2 = this.prevStep2 != null ? this.prevStep2.getFromInput(logger, httpClient, input) : null;
        final I3 prevResult3 = this.prevStep3 != null ? this.prevStep3.getFromInput(logger, httpClient, input) : null;
        return this.execute(logger, httpClient, prevResult1, prevResult2, prevResult3);
    }

    public abstract static class StepResult<P1 extends AbstractStep.StepResult<?>, P2 extends AbstractStep.StepResult<?>, P3 extends AbstractStep.StepResult<?>> extends BiMergeStep.StepResult<P1, P2> {

        protected abstract P3 prevResult3();

        @Override
        public boolean isExpired() {
            return super.isExpired() || (this.prevResult3() != null && this.prevResult3().isExpired());
        }

        @Override
        public boolean isExpiredOrOutdated() {
            return super.isExpiredOrOutdated() || (this.prevResult3() != null && this.prevResult3().isExpiredOrOutdated());
        }

    }

}
