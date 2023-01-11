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

import com.google.gson.*;
import net.raphimc.mcauth.MinecraftAuth;
import net.raphimc.mcauth.step.AbstractStep;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class StepGameOwnership extends AbstractStep<StepMCToken.MCToken, StepGameOwnership.GameOwnership> {

    public static final String MINECRAFT_OWNERSHIP_URL = "https://api.minecraftservices.com/entitlements/mcstore";

    public StepGameOwnership(AbstractStep<?, StepMCToken.MCToken> prevStep) {
        super(prevStep);
    }

    @Override
    public GameOwnership applyStep(HttpClient httpClient, StepMCToken.MCToken prevResult) throws Exception {
        MinecraftAuth.LOGGER.info("Getting game ownership...");

        final HttpGet httpGet = new HttpGet(MINECRAFT_OWNERSHIP_URL);
        httpGet.addHeader("Authorization", prevResult.token_type() + " " + prevResult.access_token());
        final String response = httpClient.execute(httpGet, new BasicResponseHandler());
        final JsonObject obj = JsonParser.parseString(response).getAsJsonObject();

        final List<String> items = new ArrayList<>();
        final JsonArray itemsArr = obj.getAsJsonArray("items");
        for (JsonElement item : itemsArr) {
            items.add(item.getAsJsonObject().get("name").getAsString());
        }

        final GameOwnership result = new GameOwnership(
                items,
                prevResult
        );
        MinecraftAuth.LOGGER.info("Got GameOwnership, games: " + result.items);
        if (!result.items.contains("product_minecraft") || !result.items.contains("game_minecraft")) {
            MinecraftAuth.LOGGER.warn("Microsoft account does not own minecraft!");
        }
        return result;
    }

    @Override
    public GameOwnership fromJson(JsonObject json) throws Exception {
        final StepMCToken.MCToken prev = this.prevStep.fromJson(json.getAsJsonObject("prev"));
        return new GameOwnership(
                new Gson().<List<String>>fromJson(json.get("items"), List.class),
                prev
        );
    }

    public static final class GameOwnership implements AbstractStep.StepResult<StepMCToken.MCToken> {

        private final List<String> items;
        private final StepMCToken.MCToken prevResult;

        public GameOwnership(List<String> items, StepMCToken.MCToken prevResult) {
            this.items = items;
            this.prevResult = prevResult;
        }

        @Override
        public JsonObject toJson() {
            final JsonObject json = new JsonObject();
            json.add("items", new Gson().toJsonTree(items));
            json.add("prev", this.prevResult.toJson());
            return json;
        }

        public List<String> items() {
            return items;
        }

        @Override
        public StepMCToken.MCToken prevResult() {
            return prevResult;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            GameOwnership that = (GameOwnership) obj;
            return Objects.equals(this.items, that.items) &&
                    Objects.equals(this.prevResult, that.prevResult);
        }

        @Override
        public int hashCode() {
            return Objects.hash(items, prevResult);
        }

        @Override
        public String toString() {
            return "GameOwnership[" +
                    "items=" + items + ", " +
                    "prevResult=" + prevResult + ']';
        }

    }

}
