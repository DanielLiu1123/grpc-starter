package com.freemanan.starter.grpc.extensions.jsontranscoder;

import io.grpc.Metadata;
import org.springframework.http.HttpHeaders;

/**
 * @author Freeman
 */
public interface HeaderConverter {

    /**
     * Convert http headers to gRPC metadata before the request sent.
     *
     * @param headers {@link HttpHeaders}
     */
    Metadata toMetadata(HttpHeaders headers);

    /**
     * Convert gRPC metadata to http headers after the response received.
     *
     * @param headers {@link Metadata}
     */
    HttpHeaders toHttpHeaders(Metadata headers);
}
