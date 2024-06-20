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

import net.lenni0451.commons.httpclient.HttpClient;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.step.AbstractStep;

public class StepRefreshTokenMsaCode extends MsaCodeStep<StepRefreshTokenMsaCode.RefreshToken> {

    private final StepMsaToken stepMsaToken = new StepMsaToken(null);
    private final ApplicationDetails applicationDetails;

    public StepRefreshTokenMsaCode(final ApplicationDetails applicationDetails) {
        super(null);

        this.applicationDetails = applicationDetails;
    }

    @Override
    public MsaCode applyStep(final HttpClient httpClient, final RefreshToken refreshToken) throws Exception {
        MinecraftAuth.LOGGER.info("Using externally supplied refresh token...");

        if (refreshToken == null) {
            throw new IllegalStateException("Missing StepRefreshTokenMsaCode.RefreshToken input");
        }

        final MsaCode msaCode = new MsaCode(null, this.applicationDetails);
        msaCode.msaToken = new StepMsaToken.MsaToken(0, null, refreshToken.refreshToken, msaCode);
        msaCode.msaToken = this.stepMsaToken.refresh(httpClient, msaCode.msaToken);
        return msaCode;
    }

    public static class RefreshToken extends AbstractStep.InitialInput {

        private final String refreshToken;

        public RefreshToken(final String refreshToken) {
            this.refreshToken = refreshToken;
        }

    }

}
