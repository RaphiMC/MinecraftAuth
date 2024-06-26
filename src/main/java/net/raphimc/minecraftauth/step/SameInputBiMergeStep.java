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
import java.util.List;

public abstract class SameInputBiMergeStep<I1 extends AbstractStep.StepResult<?>, I2 extends AbstractStep.StepResult<?>, O extends BiMergeStep.StepResult<I1, I2>> extends BiMergeStep<I1, I2, O> implements SameInputStep<I1, O> {

    protected final List<AbstractStep<?, ?>> steps1UntilSameInput = new ArrayList<>();
    protected final List<AbstractStep<?, ?>> steps2UntilSameInput = new ArrayList<>();

    public SameInputBiMergeStep(final String name, final AbstractStep<?, I1> prevStep1, final AbstractStep<?, I2> prevStep2) {
        super(name, prevStep1, prevStep2);

        if (this.prevStep2 != null) {
            this.steps1UntilSameInput.addAll(this.findCommonStep(this.prevStep, this.prevStep2));
            this.steps2UntilSameInput.addAll(this.findCommonStep(this.prevStep2, this.prevStep));
        }
    }

    @Override
    public O refresh(final ILogger logger, final HttpClient httpClient, final O result) throws Exception {
        if (!result.isExpired()) {
            return result;
        }

        final I1 prevResult1 = this.prevStep.refresh(logger, httpClient, result.prevResult());
        final I2 prevResult2 = this.refreshSecondaryStepChain(logger, httpClient, prevResult1, result.prevResult2(), this.steps1UntilSameInput, this.steps2UntilSameInput);
        return this.applyStep(httpClient, prevResult1, prevResult2);
    }

    @Override
    public O getFromInput(final ILogger logger, final HttpClient httpClient, final InitialInput input) throws Exception {
        final I1 prevResult1 = this.prevStep.getFromInput(logger, httpClient, input);
        final I2 prevResult2 = this.applySecondaryStepChain(logger, httpClient, prevResult1, this.steps1UntilSameInput, this.steps2UntilSameInput);
        return this.applyStep(httpClient, prevResult1, prevResult2);
    }

    @Override
    public JsonObject toJson(final O result) {
        final JsonObject json = this.toRawJson(result);
        this.removeDuplicateStepResultsFromJson(json, this.steps2UntilSameInput);
        return json;
    }

    @Override
    public O fromJson(final JsonObject json) {
        this.insertDuplicateStepResultsIntoJson(json, this.steps1UntilSameInput, this.steps2UntilSameInput);
        return this.fromRawJson(json);
    }

}
