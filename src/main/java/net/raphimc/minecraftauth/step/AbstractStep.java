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
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.With;
import net.lenni0451.commons.httpclient.HttpClient;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.util.JsonUtil;
import net.raphimc.minecraftauth.util.OAuthEnvironment;
import net.raphimc.minecraftauth.util.UuidUtil;
import net.raphimc.minecraftauth.util.logging.ILogger;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractStep<I extends AbstractStep.StepResult<?>, O extends AbstractStep.StepResult<?>> {

    public final String name;
    public final ApplicationDetails applicationDetails;
    protected final AbstractStep<?, I> prevStep;

    public AbstractStep(final String name) {
        this.name = name;
        this.applicationDetails = null;
        this.prevStep = null;
    }

    public AbstractStep(final String name, final AbstractStep<?, I> prevStep) {
        this.name = name;
        this.applicationDetails = prevStep.applicationDetails;
        this.prevStep = prevStep;
    }

    public AbstractStep(final String name, final ApplicationDetails applicationDetails) {
        this.name = name;
        this.applicationDetails = applicationDetails;
        this.prevStep = null;
    }

    protected abstract O execute(final ILogger logger, final HttpClient httpClient, final I prevResult) throws Exception;

    public final O refresh(final HttpClient httpClient, final O result) throws Exception {
        return this.refresh(MinecraftAuth.LOGGER, httpClient, result);
    }

    public O refresh(final ILogger logger, final HttpClient httpClient, final O result) throws Exception {
        if (!result.isExpiredOrOutdated()) {
            return result;
        }

        return this.execute(logger, httpClient, this.prevStep != null ? this.prevStep.refresh(logger, httpClient, (I) result.prevResult()) : null);
    }

    public final O getFromInput(final HttpClient httpClient, final InitialInput input) throws Exception {
        return this.getFromInput(MinecraftAuth.LOGGER, httpClient, input);
    }

    public O getFromInput(final ILogger logger, final HttpClient httpClient, final InitialInput input) throws Exception {
        return this.execute(logger, httpClient, this.prevStep != null ? this.prevStep.getFromInput(logger, httpClient, input) : (I) input);
    }

    public abstract O fromJson(final JsonObject json);

    public abstract JsonObject toJson(final O result);

    public abstract static class StepResult<P extends StepResult<?>> {

        protected abstract P prevResult();

        /**
         * Checks if this or any previous result is expired.<br>
         * Results that have no expire time returned by their API are never considered expired.<br>
         * If you want to minimize the amount of HTTP requests, you should only call {@link AbstractStep#refresh} if this method returns true.<br>
         * For certain use cases, like joining a Minecraft server, you want to make sure that the data is up-to-date, even if it is not expired yet. See {@link StepResult#isExpiredOrOutdated}.
         *
         * @return true if this or any previous result is expired
         */
        public abstract boolean isExpired();

        /**
         * Checks if this or any previous result is expired or potentially outdated.<br>
         * Results that have no expire time returned by their API are always considered outdated.<br>
         * If you want the data in the result to be up-to-date, you should call {@link AbstractStep#refresh} if this method returns true.<br>
         *
         * @return true if this or any previous result is potentially outdated or expired
         */
        public boolean isExpiredOrOutdated() {
            return this.isExpired();
        }

    }

    public abstract static class FirstStepResult extends StepResult<StepResult<?>> {

        @Override
        protected final StepResult<?> prevResult() {
            return null;
        }

    }

    public abstract static class InitialInput extends StepResult<StepResult<?>> {

        @Override
        protected final StepResult<?> prevResult() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isExpired() {
            throw new UnsupportedOperationException();
        }

    }

    @Value
    @With
    @EqualsAndHashCode(callSuper = false)
    public static class ApplicationDetails {

        String clientId;
        String scope;
        String clientSecret;
        String redirectUri;
        OAuthEnvironment oAuthEnvironment;

        public static ApplicationDetails fromJson(final JsonObject json) {
            return new ApplicationDetails(
                    json.get("clientId").getAsString(),
                    json.get("scope").getAsString(),
                    JsonUtil.getStringOr(json, "clientSecret", null),
                    JsonUtil.getStringOr(json, "redirectUri", null),
                    OAuthEnvironment.valueOf(JsonUtil.getStringOr(json, "oAuthEnvironment", "LIVE"))
            );
        }

        public static JsonObject toJson(final ApplicationDetails applicationDetails) {
            final JsonObject json = new JsonObject();
            json.addProperty("clientId", applicationDetails.clientId);
            json.addProperty("scope", applicationDetails.scope);
            json.addProperty("clientSecret", applicationDetails.clientSecret);
            json.addProperty("redirectUri", applicationDetails.redirectUri);
            json.addProperty("oAuthEnvironment", applicationDetails.oAuthEnvironment.name());
            return json;
        }

        public boolean isTitleClientId() {
            return !UuidUtil.isDashedUuid(this.clientId);
        }

        public Map<String, String> getOAuthParameters() {
            final Map<String, String> parameters = new HashMap<>();
            parameters.put("client_id", this.clientId);
            parameters.put("scope", this.scope);
            parameters.put("redirect_uri", this.redirectUri);
            parameters.put("response_type", "code");
            parameters.put("response_mode", "query");
            return parameters;
        }

    }

}
