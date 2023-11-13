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

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class SameInputOptionalMergeStep<I1 extends AbstractStep.StepResult<?>, I2 extends AbstractStep.StepResult<?>, I extends AbstractStep.StepResult<?>, O extends OptionalMergeStep.StepResult<?, ?>> extends OptionalMergeStep<I1, I2, O> {

    private final List<AbstractStep<?, ?>> stepsUntilSameInput = new ArrayList<>();
    private final int step1SameInputOffset;

    public SameInputOptionalMergeStep(final AbstractStep<I, I1> prevStep1, final AbstractStep<I, I2> prevStep2) {
        super(prevStep1, prevStep2);

        STEP2_CHECK:
        if (this.prevStep2 != null) {
            if (!((ParameterizedType) this.prevStep.getClass().getGenericSuperclass()).getActualTypeArguments()[0].equals(((ParameterizedType) this.prevStep2.getClass().getGenericSuperclass()).getActualTypeArguments()[0])) {
                throw new IllegalStateException("Steps do not take the same input");
            }

            this.stepsUntilSameInput.add(this.prevStep2);
            AbstractStep<?, ?> step2 = this.prevStep2;
            while ((step2 = step2.prevStep) != null) {
                this.stepsUntilSameInput.add(step2);
                AbstractStep<?, ?> step1 = this.prevStep;
                int steps1Offset = 0;
                while ((step1 = step1.prevStep) != null) {
                    steps1Offset++;
                    if (step2 == step1) {
                        Collections.reverse(this.stepsUntilSameInput);
                        this.stepsUntilSameInput.remove(0);
                        this.step1SameInputOffset = steps1Offset;
                        break STEP2_CHECK;
                    }
                }
            }

            throw new IllegalStateException("Cannot find a common step");
        } else {
            this.step1SameInputOffset = 0;
        }

        System.out.println(this.step1SameInputOffset + " " + this.stepsUntilSameInput.size() + " " + getClass().getSimpleName());
    }

    @Override
    public O refresh(final HttpClient httpClient, final O result) throws Exception {
        final I1 prevResult1 = this.prevStep.refresh(httpClient, (I1) result.prevResult());
        I2 prevResult2 = this.prevStep2 != null ? this.prevStep2.refresh(httpClient, (I2) result.prevResult2()) : null;
        return this.applyStep(httpClient, prevResult1, prevResult2);
    }

    @Override
    public O getFromInput(final HttpClient httpClient, final Object input) throws Exception {
        final I1 prevResult1 = this.prevStep.getFromInput(httpClient, input);

        if (!this.stepsUntilSameInput.isEmpty()) {
            AbstractStep.StepResult<?> result2 = prevResult1;
            for (int i = 0; i < this.step1SameInputOffset; i++) {
                result2 = result2.prevResult();
            }

            for (AbstractStep step : this.stepsUntilSameInput) {
                result2 = step.applyStep(httpClient, result2);
            }

            return this.applyStep(httpClient, prevResult1, (I2) result2);
        } else {
            return this.applyStep(httpClient, prevResult1, null);
        }
    }

    @Override
    public O fromJson(final JsonObject json) {
        return this.fromDeduplicatedJson(json);
    }

    @Override
    public JsonObject toJson(final O result) {
        return this.toRawJson(result);
    }

    protected abstract O fromDeduplicatedJson(final JsonObject json);

    protected abstract JsonObject toRawJson(final O result);

}
