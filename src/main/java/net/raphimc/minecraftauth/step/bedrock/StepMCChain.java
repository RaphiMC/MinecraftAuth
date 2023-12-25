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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import lombok.EqualsAndHashCode;
import lombok.Value;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.responsehandler.MinecraftResponseHandler;
import net.raphimc.minecraftauth.step.AbstractStep;
import net.raphimc.minecraftauth.step.xbl.StepXblXstsToken;
import net.raphimc.minecraftauth.util.CryptUtil;
import net.raphimc.minecraftauth.util.JsonUtil;
import org.apache.http.HttpHeaders;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

public class StepMCChain extends AbstractStep<StepXblXstsToken.XblXsts<?>, StepMCChain.MCChain> {

    public static final String MINECRAFT_LOGIN_URL = "https://multiplayer.minecraft.net/authentication";

    private static final String MOJANG_PUBLIC_KEY_BASE64 = "MHYwEAYHKoZIzj0CAQYFK4EEACIDYgAECRXueJeTDqNRRgJi/vlRufByu/2G0i2Ebt6YMar5QX/R0DIIyrJMcUpruK4QveTfJSTp3Shlq4Gk34cD/4GUWwkv0DVuzeuB+tXija7HBxii03NHDbPAD0AKnLr2wdAp";
    private static final ECPublicKey MOJANG_PUBLIC_KEY = CryptUtil.publicKeyEcFromBase64(MOJANG_PUBLIC_KEY_BASE64);
    private static final int CLOCK_SKEW = 60;

    public StepMCChain(final AbstractStep<?, ? extends StepXblXstsToken.XblXsts<?>> prevStep) {
        super("mcChain", (AbstractStep<?, StepXblXstsToken.XblXsts<?>>) prevStep);
    }

    @Override
    public MCChain applyStep(final HttpClient httpClient, final StepXblXstsToken.XblXsts<?> xblXsts) throws Exception {
        MinecraftAuth.LOGGER.info("Authenticating with Minecraft Services...");

        final KeyPairGenerator secp384r1 = KeyPairGenerator.getInstance("EC");
        secp384r1.initialize(new ECGenParameterSpec("secp384r1"));
        final KeyPair ecdsa384KeyPair = secp384r1.generateKeyPair();
        final ECPublicKey publicKey = (ECPublicKey) ecdsa384KeyPair.getPublic();
        final ECPrivateKey privateKey = (ECPrivateKey) ecdsa384KeyPair.getPrivate();

        final JsonObject postData = new JsonObject();
        postData.addProperty("identityPublicKey", Base64.getEncoder().encodeToString(publicKey.getEncoded()));

        final HttpPost httpPost = new HttpPost(MINECRAFT_LOGIN_URL);
        httpPost.setEntity(new StringEntity(postData.toString(), ContentType.APPLICATION_JSON));
        httpPost.addHeader(HttpHeaders.AUTHORIZATION, "XBL3.0 x=" + xblXsts.getServiceToken());
        final String response = httpClient.execute(httpPost, new MinecraftResponseHandler());
        final JsonObject obj = JsonUtil.parseString(response).getAsJsonObject();
        final JsonArray chain = obj.get("chain").getAsJsonArray();
        if (chain.size() != 2) throw new IllegalStateException("Invalid chain size");

        final Jws<Claims> mojangJwt = Jwts.parser().clockSkewSeconds(CLOCK_SKEW).verifyWith(MOJANG_PUBLIC_KEY).build().parseSignedClaims(chain.get(0).getAsString());
        final ECPublicKey mojangJwtPublicKey = CryptUtil.publicKeyEcFromBase64(mojangJwt.getPayload().get("identityPublicKey", String.class));
        final Jws<Claims> identityJwt = Jwts.parser().clockSkewSeconds(CLOCK_SKEW).verifyWith(mojangJwtPublicKey).build().parseSignedClaims(chain.get(1).getAsString());

        final Map<String, Object> extraData = identityJwt.getPayload().get("extraData", Map.class);
        final String xuid = (String) extraData.get("XUID");
        final UUID id = UUID.fromString((String) extraData.get("identity"));
        final String displayName = (String) extraData.get("displayName");

        if (!extraData.containsKey("titleId")) {
            MinecraftAuth.LOGGER.warn("Minecraft chain does not contain titleId! You might get kicked from some servers");
        }

        final MCChain mcChain = new MCChain(
                publicKey,
                privateKey,
                chain.get(0).getAsString(),
                chain.get(1).getAsString(),
                xuid,
                id,
                displayName,
                xblXsts
        );
        MinecraftAuth.LOGGER.info("Got MC Chain, name: " + mcChain.displayName + ", uuid: " + mcChain.id + ", xuid: " + mcChain.xuid);
        return mcChain;
    }

    @Override
    public MCChain fromJson(final JsonObject json) {
        final StepXblXstsToken.XblXsts<?> xblXsts = this.prevStep != null ? this.prevStep.fromJson(json.getAsJsonObject(this.prevStep.name)) : null;
        return new MCChain(
                CryptUtil.publicKeyEcFromBase64(json.get("publicKey").getAsString()),
                CryptUtil.privateKeyEcFromBase64(json.get("privateKey").getAsString()),
                json.get("mojangJwt").getAsString(),
                json.get("identityJwt").getAsString(),
                json.get("xuid").getAsString(),
                UUID.fromString(json.get("id").getAsString()),
                json.get("displayName").getAsString(),
                xblXsts
        );
    }

    @Override
    public JsonObject toJson(final MCChain mcChain) {
        final JsonObject json = new JsonObject();
        json.addProperty("publicKey", Base64.getEncoder().encodeToString(mcChain.publicKey.getEncoded()));
        json.addProperty("privateKey", Base64.getEncoder().encodeToString(mcChain.privateKey.getEncoded()));
        json.addProperty("mojangJwt", mcChain.mojangJwt);
        json.addProperty("identityJwt", mcChain.identityJwt);
        json.addProperty("xuid", mcChain.xuid);
        json.addProperty("id", mcChain.id.toString());
        json.addProperty("displayName", mcChain.displayName);
        if (this.prevStep != null) json.add(this.prevStep.name, this.prevStep.toJson(mcChain.xblXsts));
        return json;
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class MCChain extends AbstractStep.StepResult<StepXblXstsToken.XblXsts<?>> {

        ECPublicKey publicKey;
        ECPrivateKey privateKey;
        String mojangJwt;
        String identityJwt;
        String xuid;
        UUID id;
        String displayName;
        StepXblXstsToken.XblXsts<?> xblXsts;

        @Override
        protected StepXblXstsToken.XblXsts<?> prevResult() {
            return this.xblXsts;
        }

    }

}
