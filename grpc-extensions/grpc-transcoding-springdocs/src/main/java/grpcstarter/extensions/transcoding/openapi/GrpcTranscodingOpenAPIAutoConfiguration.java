package grpcstarter.extensions.transcoding.openapi;

import grpcstarter.extensions.transcoding.GrpcTranscodingAutoConfiguration;
import grpcstarter.extensions.transcoding.GrpcTranscodingProperties;
import io.grpc.BindableService;
import java.util.List;
import org.springdoc.core.configuration.SpringDocConfiguration;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * @author Freeman
 */
@AutoConfiguration(after = {GrpcTranscodingAutoConfiguration.class, SpringDocConfiguration.class})
@ConditionalOnClass({
    GrpcTranscodingProperties.class, // grpc-starter-transcoding
    OpenApiCustomizer.class, // springdoc-openapi-starter-common
})
@ConditionalOnBean({GrpcTranscodingProperties.class, SpringDocConfiguration.class}) // transcoding & springdoc enabled
public class GrpcTranscodingOpenAPIAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public GrpcTranscodingOpenApiCustomizer grpcTranscodingOpenApiCustomizer(
            List<BindableService> services,
            GrpcTranscodingProperties grpcTranscodingProperties,
            SpringDocConfigProperties springDocConfigProperties) {
        return new GrpcTranscodingOpenApiCustomizer(services, grpcTranscodingProperties, springDocConfigProperties);
    }
}
