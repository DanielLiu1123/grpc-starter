package com.freemanan.starter.grpc.extensions.jsontranscoder;

import static org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type.REACTIVE;
import static org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type.SERVLET;

import com.freemanan.starter.grpc.server.GrpcServerCustomizer;
import com.freemanan.starter.grpc.server.GrpcServerProperties;
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
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * @author Freeman
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({Metadata.class, HttpHeaders.class})
@ConditionalOnProperty(prefix = GrpcJsonTranscoderProperties.PREFIX, name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(GrpcJsonTranscoderProperties.class)
public class GrpcJsonTranscoderAutoConfiguration {

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
        public RouterFunction<ServerResponse> webMvcTranscodingRouterFunction(List<BindableService> services) {
            return new ServletTranscodingRouterFunction(services);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnWebApplication(type = REACTIVE)
    static class WebFlux {

        @Bean
        public org.springframework.web.reactive.function.server.RouterFunction<
                        org.springframework.web.reactive.function.server.ServerResponse>
                webFluxTranscodingRouterFunction(List<BindableService> services) {
            return new ReactiveTranscodingRouterFunction(services);
        }
    }
}
