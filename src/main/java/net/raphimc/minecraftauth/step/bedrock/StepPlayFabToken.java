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
package net.raphimc.minecraftauth.step.bedrock;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.EqualsAndHashCode;
import lombok.Value;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.responsehandler.PlayFabResponseHandler;
import net.raphimc.minecraftauth.step.AbstractStep;
import net.raphimc.minecraftauth.step.xbl.StepXblXstsToken;
import net.raphimc.minecraftauth.util.MicrosoftConstants;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;

import java.time.Instant;
import java.time.ZoneId;

public class StepPlayFabToken extends AbstractStep<StepXblXstsToken.XblXsts<?>, StepPlayFabToken.PlayFabToken> {

    public static final String PLAY_FAB_URL = "https://" + MicrosoftConstants.BEDROCK_PLAY_FAB_TITLE_ID.toLowerCase() + ".playfabapi.com/Client/LoginWithXbox";

    public StepPlayFabToken(final AbstractStep<?, StepXblXstsToken.XblXsts<?>> prevStep) {
        super(prevStep);
    }

    @Override
    public PlayFabToken applyStep(final HttpClient httpClient, final StepXblXstsToken.XblXsts<?> prevResult) throws Exception {
        MinecraftAuth.LOGGER.info("Authenticating with PlayFab...");

        final JsonObject postData = new JsonObject();
        postData.addProperty("CreateAccount", true);
        postData.add("EncryptedRequest", null);
        final JsonObject infoRequestParameters = new JsonObject();
        infoRequestParameters.addProperty("GetCharacterInventories", false);
        infoRequestParameters.addProperty("GetCharacterList", false);
        infoRequestParameters.addProperty("GetPlayerProfile", true);
        infoRequestParameters.addProperty("GetPlayerStatistics", false);
        infoRequestParameters.addProperty("GetTitleData", false);
        infoRequestParameters.addProperty("GetUserAccountInfo", true);
        infoRequestParameters.addProperty("GetUserData", false);
        infoRequestParameters.addProperty("GetUserInventory", false);
        infoRequestParameters.addProperty("GetUserReadOnlyData", false);
        infoRequestParameters.addProperty("GetUserVirtualCurrency", false);
        infoRequestParameters.add("PlayerStatisticNames", null);
        infoRequestParameters.add("ProfileConstraints", null);
        infoRequestParameters.add("TitleDataKeys", null);
        infoRequestParameters.add("UserDataKeys", null);
        infoRequestParameters.add("UserReadOnlyDataKeys", null);
        postData.add("InfoRequestParameters", infoRequestParameters);
        postData.add("PlayerSecret", null);
        postData.addProperty("TitleId", MicrosoftConstants.BEDROCK_PLAY_FAB_TITLE_ID);
        postData.addProperty("XboxToken", "XBL3.0 x=" + prevResult.getUserHash() + ";" + prevResult.getToken());

        final HttpPost httpPost = new HttpPost(PLAY_FAB_URL);
        httpPost.setEntity(new StringEntity(postData.toString(), ContentType.APPLICATION_JSON));
        final String response = httpClient.execute(httpPost, new PlayFabResponseHandler());
        final JsonObject obj = JsonParser.parseString(response).getAsJsonObject();
        final JsonObject data = obj.getAsJsonObject("data");
        final JsonObject entityToken = data.getAsJsonObject("EntityToken");

        final PlayFabToken result = new PlayFabToken(
                Instant.parse(entityToken.get("TokenExpiration").getAsString()).toEpochMilli(),
                entityToken.get("EntityToken").getAsString(),
                entityToken.get("Entity").getAsJsonObject().get("Id").getAsString(),
                data.get("SessionTicket").getAsString(),
                data.get("PlayFabId").getAsString(),
                prevResult
        );
        MinecraftAuth.LOGGER.info("Got PlayFab Token, expires: " + Instant.ofEpochMilli(result.getExpireTimeMs()).atZone(ZoneId.systemDefault()));
        return result;
    }

    @Override
    public PlayFabToken refresh(final HttpClient httpClient, final PlayFabToken result) throws Exception {
        if (result.isExpired()) return super.refresh(httpClient, result);

        return result;
    }

    @Override
    public PlayFabToken fromJson(final JsonObject json) {
        final StepXblXstsToken.XblXsts<?> xblXsts = this.prevStep != null ? this.prevStep.fromJson(json.getAsJsonObject("xblXsts")) : null;
        return new PlayFabToken(
                json.get("expireTimeMs").getAsLong(),
                json.get("entityToken").getAsString(),
                json.get("entityId").getAsString(),
                json.get("sessionTicket").getAsString(),
                json.get("playFabId").getAsString(),
                xblXsts
        );
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class PlayFabToken extends AbstractStep.StepResult<StepXblXstsToken.XblXsts<?>> {

        long expireTimeMs;
        String entityToken;
        String entityId;
        String sessionTicket;
        String playFabId;
        StepXblXstsToken.XblXsts<?> xblXsts;

        @Override
        protected StepXblXstsToken.XblXsts<?> prevResult() {
            return this.xblXsts;
        }

        @Override
        public JsonObject toJson() {
            final JsonObject json = new JsonObject();
            json.addProperty("expireTimeMs", this.expireTimeMs);
            json.addProperty("entityToken", this.entityToken);
            json.addProperty("entityId", this.entityId);
            json.addProperty("sessionTicket", this.sessionTicket);
            json.addProperty("playFabId", this.playFabId);
            if (this.xblXsts != null) json.add("xblXsts", this.xblXsts.toJson());
            return json;
        }

        @Override
        public boolean isExpired() {
            return this.expireTimeMs <= System.currentTimeMillis();
        }

    }

}
