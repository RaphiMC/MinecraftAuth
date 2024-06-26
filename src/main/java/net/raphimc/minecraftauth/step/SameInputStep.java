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
import net.raphimc.minecraftauth.util.logging.ILogger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public interface SameInputStep<I1 extends AbstractStep.StepResult<?>, O extends AbstractStep.StepResult<?>> {

    O fromRawJson(final JsonObject json);

    JsonObject toRawJson(final O result);

    default <I2 extends AbstractStep.StepResult<?>> I2 applySecondaryStepChain(final ILogger logger, final HttpClient httpClient, final I1 prevResult1, final List<AbstractStep<?, ?>> steps1UntilSameInput, final List<AbstractStep<?, ?>> steps2UntilSameInput) throws Exception {
        if (steps2UntilSameInput.isEmpty()) {
            return null;
        }

        AbstractStep.StepResult<?> prevResult = prevResult1;
        for (int i = 0; i < steps1UntilSameInput.size() - 1; i++) {
            prevResult = prevResult.prevResult();
        }

        for (int i = 1; i < steps2UntilSameInput.size(); i++) {
            final AbstractStep step = steps2UntilSameInput.get(i);
            prevResult = step.applyStep(logger, httpClient, prevResult);
        }

        return (I2) prevResult;
    }

    default <I2 extends AbstractStep.StepResult<?>> I2 refreshSecondaryStepChain(final ILogger logger, final HttpClient httpClient, final I1 prevResult1, AbstractStep.StepResult<?> prevResult2, final List<AbstractStep<?, ?>> steps1UntilSameInput, final List<AbstractStep<?, ?>> steps2UntilSameInput) throws Exception {
        int count = 1;
        while (count < steps2UntilSameInput.size()) {
            if (prevResult2.isExpired()) {
                prevResult2 = prevResult2.prevResult();
                count++;
            } else {
                break;
            }
        }

        if (count == steps2UntilSameInput.size()) {
            AbstractStep.StepResult<?> prevResult = prevResult1;
            for (int i = 0; i < steps1UntilSameInput.size() - 1; i++) {
                prevResult = prevResult.prevResult();
            }
            prevResult2 = prevResult;
        }

        for (int i = steps2UntilSameInput.size() - count + 1; i < steps2UntilSameInput.size(); i++) {
            final AbstractStep step = steps2UntilSameInput.get(i);
            prevResult2 = step.applyStep(logger, httpClient, prevResult2);
        }

        return (I2) prevResult2;
    }

    default void removeDuplicateStepResultsFromJson(final JsonObject json, final List<AbstractStep<?, ?>> steps2UntilSameInput) {
        if (!steps2UntilSameInput.isEmpty()) {
            JsonObject resultJson = json;
            for (int i = steps2UntilSameInput.size() - 1; i >= 0; i--) {
                final AbstractStep<?, ?> step = steps2UntilSameInput.get(i);
                if (i == 0) {
                    resultJson.remove(step.name);
                    break;
                }

                resultJson = resultJson.getAsJsonObject(step.name);
            }
        }
    }

    default void insertDuplicateStepResultsIntoJson(final JsonObject json, final List<AbstractStep<?, ?>> steps1UntilSameInput, final List<AbstractStep<?, ?>> steps2UntilSameInput) {
        if (!steps2UntilSameInput.isEmpty()) {
            String targetName = null;
            JsonObject step2Json = json;
            for (int i = steps2UntilSameInput.size() - 1; i >= 0; i--) {
                final AbstractStep<?, ?> step = steps2UntilSameInput.get(i);
                if (i == 0) {
                    targetName = step.name;
                    break;
                }

                step2Json = step2Json.getAsJsonObject(step.name);
            }

            JsonObject step1Json = json;
            for (int i = steps1UntilSameInput.size() - 1; i >= 0; i--) {
                final AbstractStep<?, ?> step = steps1UntilSameInput.get(i);
                step1Json = step1Json.getAsJsonObject(step.name);
            }
            step2Json.add(targetName, step1Json);
        }
    }

    default List<AbstractStep<?, ?>> findCommonStep(final AbstractStep<?, ?> step1, final AbstractStep<?, ?> step2) {
        final List<AbstractStep<?, ?>> stepsUntilSameInput = new ArrayList<>();

        stepsUntilSameInput.add(step1);
        AbstractStep<?, ?> step1Temp = step1;
        while ((step1Temp = step1Temp.prevStep) != null) {
            stepsUntilSameInput.add(step1Temp);

            AbstractStep<?, ?> step2Temp = step2;
            while ((step2Temp = step2Temp.prevStep) != null) {
                if (step1Temp == step2Temp) {
                    Collections.reverse(stepsUntilSameInput);
                    return stepsUntilSameInput;
                }
            }
        }

        throw new IllegalStateException("Cannot find a common step");
    }

}
