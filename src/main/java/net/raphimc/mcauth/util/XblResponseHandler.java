package net.raphimc.mcauth.util;

import org.apache.http.*;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

public class XblResponseHandler implements ResponseHandler<String> {

    @Override
    public String handleResponse(HttpResponse response) throws IOException {
        final StatusLine statusLine = response.getStatusLine();
        final HttpEntity entity = response.getEntity();
        if (statusLine.getStatusCode() >= 300) {
            EntityUtils.consume(entity);
            if (response.containsHeader("X-Err")) {
                throw new HttpResponseException(statusLine.getStatusCode(), MicrosoftConstants.XBOX_LIVE_ERRORS.getOrDefault(Long.valueOf(response.getFirstHeader("X-Err").getValue()), statusLine.getReasonPhrase()));
            }
            throw new HttpResponseException(statusLine.getStatusCode(), statusLine.getReasonPhrase());
        }
        return entity == null ? null : EntityUtils.toString(entity);
    }

}
