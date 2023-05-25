package com.freemanan.starter.grpc.extensions.transcoderjson;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Freeman
 */
@Data
@ConfigurationProperties(TranscoderJsonProperties.PREFIX)
public class TranscoderJsonProperties {
    public static final String PREFIX = "grpc.transcoder-json";

    /**
     * Whether to enable transcoder-json auto-configuration, default {@code true}.
     */
    private boolean enabled = true;
}
