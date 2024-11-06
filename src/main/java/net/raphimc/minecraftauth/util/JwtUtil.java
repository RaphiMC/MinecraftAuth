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
package net.raphimc.minecraftauth.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.impl.security.DefaultSecureRequest;
import org.jetbrains.annotations.ApiStatus;

import java.io.ByteArrayInputStream;
import java.security.PrivateKey;
import java.security.PublicKey;

import static net.raphimc.minecraftauth.util.TimeUtil.MAX_JWT_CLOCK_SKEW;

/**
 * Utility class for handling JWTs. Code is intentionally kept separate from the rest of the project to allow for the exclusion of the JWT library.
 */
@ApiStatus.Internal
public class JwtUtil {

    public static Jwt parseSignedJwt(final String jwt, final PublicKey publicKey) {
        final Jws<Claims> parsedJwt = Jwts.parser().clockSkewSeconds(MAX_JWT_CLOCK_SKEW).verifyWith(publicKey).build().parseSignedClaims(jwt);
        return new Jwt() {

            @Override
            public <T> T getClaim(final String claimName, final Class<T> requiredType) {
                return parsedJwt.getPayload().get(claimName, requiredType);
            }

        };
    }

    public static byte[] signES256(final PrivateKey privateKey, final byte[] data) {
        return Jwts.SIG.ES256.digest(new DefaultSecureRequest<>(new ByteArrayInputStream(data), null, null, privateKey));
    }

    public interface Jwt {

        <T> T getClaim(final String claimName, final Class<T> requiredType);

    }

}
