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
package net.raphimc.minecraftauth.step.msa;

import com.google.gson.JsonObject;
import lombok.*;
import lombok.experimental.NonFinal;
import lombok.experimental.PackagePrivate;
import net.lenni0451.commons.httpclient.HttpClient;
import net.raphimc.minecraftauth.step.AbstractStep;
import net.raphimc.minecraftauth.util.JsonUtil;
import net.raphimc.minecraftauth.util.logging.ILogger;
import org.jetbrains.annotations.ApiStatus;

public abstract class MsaCodeStep<I extends AbstractStep.InitialInput> extends AbstractStep<I, MsaCodeStep.MsaCode> {

    public MsaCodeStep(final AbstractStep<?, I> prevStep) {
        super("msaCode", prevStep);
    }

    public MsaCodeStep(final ApplicationDetails applicationDetails) {
        super("msaCode", applicationDetails);
    }

    @Override
    public final MsaCodeStep.MsaCode refresh(final ILogger logger, final HttpClient httpClient, final MsaCode result) {
        throw new UnsupportedOperationException("Cannot refresh MsaCodeStep");
    }

    @Override
    public final MsaCode fromJson(final JsonObject json) {
        return new MsaCode(
                JsonUtil.getStringOr(json, "code", null)
        );
    }

    @Override
    public final JsonObject toJson(final MsaCode msaCode) {
        final JsonObject json = new JsonObject();
        json.addProperty("code", msaCode.code);
        return json;
    }

    @Value
    @RequiredArgsConstructor
    @EqualsAndHashCode(callSuper = false)
    public static class MsaCode extends AbstractStep.FirstStepResult {

        String code;

        @ApiStatus.Internal
        @Getter(AccessLevel.NONE)
        @Setter(AccessLevel.NONE)
        @PackagePrivate
        @NonFinal
        @ToString.Exclude
        @EqualsAndHashCode.Exclude
        StepMsaToken.MsaToken msaToken; // Used in device code flow

        @Override
        public boolean isExpired() {
            return true; // MsaCode can only be used one time and can't be refreshed
        }

    }

}
