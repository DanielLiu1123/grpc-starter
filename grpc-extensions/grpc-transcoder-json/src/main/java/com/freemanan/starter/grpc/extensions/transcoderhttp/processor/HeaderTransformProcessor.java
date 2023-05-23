package com.freemanan.starter.grpc.extensions.transcoderhttp.processor;

import io.grpc.Metadata;
import org.springframework.http.HttpHeaders;

/**
 * @author Freeman
 */
public interface HeaderTransformProcessor {

    /**
     * Convert http headers to gRPC metadata before the request sent.
     *
     * @param headers {@link HttpHeaders}
     */
    Metadata toRequestMetadata(HttpHeaders headers);

    /**
     * Convert gRPC metadata to http headers after the response received.
     *
     * @param headers {@link Metadata}
     */
    HttpHeaders toResponseHeader(Metadata headers);
}
