package com.freemanan.starter.grpc;

import com.freemanan.starter.grpc.client.GrpcClientConfiguration;
import com.freemanan.starter.grpc.server.GrpcServerConfiguration;
import io.grpc.Grpc;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

/**
 * gRPC auto configuration.
 *
 * @author Freeman
 */
@AutoConfiguration
@ConditionalOnClass(Grpc.class)
@ConditionalOnProperty(prefix = GrpcProperties.PREFIX, name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(GrpcProperties.class)
@Import({GrpcServerConfiguration.class, GrpcClientConfiguration.class})
public class GrpcAutoConfiguration {}
