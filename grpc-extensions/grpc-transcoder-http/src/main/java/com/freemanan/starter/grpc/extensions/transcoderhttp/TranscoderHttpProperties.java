package com.freemanan.starter.grpc.extensions.transcoderhttp;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Freeman
 */
@Data
@ConfigurationProperties(TranscoderHttpProperties.PREFIX)
public class TranscoderHttpProperties {
    public static final String PREFIX = "grpc.transcoder-http";

    /**
     * Whether to enable transcoder-http auto-configuration, default {@code true}.
     */
    private boolean enabled = true;
}
