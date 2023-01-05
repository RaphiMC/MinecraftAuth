package net.raphimc.mcauth.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.http.*;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.entity.ContentType;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

public class MsaResponseHandler implements ResponseHandler<String> {

    @Override
    public String handleResponse(HttpResponse response) throws IOException {
        final StatusLine statusLine = response.getStatusLine();
        final HttpEntity entity = response.getEntity();
        final String body = entity == null ? null : EntityUtils.toString(entity);
        if (statusLine.getStatusCode() >= 300) {
            if (body != null && ContentType.getOrDefault(entity).getMimeType().equals(ContentType.APPLICATION_JSON.getMimeType())) {
                final JsonObject obj = (JsonObject) JsonParser.parseString(body);
                if (obj.has("error") && obj.has("error_description")) {
                    throw new HttpResponseException(statusLine.getStatusCode(), obj.get("error").getAsString() + ": " + obj.get("error_description").getAsString());
                }
            }
            throw new HttpResponseException(statusLine.getStatusCode(), statusLine.getReasonPhrase());
        }
        return body;
    }

}
