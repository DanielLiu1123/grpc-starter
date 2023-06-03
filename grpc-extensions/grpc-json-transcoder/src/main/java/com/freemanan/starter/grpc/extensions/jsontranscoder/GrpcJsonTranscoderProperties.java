package com.freemanan.starter.grpc.extensions.jsontranscoder;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Freeman
 */
@Data
@ConfigurationProperties(GrpcJsonTranscoderProperties.PREFIX)
public class GrpcJsonTranscoderProperties {
    public static final String PREFIX = "grpc.json-transcoder";

    /**
     * Whether to enable transcoder-json auto-configuration, default {@code true}.
     */
    private boolean enabled = true;
}
