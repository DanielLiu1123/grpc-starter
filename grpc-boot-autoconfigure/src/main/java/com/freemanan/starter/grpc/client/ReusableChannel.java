package com.freemanan.starter.grpc.client;

import com.freemanan.starter.grpc.GrpcProperties;
import io.grpc.Channel;
import java.util.List;
import org.springframework.util.unit.DataSize;

/**
 * Determine whether {@link Channel} can be reused.
 *
 * @author Freeman
 */
record ReusableChannel(
        String authority,
        DataSize maxMessageSize,
        DataSize maxMetadataSize,
        List<GrpcProperties.Client.Metadata> metadata) {}
