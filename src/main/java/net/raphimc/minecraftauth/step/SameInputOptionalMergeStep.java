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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.http.client.HttpClient;

import java.lang.reflect.ParameterizedType;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class SameInputOptionalMergeStep<I1 extends AbstractStep.StepResult<?>, I2 extends AbstractStep.StepResult<?>, I extends AbstractStep.StepResult<?>, O extends OptionalMergeStep.StepResult<?, ?>> extends OptionalMergeStep<I1, I2, O> {

    public SameInputOptionalMergeStep(final AbstractStep<I, I1> prevStep1, final AbstractStep<I, I2> prevStep2) {
        super(prevStep1, prevStep2);

        if (this.prevStep2 != null && !((ParameterizedType) this.prevStep.getClass().getGenericSuperclass()).getActualTypeArguments()[0].equals(((ParameterizedType) this.prevStep2.getClass().getGenericSuperclass()).getActualTypeArguments()[0])) {
            throw new IllegalStateException("Steps do not take the same input");
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
        final I2 prevResult2 = this.prevStep2 != null ? ((AbstractStep<I, I2>) this.prevStep2).applyStep(httpClient, (I) prevResult1.prevResult()) : null;
        return this.applyStep(httpClient, prevResult1, prevResult2);
    }

    @Override
    public O fromJson(final JsonObject json) {
        if (json.has("shared")) {
            final JsonObject shared = json.getAsJsonObject("shared");
            json.remove("shared");
            for (Map.Entry<String, JsonElement> entry : shared.entrySet()) {
                json.asMap().values().stream()
                        .filter(JsonElement::isJsonObject)
                        .map(JsonElement::getAsJsonObject)
                        .forEach(jsonObject -> jsonObject.add(entry.getKey(), entry.getValue()));
            }
        }

        return this.fromDeduplicatedJson(json);
    }

    protected abstract O fromDeduplicatedJson(final JsonObject json);

    public abstract static class StepResult<P1 extends AbstractStep.StepResult<?>, P2 extends AbstractStep.StepResult<?>> extends OptionalMergeStep.StepResult<P1, P2> {

        @Override
        public JsonObject toJson() {
            final JsonObject json = this._toJson();

            final Map<String, JsonElement> shared = json.asMap().values().stream()
                    .filter(JsonElement::isJsonObject)
                    .map(JsonElement::getAsJsonObject)
                    .map(JsonObject::entrySet)
                    .flatMap(Set::stream)
                    .collect(Collectors.groupingBy(Function.identity(), Collectors.counting())).entrySet().stream()
                    .filter(entry -> entry.getValue() > 1)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            json.asMap().values().stream()
                    .filter(JsonElement::isJsonObject)
                    .map(JsonElement::getAsJsonObject)
                    .forEach(jsonObject -> jsonObject.entrySet().removeIf(entry -> shared.containsKey(entry.getKey())));

            final JsonObject sharedObj = new JsonObject();
            shared.forEach(sharedObj::add);
            json.add("shared", sharedObj);

            return json;
        }

        protected abstract JsonObject _toJson();

    }

}
