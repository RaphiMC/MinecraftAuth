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
package net.raphimc.minecraftauth.step.java.session;

import com.google.gson.JsonObject;
import lombok.EqualsAndHashCode;
import lombok.Value;
import net.raphimc.minecraftauth.step.AbstractStep;
import net.raphimc.minecraftauth.step.OptionalMergeStep;
import net.raphimc.minecraftauth.step.SameInputOptionalMergeStep;
import net.raphimc.minecraftauth.step.java.StepMCProfile;
import net.raphimc.minecraftauth.step.java.StepMCToken;
import net.raphimc.minecraftauth.step.java.StepPlayerCertificates;
import org.apache.http.client.HttpClient;

public class StepFullJavaSession extends SameInputOptionalMergeStep<StepMCProfile.MCProfile, StepPlayerCertificates.PlayerCertificates, StepMCToken.MCToken, StepFullJavaSession.FullJavaSession> {

    public StepFullJavaSession(final AbstractStep<StepMCToken.MCToken, StepMCProfile.MCProfile> prevStep1, final AbstractStep<StepMCToken.MCToken, StepPlayerCertificates.PlayerCertificates> prevStep2) {
        super("fullJavaSession", prevStep1, prevStep2);
    }

    @Override
    public FullJavaSession applyStep(final HttpClient httpClient, final StepMCProfile.MCProfile mcProfile, final StepPlayerCertificates.PlayerCertificates playerCertificates) throws Exception {
        return new FullJavaSession(mcProfile, playerCertificates);
    }

    @Override
    protected FullJavaSession fromDeduplicatedJson(final JsonObject json) {
        final StepMCProfile.MCProfile mcProfile = this.prevStep != null ? this.prevStep.fromJson(json.getAsJsonObject(this.prevStep.name)) : null;
        final StepPlayerCertificates.PlayerCertificates playerCertificates = this.prevStep2 != null ? this.prevStep2.fromJson(json.getAsJsonObject(this.prevStep2.name)) : null;
        return new FullJavaSession(mcProfile, playerCertificates);
    }

    @Override
    protected JsonObject toRawJson(final FullJavaSession fullJavaSession) {
        final JsonObject json = new JsonObject();
        if (this.prevStep != null) json.add(this.prevStep.name, this.prevStep.toJson(fullJavaSession.mcProfile));
        if (this.prevStep2 != null) json.add(this.prevStep2.name, this.prevStep2.toJson(fullJavaSession.playerCertificates));
        return json;
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class FullJavaSession extends OptionalMergeStep.StepResult<StepMCProfile.MCProfile, StepPlayerCertificates.PlayerCertificates> {

        StepMCProfile.MCProfile mcProfile;
        StepPlayerCertificates.PlayerCertificates playerCertificates;

        @Override
        protected StepMCProfile.MCProfile prevResult() {
            return this.mcProfile;
        }

        @Override
        protected StepPlayerCertificates.PlayerCertificates prevResult2() {
            return this.playerCertificates;
        }

    }

}
