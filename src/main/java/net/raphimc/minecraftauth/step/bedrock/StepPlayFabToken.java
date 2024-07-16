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
package net.raphimc.minecraftauth.step.bedrock;

import com.google.gson.JsonObject;
import lombok.EqualsAndHashCode;
import lombok.Value;
import net.lenni0451.commons.httpclient.HttpClient;
import net.lenni0451.commons.httpclient.requests.impl.PostRequest;
import net.raphimc.minecraftauth.responsehandler.PlayFabResponseHandler;
import net.raphimc.minecraftauth.step.AbstractStep;
import net.raphimc.minecraftauth.step.xbl.StepXblXstsToken;
import net.raphimc.minecraftauth.util.JsonContent;
import net.raphimc.minecraftauth.util.MicrosoftConstants;
import net.raphimc.minecraftauth.util.logging.ILogger;

import java.time.Instant;
import java.time.ZoneId;

public class StepPlayFabToken extends AbstractStep<StepXblXstsToken.XblXsts<?>, StepPlayFabToken.PlayFabToken> {

    public static final String PLAY_FAB_URL = "https://" + MicrosoftConstants.BEDROCK_PLAY_FAB_TITLE_ID.toLowerCase() + ".playfabapi.com/Client/LoginWithXbox";

    public StepPlayFabToken(final AbstractStep<?, ? extends StepXblXstsToken.XblXsts<?>> prevStep) {
        super("playFabToken", (AbstractStep<?, StepXblXstsToken.XblXsts<?>>) prevStep);
    }

    @Override
    protected PlayFabToken execute(final ILogger logger, final HttpClient httpClient, final StepXblXstsToken.XblXsts<?> xblXsts) throws Exception {
        logger.info("Authenticating with PlayFab...");

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
        postData.addProperty("XboxToken", "XBL3.0 x=" + xblXsts.getServiceToken());

        final PostRequest postRequest = new PostRequest(PLAY_FAB_URL);
        postRequest.setContent(new JsonContent(postData));
        final JsonObject obj = httpClient.execute(postRequest, new PlayFabResponseHandler());
        final JsonObject data = obj.getAsJsonObject("data");
        final JsonObject entityToken = data.getAsJsonObject("EntityToken");

        final PlayFabToken playFabToken = new PlayFabToken(
                Instant.parse(entityToken.get("TokenExpiration").getAsString()).toEpochMilli(),
                entityToken.get("EntityToken").getAsString(),
                entityToken.get("Entity").getAsJsonObject().get("Id").getAsString(),
                data.get("SessionTicket").getAsString(),
                data.get("PlayFabId").getAsString(),
                xblXsts
        );
        logger.info("Got PlayFab Token, expires: " + Instant.ofEpochMilli(playFabToken.getExpireTimeMs()).atZone(ZoneId.systemDefault()));
        return playFabToken;
    }

    @Override
    public PlayFabToken fromJson(final JsonObject json) {
        final StepXblXstsToken.XblXsts<?> xblXsts = this.prevStep != null ? this.prevStep.fromJson(json.getAsJsonObject(this.prevStep.name)) : null;
        return new PlayFabToken(
                json.get("expireTimeMs").getAsLong(),
                json.get("entityToken").getAsString(),
                json.get("entityId").getAsString(),
                json.get("sessionTicket").getAsString(),
                json.get("playFabId").getAsString(),
                xblXsts
        );
    }

    @Override
    public JsonObject toJson(final PlayFabToken playFabToken) {
        final JsonObject json = new JsonObject();
        json.addProperty("expireTimeMs", playFabToken.expireTimeMs);
        json.addProperty("entityToken", playFabToken.entityToken);
        json.addProperty("entityId", playFabToken.entityId);
        json.addProperty("sessionTicket", playFabToken.sessionTicket);
        json.addProperty("playFabId", playFabToken.playFabId);
        if (this.prevStep != null) json.add(this.prevStep.name, this.prevStep.toJson(playFabToken.xblXsts));
        return json;
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
        public boolean isExpired() {
            return this.expireTimeMs <= System.currentTimeMillis();
        }

    }

}
