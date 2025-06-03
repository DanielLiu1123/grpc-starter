package grpcstarter.extensions.transcoding.openapi;

import grpcstarter.extensions.transcoding.GrpcTranscodingAutoConfiguration;
import grpcstarter.extensions.transcoding.GrpcTranscodingProperties;
import io.grpc.BindableService;
import java.util.List;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import springdocbridge.protobuf.SpringDocBridgeProtobufAutoConfiguration;
import springdocbridge.protobuf.SpringDocBridgeProtobufProperties;

/**
 * @author Freeman
 */
@AutoConfiguration(after = {GrpcTranscodingAutoConfiguration.class, SpringDocBridgeProtobufAutoConfiguration.class})
@ConditionalOnClass({
    GrpcTranscodingProperties.class, // grpc-starter-transcoding
    OpenApiCustomizer.class, // springdoc-openapi-starter-common
    SpringDocBridgeProtobufAutoConfiguration.class // springdoc-bridge-protobuf
})
// @ConditionalOnBean({
//        GrpcTranscodingProperties.class, // grpc.transcoding.enabled=true
//        SpringDocConfigProperties.class, // springdoc.api-docs.enabled=true
//        SpringDocBridgeProtobufProperties.class // springdoc-bridge.enable=true
// })
public class GrpcTranscodingOpenAPIAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public GrpcTranscodingOpenApiCustomizer grpcTranscodingOpenApiCustomizer(
            List<BindableService> services,
            GrpcTranscodingProperties grpcTranscodingProperties,
            SpringDocConfigProperties springDocConfigProperties,
            SpringDocBridgeProtobufProperties springDocBridgeProtobufProperties) {
        return new GrpcTranscodingOpenApiCustomizer(
                services, grpcTranscodingProperties, springDocConfigProperties, springDocBridgeProtobufProperties);
    }
}
