package com.freemanan.starter.grpc.extensions.validation;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Freeman
 */
@Data
@ConfigurationProperties(GrpcValidationProperties.PREFIX)
public class GrpcValidationProperties {
    public static final String PREFIX = "grpc.validation";

    /**
     * Whether to enable validation, default is {@code true}.
     */
    private boolean enabled = true;

    /**
     * Client-side validation configuration.
     */
    private Client client = new Client();

    /**
     * Server-side validation configuration.
     */
    private Server server = new Server();

    /**
     * Validation implementation, default is {@link Implementation#PGV}.
     */
    private Implementation implementation = Implementation.PGV;

    @Data
    public static class Client {
        public static final String PREFIX = GrpcValidationProperties.PREFIX + ".client";

        /**
         * Whether to enable validation, default is {@code true}.
         */
        private boolean enabled = true;
        /**
         * Validating interceptor order, default is {@code 0}.
         */
        private int order = 0;
    }

    @Data
    public static class Server {
        public static final String PREFIX = GrpcValidationProperties.PREFIX + ".server";

        /**
         * Whether to enable validation, default is {@code true}.
         */
        private boolean enabled = true;
        /**
         * Validating interceptor order, default is {@code 0}.
         */
        private int order = 0;
    }

    public enum Implementation {
        /**
         * Validation implementation based on <a href="https://github.com/bufbuild/protoc-gen-validate">protoc-gen-validate</a>.
         */
        PGV,
        /**
         * Validation implementation based on <a href="https://github.com/bufbuild/protovalidate-java">protovalidate</a>.
         */
        PROTO_VALIDATE
    }
}
