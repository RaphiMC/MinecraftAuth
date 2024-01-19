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
package net.raphimc.minecraftauth.step.xbl.adapter;

import com.google.gson.JsonObject;
import net.lenni0451.commons.httpclient.HttpClient;
import net.raphimc.minecraftauth.step.AbstractStep;
import net.raphimc.minecraftauth.step.xbl.StepXblXstsToken;
import net.raphimc.minecraftauth.step.xbl.session.StepFullXblSession;

public class StepXblXstsToFullXblSession extends AbstractStep<StepXblXstsToken.XblXsts<?>, StepFullXblSession.FullXblSession> {

    public StepXblXstsToFullXblSession(final AbstractStep<?, ? extends StepXblXstsToken.XblXsts<?>> prevStep) {
        super("xblXstsToFullXblSession", (AbstractStep<?, StepXblXstsToken.XblXsts<?>>) prevStep);
    }

    @Override
    public StepFullXblSession.FullXblSession applyStep(final HttpClient httpClient, final StepXblXstsToken.XblXsts<?> xblXsts) throws Exception {
        return new FullXblSessionWrapper(xblXsts);
    }

    @Override
    public StepFullXblSession.FullXblSession refresh(final HttpClient httpClient, final StepFullXblSession.FullXblSession fullXblSession) throws Exception {
        final FullXblSessionWrapper fullXblSessionWrapper = (FullXblSessionWrapper) fullXblSession;
        return new FullXblSessionWrapper(this.prevStep.refresh(httpClient, fullXblSessionWrapper.xblXsts));
    }

    @Override
    public StepFullXblSession.FullXblSession fromJson(final JsonObject json) {
        return new FullXblSessionWrapper(this.prevStep.fromJson(json.getAsJsonObject(this.prevStep.name)));
    }

    @Override
    public JsonObject toJson(final StepFullXblSession.FullXblSession fullXblSession) {
        final FullXblSessionWrapper fullXblSessionWrapper = (FullXblSessionWrapper) fullXblSession;
        final JsonObject json = new JsonObject();
        json.add(this.prevStep.name, this.prevStep.toJson(fullXblSessionWrapper.xblXsts));
        return json;
    }

    private static class FullXblSessionWrapper extends StepFullXblSession.FullXblSession {

        private final StepXblXstsToken.XblXsts<?> xblXsts;

        public FullXblSessionWrapper(final StepXblXstsToken.XblXsts<?> xblXsts) {
            super(xblXsts.getFullXblSession());

            this.xblXsts = xblXsts;
        }

    }

}
