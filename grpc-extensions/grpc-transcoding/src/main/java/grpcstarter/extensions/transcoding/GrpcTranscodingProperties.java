package grpcstarter.extensions.transcoding;

import javax.annotation.Nullable;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties for gRPC transcoding.
 *
 * @author Freeman
 */
@Data
@ConfigurationProperties(GrpcTranscodingProperties.PREFIX)
public class GrpcTranscodingProperties {
    public static final String PREFIX = "grpc.transcoding";

    /**
     * Whether to enable transcoding autoconfiguration, default {@code true}.
     */
    private boolean enabled = true;

    /**
     * gRPC server endpoint, if not set, will use {@code localhost:${grpc.server.port}}.
     *
     * <p> In most cases, do not need to set this property explicitly.
     */
    @Nullable
    private String endpoint;

    /**
     * Whether to route methods without the `google.api.http` option, default true.
     *
     * <p> Example:
     * <pre>{@code
     * package bookstore;
     *
     * service Bookstore {
     *   rpc GetShelf(GetShelfRequest) returns (Shelf) {}
     * }
     *
     * message GetShelfRequest {
     *   int64 shelf = 1;
     * }
     *
     * message Shelf {}
     * }</pre>
     *
     * <p> The client could `post` a json body `{"shelf": 1234}` with the path of `/bookstore.Bookstore/GetShelfRequest` to call `GetShelfRequest`.
     */
    private boolean autoMapping = true;

    /**
     * Print options.
     */
    private PrintOptions printOptions = new PrintOptions();

    /**
     * Options for printing JSON output.
     *
     * @see <a href="https://www.envoyproxy.io/docs/envoy/latest/api-v3/extensions/filters/http/grpc_json_transcoder/v3/transcoder.proto#extensions-filters-http-grpc-json-transcoder-v3-grpcjsontranscoder-printoptions">Envoy gRPC-JSON Transcoder PrintOptions</a>
     */
    @Data
    public static class PrintOptions {
        /**
         * Whether to add spaces, line breaks and indentation to make the JSON
         * output easy to read. Defaults to false.
         */
        private boolean addWhitespace = false;

        /**
         * Whether to always print enums as ints. By default they are rendered as strings. Defaults to false.
         */
        private boolean alwaysPrintEnumsAsInts = false;
    }
}
