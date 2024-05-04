package grpcstarter.extensions.transcoding;

import static org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type.REACTIVE;
import static org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type.SERVLET;

import grpcstarter.server.GrpcServerCustomizer;
import grpcstarter.server.GrpcServerProperties;
import io.grpc.BindableService;
import io.grpc.Metadata;
import io.grpc.ServerInterceptor;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;

/**
 * @author Freeman
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({Metadata.class, HttpHeaders.class})
@ConditionalOnProperty(prefix = GrpcTranscodingProperties.PREFIX, name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(GrpcTranscodingProperties.class)
public class GrpcTranscodingAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public HeaderConverter defaultHeaderConverter() {
        return new DefaultHeaderConverter();
    }

    @Bean
    @ConditionalOnMissingBean
    public TranscodingGrpcServer transcodingGrpcServer(
            GrpcServerProperties properties,
            ObjectProvider<BindableService> serviceProvider,
            ObjectProvider<ServerInterceptor> interceptorProvider,
            ObjectProvider<GrpcServerCustomizer> customizers) {
        return new TranscodingGrpcServer(properties, serviceProvider, interceptorProvider, customizers);
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnWebApplication(type = SERVLET)
    static class WebMvc {

        @Bean
        public ServletTranscodingRouterFunction webMvcTranscodingRouterFunction(
                List<BindableService> services, HeaderConverter headerConverter) {
            return new ServletTranscodingRouterFunction(services, headerConverter);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnWebApplication(type = REACTIVE)
    static class WebFlux {

        @Bean
        public ReactiveTranscodingRouterFunction webFluxTranscodingRouterFunction(
                List<BindableService> services, HeaderConverter headerConverter) {
            return new ReactiveTranscodingRouterFunction(services, headerConverter);
        }
    }
}
