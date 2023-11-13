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

    private final List<AbstractStep<?, ?>> steps1UntilSameInput = new ArrayList<>();
    private final List<AbstractStep<?, ?>> steps2UntilSameInput = new ArrayList<>();

    public SameInputOptionalMergeStep(final String name, final AbstractStep<I, I1> prevStep1, final AbstractStep<I, I2> prevStep2) {
        super(name, prevStep1, prevStep2);

        STEP2_CHECK:
        if (this.prevStep2 != null) {
            if (!((ParameterizedType) this.prevStep.getClass().getGenericSuperclass()).getActualTypeArguments()[0].equals(((ParameterizedType) this.prevStep2.getClass().getGenericSuperclass()).getActualTypeArguments()[0])) {
                throw new IllegalStateException("Steps do not take the same input");
            }

            this.steps2UntilSameInput.add(this.prevStep2);
            AbstractStep<?, ?> step2 = this.prevStep2;
            while ((step2 = step2.prevStep) != null) {
                this.steps2UntilSameInput.add(step2);
                this.steps1UntilSameInput.clear();
                this.steps1UntilSameInput.add(this.prevStep);

                AbstractStep<?, ?> step1 = this.prevStep;
                while ((step1 = step1.prevStep) != null) {
                    this.steps1UntilSameInput.add(step1);
                    if (step2 == step1) {
                        Collections.reverse(this.steps2UntilSameInput);
                        Collections.reverse(this.steps1UntilSameInput);
                        break STEP2_CHECK;
                    }
                }
            }

            throw new IllegalStateException("Cannot find a common step");
        }
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

        if (!this.steps2UntilSameInput.isEmpty()) {
            AbstractStep.StepResult<?> result2 = prevResult1;
            for (int i = 0; i < this.steps1UntilSameInput.size() - 1; i++) {
                result2 = result2.prevResult();
            }

            for (int i = 1; i < this.steps2UntilSameInput.size(); i++) {
                final AbstractStep step = this.steps2UntilSameInput.get(i);
                result2 = step.applyStep(httpClient, result2);
            }

            return this.applyStep(httpClient, prevResult1, (I2) result2);
        } else {
            return this.applyStep(httpClient, prevResult1, null);
        }
    }

    @Override
    public final JsonObject toJson(final O result) {
        final JsonObject json = this.toRawJson(result);

        if (!this.steps2UntilSameInput.isEmpty()) {
            JsonObject resultJson = json;
            for (int i = this.steps2UntilSameInput.size() - 1; i >= 1; i--) {
                final AbstractStep<?, ?> step = this.steps2UntilSameInput.get(i);
                if (i == 1) {
                    resultJson.remove(step.name);
                    break;
                }

                resultJson = resultJson.getAsJsonObject(step.name);
            }
        }

        return json;
    }

    @Override
    public final O fromJson(final JsonObject json) {
        if (!this.steps2UntilSameInput.isEmpty()) {
            String targetName = null;
            JsonObject step2Json = json;
            for (int i = this.steps2UntilSameInput.size() - 1; i >= 1; i--) {
                final AbstractStep<?, ?> step = this.steps2UntilSameInput.get(i);
                if (i == 1) {
                    targetName = step.name;
                    break;
                }

                step2Json = step2Json.getAsJsonObject(step.name);
            }

            JsonObject step1Json = json;
            for (int i = this.steps1UntilSameInput.size() - 1; i >= 0; i--) {
                final AbstractStep<?, ?> step = this.steps1UntilSameInput.get(i);
                step1Json = step1Json.getAsJsonObject(step.name);
            }
            step2Json.add(targetName, step1Json);
        }

        return this.fromDeduplicatedJson(json);
    }

    protected abstract O fromDeduplicatedJson(final JsonObject json);

    protected abstract JsonObject toRawJson(final O result);

}
