/*
 * This file is part of MinecraftAuth - https://github.com/RaphiMC/MinecraftAuth
 * Copyright (C) 2023 RK_01/RaphiMC and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.raphimc.mcauth.step;

import org.apache.http.client.HttpClient;

import java.lang.reflect.ParameterizedType;

public abstract class SameInputOptionalMergeStep<I1 extends AbstractStep.StepResult<?>, I2 extends AbstractStep.StepResult<?>, I extends AbstractStep.StepResult<?>, O extends OptionalMergeStep.StepResult<?, ?>> extends OptionalMergeStep<I1, I2, O> {

    public SameInputOptionalMergeStep(final AbstractStep<I, I1> prevStep1, final AbstractStep<I, I2> prevStep2) {
        super(prevStep1, prevStep2);

        if (this.prevStep2 != null && !((ParameterizedType) this.prevStep.getClass().getGenericSuperclass()).getActualTypeArguments()[0].equals(((ParameterizedType) this.prevStep2.getClass().getGenericSuperclass()).getActualTypeArguments()[0])) {
            throw new IllegalStateException("Steps do not take the same input");
        }
    }

    @Override
    public O refresh(final HttpClient httpClient, final O result) throws Exception {
        final I1 prevResult1 = this.prevStep.refresh(httpClient, result != null ? (I1) result.prevResult() : null);
        I2 prevResult2;
        if (this.prevStep2 != null && result == null) {
            prevResult2 = ((AbstractStep<I, I2>) this.prevStep2).applyStep(httpClient, (I) prevResult1.prevResult());
        } else {
            prevResult2 = this.prevStep2 != null ? this.prevStep2.refresh(httpClient, (I2) result.prevResult2()) : null;
        }
        return this.applyStep(httpClient, prevResult1, prevResult2);
    }

}
