package grpcstarter.extensions.transcoding;

import io.grpc.Metadata;
import org.springframework.http.HttpHeaders;

/**
 * {@link HeaderConverter} is used to:
 *
 * <p> Convert http headers to gRPC metadata when transcoding http request to gRPC request.
 * <p> Convert gRPC metadata to http headers when transcoding gRPC response to http response.
 *
 * @author Freeman
 * @see DefaultHeaderConverter
 */
public interface HeaderConverter {

    /**
     * Convert http headers to gRPC metadata when transcoding http request to gRPC request.
     *
     * @param headers {@link HttpHeaders}
     * @return converted gRPC metadata
     */
    Metadata toMetadata(HttpHeaders headers);

    /**
     * Convert gRPC metadata to http headers when transcoding gRPC response to http response.
     *
     * @param headers {@link Metadata}
     * @return converted http headers
     */
    HttpHeaders toHttpHeaders(Metadata headers);
}
