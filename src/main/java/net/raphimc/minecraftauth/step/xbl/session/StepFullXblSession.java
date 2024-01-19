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
package net.raphimc.minecraftauth.step.xbl.session;

import com.google.gson.JsonObject;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.experimental.NonFinal;
import net.lenni0451.commons.httpclient.HttpClient;
import net.raphimc.minecraftauth.step.AbstractStep;
import net.raphimc.minecraftauth.step.BiMergeStep;
import net.raphimc.minecraftauth.step.SameInputBiMergeStep;
import net.raphimc.minecraftauth.step.xbl.StepXblTitleToken;
import net.raphimc.minecraftauth.step.xbl.StepXblUserToken;

public class StepFullXblSession extends SameInputBiMergeStep<StepXblUserToken.XblUserToken, StepXblTitleToken.XblTitleToken, StepFullXblSession.FullXblSession> {

    public StepFullXblSession(final AbstractStep<?, StepXblUserToken.XblUserToken> prevStep1, final AbstractStep<?, StepXblTitleToken.XblTitleToken> prevStep2) {
        super("fullXblSession", prevStep1, prevStep2);
    }

    @Override
    public FullXblSession applyStep(final HttpClient httpClient, final StepXblUserToken.XblUserToken xblUserToken, final StepXblTitleToken.XblTitleToken xblTitleToken) throws Exception {
        return new FullXblSession(xblUserToken, xblTitleToken);
    }

    @Override
    public FullXblSession fromRawJson(final JsonObject json) {
        final StepXblUserToken.XblUserToken xblUserToken = this.prevStep != null ? this.prevStep.fromJson(json.getAsJsonObject(this.prevStep.name)) : null;
        final StepXblTitleToken.XblTitleToken xblTitleToken = this.prevStep2 != null ? this.prevStep2.fromJson(json.getAsJsonObject(this.prevStep2.name)) : null;
        return new FullXblSession(xblUserToken, xblTitleToken);
    }

    @Override
    public JsonObject toRawJson(final FullXblSession fullXblSession) {
        final JsonObject json = new JsonObject();
        if (this.prevStep != null) json.add(this.prevStep.name, this.prevStep.toJson(fullXblSession.xblUserToken));
        if (this.prevStep2 != null) json.add(this.prevStep2.name, this.prevStep2.toJson(fullXblSession.xblTitleToken));
        return json;
    }

    @Value
    @NonFinal
    @AllArgsConstructor
    @EqualsAndHashCode(callSuper = false)
    public static class FullXblSession extends BiMergeStep.StepResult<StepXblUserToken.XblUserToken, StepXblTitleToken.XblTitleToken> {

        StepXblUserToken.XblUserToken xblUserToken;
        StepXblTitleToken.XblTitleToken xblTitleToken;

        public FullXblSession(final FullXblSession fullXblSession) {
            this.xblUserToken = fullXblSession.xblUserToken;
            this.xblTitleToken = fullXblSession.xblTitleToken;
        }

        @Override
        protected StepXblUserToken.XblUserToken prevResult() {
            return this.xblUserToken;
        }

        @Override
        protected StepXblTitleToken.XblTitleToken prevResult2() {
            return this.xblTitleToken;
        }

    }

}
