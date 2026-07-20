/*
 * This file is part of MinecraftAuth - https://github.com/RaphiMC/MinecraftAuth
 * Copyright (C) 2022-2026 RK_01/RaphiMC and contributors
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
package net.raphimc.minecraftauth.xbl.request;

import net.lenni0451.commons.gson.elements.GsonArray;
import net.lenni0451.commons.gson.elements.GsonElement;
import net.lenni0451.commons.gson.elements.GsonObject;
import net.lenni0451.commons.httpclient.HttpResponse;
import net.lenni0451.commons.httpclient.constants.HttpHeaders;
import net.lenni0451.commons.httpclient.requests.impl.GetRequest;
import net.lenni0451.commons.httpclient.utils.URLCoder;
import net.raphimc.minecraftauth.xbl.model.XblUserProfile;
import net.raphimc.minecraftauth.xbl.model.XblXstsToken;
import net.raphimc.minecraftauth.xbl.responsehandler.XblResponseHandler;

import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.stream.Collectors;

public class XblUserProfileSettingsRequest extends GetRequest implements XblResponseHandler<XblUserProfile> {

    public XblUserProfileSettingsRequest(final XblXstsToken xstsToken, final String user, final String... settings) throws MalformedURLException {
        this(xstsToken, user, Arrays.asList(settings));
    }

    public XblUserProfileSettingsRequest(final XblXstsToken xstsToken, final String user, final Iterable<String> settings) throws MalformedURLException {
        super("https://profile.xboxlive.com/users/" + URLCoder.encode(user) + "/profile/settings?settings=" + URLCoder.encode(String.join(",", settings)));

        this.setHeader(HttpHeaders.AUTHORIZATION, xstsToken.getAuthorizationHeader());
        this.setHeader("x-xbl-contract-version", "3");
    }

    @Override
    public XblUserProfile handle(final HttpResponse response, final GsonObject json) {
        final GsonArray profileUsers = json.reqArray("profileUsers");
        if (profileUsers.size() != 1) { // Should not happen
            throw new IllegalStateException("Expected 1 profile user, but got " + profileUsers.size());
        }
        final GsonObject profileUser = profileUsers.getObject(0);
        return new XblUserProfile(
                profileUser.reqString("id"),
                profileUser.reqArray("settings").stream().map(GsonElement::asObject).collect(Collectors.toMap(
                        setting -> setting.reqString("id"),
                        setting -> setting.reqString("value")
                ))
        );
    }

}
