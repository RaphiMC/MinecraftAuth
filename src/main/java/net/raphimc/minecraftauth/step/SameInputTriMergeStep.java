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

public abstract class SameInputTriMergeStep<I1 extends AbstractStep.StepResult<?>, I2 extends AbstractStep.StepResult<?>, I3 extends AbstractStep.StepResult<?>, O extends TriMergeStep.StepResult<I1, I2, I3>> extends TriMergeStep<I1, I2, I3, O> implements SameInputStep<I1, O> {

    protected final List<AbstractStep<?, ?>> steps1UntilSameInput = new ArrayList<>();
    protected final List<AbstractStep<?, ?>> steps2UntilSameInput = new ArrayList<>();
    protected final List<AbstractStep<?, ?>> steps3UntilSameInput = new ArrayList<>();

    public SameInputTriMergeStep(final String name, final AbstractStep<?, I1> prevStep1, final AbstractStep<?, I2> prevStep2, final AbstractStep<?, I3> prevStep3) {
        super(name, prevStep1, prevStep2, prevStep3);

        AbstractStep<?, ?> secondaryStep = null;
        if (this.prevStep2 != null) {
            secondaryStep = this.prevStep2;
            this.steps2UntilSameInput.addAll(this.findCommonStep(this.prevStep2, this.prevStep));
        }
        if (this.prevStep3 != null) {
            secondaryStep = this.prevStep3;
            this.steps3UntilSameInput.addAll(this.findCommonStep(this.prevStep3, this.prevStep));
        }
        if (secondaryStep != null) {
            this.steps1UntilSameInput.addAll(this.findCommonStep(this.prevStep, secondaryStep));
        }
    }

    @Override
    public O refresh(final ILogger logger, final HttpClient httpClient, final O result) throws Exception {
        if (!result.isExpired()) {
            return result;
        }

        final I1 prevResult1 = this.prevStep.refresh(logger, httpClient, result.prevResult());
        final I2 prevResult2 = this.refreshSecondaryStepChain(logger, httpClient, prevResult1, result.prevResult2(), this.steps1UntilSameInput, this.steps2UntilSameInput);
        final I3 prevResult3 = this.refreshSecondaryStepChain(logger, httpClient, prevResult1, result.prevResult3(), this.steps1UntilSameInput, this.steps3UntilSameInput);
        return this.applyStep(httpClient, prevResult1, prevResult2, prevResult3);
    }

    @Override
    public O getFromInput(final ILogger logger, final HttpClient httpClient, final InitialInput input) throws Exception {
        final I1 prevResult1 = this.prevStep.getFromInput(logger, httpClient, input);
        final I2 prevResult2 = this.applySecondaryStepChain(logger, httpClient, prevResult1, this.steps1UntilSameInput, this.steps2UntilSameInput);
        final I3 prevResult3 = this.applySecondaryStepChain(logger, httpClient, prevResult1, this.steps1UntilSameInput, this.steps3UntilSameInput);
        return this.applyStep(httpClient, prevResult1, prevResult2, prevResult3);
    }

    @Override
    public JsonObject toJson(final O result) {
        final JsonObject json = this.toRawJson(result);
        this.removeDuplicateStepResultsFromJson(json, this.steps2UntilSameInput);
        this.removeDuplicateStepResultsFromJson(json, this.steps3UntilSameInput);
        return json;
    }

    @Override
    public O fromJson(final JsonObject json) {
        this.insertDuplicateStepResultsIntoJson(json, this.steps1UntilSameInput, this.steps2UntilSameInput);
        this.insertDuplicateStepResultsIntoJson(json, this.steps1UntilSameInput, this.steps3UntilSameInput);
        return this.fromRawJson(json);
    }

}
