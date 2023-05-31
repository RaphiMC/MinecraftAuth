/*
 * This file is part of MinecraftAuth - https://github.com/RaphiMC/MinecraftAuth
 * Copyright (C) 2023 RK_01/RaphiMC and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.raphimc.mcauth.step.java;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.raphimc.mcauth.MinecraftAuth;
import net.raphimc.mcauth.step.AbstractStep;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.UUID;

public class StepMCProfile extends AbstractStep<StepGameOwnership.GameOwnership, StepMCProfile.MCProfile> {

    public static final String MINECRAFT_PROFILE_URL = "https://api.minecraftservices.com/minecraft/profile";

    public StepMCProfile(AbstractStep<?, StepGameOwnership.GameOwnership> prevStep) {
        super(prevStep);
    }

    @Override
    public MCProfile applyStep(HttpClient httpClient, StepGameOwnership.GameOwnership prevResult) throws Exception {
        MinecraftAuth.LOGGER.info("Getting profile...");

        final HttpGet httpGet = new HttpGet(MINECRAFT_PROFILE_URL);
        httpGet.addHeader("Authorization", prevResult.prevResult().token_type() + " " + prevResult.prevResult().access_token());
        final String response = httpClient.execute(httpGet, new BasicResponseHandler());
        final JsonObject obj = JsonParser.parseString(response).getAsJsonObject();

        if (obj.has("error")) {
            throw new IOException("No valid minecraft profile found: " + obj);
        }

        final MCProfile result = new MCProfile(
                UUID.fromString(obj.get("id").getAsString().replaceFirst("(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)", "$1-$2-$3-$4-$5")),
                obj.get("name").getAsString(),
                new URL(obj.get("skins").getAsJsonArray().get(0).getAsJsonObject().get("url").getAsString()),
                prevResult
        );
        MinecraftAuth.LOGGER.info("Got MC Profile, name: " + result.name + ", uuid: " + result.id);
        return result;
    }

    @Override
    public MCProfile fromJson(JsonObject json) throws Exception {
        final StepGameOwnership.GameOwnership prev = this.prevStep != null ? this.prevStep.fromJson(json.getAsJsonObject("prev")) : null;
        return new MCProfile(
                UUID.fromString(json.get("id").getAsString()),
                json.get("name").getAsString(),
                new URL(json.get("skin_url").getAsString()),
                prev
        );
    }

    public static final class MCProfile implements AbstractStep.StepResult<StepGameOwnership.GameOwnership> {

        private final UUID id;
        private final String name;
        private final URL skin_url;
        private final StepGameOwnership.GameOwnership prevResult;

        public MCProfile(UUID id, String name, URL skin_url, StepGameOwnership.GameOwnership prevResult) {
            this.id = id;
            this.name = name;
            this.skin_url = skin_url;
            this.prevResult = prevResult;
        }

        @Override
        public JsonObject toJson() {
            final JsonObject json = new JsonObject();
            json.addProperty("id", this.id.toString());
            json.addProperty("name", this.name);
            json.addProperty("skin_url", this.skin_url.toString());
            if (this.prevResult != null) json.add("prev", this.prevResult.toJson());
            return json;
        }

        public UUID id() {
            return id;
        }

        public String name() {
            return name;
        }

        public URL skin_url() {
            return skin_url;
        }

        @Override
        public StepGameOwnership.GameOwnership prevResult() {
            return prevResult;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            MCProfile that = (MCProfile) obj;
            return Objects.equals(this.id, that.id) &&
                    Objects.equals(this.name, that.name) &&
                    Objects.equals(this.skin_url, that.skin_url) &&
                    Objects.equals(this.prevResult, that.prevResult);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, name, skin_url, prevResult);
        }

        @Override
        public String toString() {
            return "MCProfile[" +
                    "id=" + id + ", " +
                    "name=" + name + ", " +
                    "skin_url=" + skin_url + ", " +
                    "prevResult=" + prevResult + ']';
        }

    }

}
