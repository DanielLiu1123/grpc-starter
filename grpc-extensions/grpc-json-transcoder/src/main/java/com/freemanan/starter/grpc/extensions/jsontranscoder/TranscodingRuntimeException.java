package com.freemanan.starter.grpc.extensions.jsontranscoder;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.server.ResponseStatusException;

/**
 * @author Freeman
 */
public class TranscodingRuntimeException extends ResponseStatusException {

    private final HttpHeaders headers = new HttpHeaders();

    public TranscodingRuntimeException(HttpStatusCode status, @Nullable String reason, @Nullable HttpHeaders headers) {
        super(status, reason);

        if (headers != null) {
            this.headers.putAll(headers);
        }
    }

    @Override
    @Nonnull
    public HttpHeaders getHeaders() {
        return headers;
    }
}
