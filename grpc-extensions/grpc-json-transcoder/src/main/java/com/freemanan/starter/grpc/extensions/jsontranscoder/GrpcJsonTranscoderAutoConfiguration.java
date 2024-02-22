package com.freemanan.starter.grpc.extensions.jsontranscoder;

import static org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type.REACTIVE;
import static org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type.SERVLET;

import com.freemanan.starter.grpc.extensions.jsontranscoder.web.WebMvcGrpcServiceHandlerMapping;
import com.freemanan.starter.grpc.extensions.jsontranscoder.web.WebMvcProtobufHandlerAdaptor;
import com.freemanan.starter.grpc.extensions.jsontranscoder.webflux.GrpcHandlerResultHandler;
import com.freemanan.starter.grpc.extensions.jsontranscoder.webflux.WebFluxGrpcServiceHandlerMapping;
import com.freemanan.starter.grpc.extensions.jsontranscoder.webflux.WebFluxProtobufHandlerAdaptor;
import io.grpc.BindableService;
import io.grpc.Metadata;
import java.util.stream.Collectors;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.http.HttpHeaders;
import org.springframework.http.codec.ServerCodecConfigurer;

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
    public GrpcHeaderConverter defaultGrpcHeaderConverter() {
        return new DefaultGrpcHeaderConverter();
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnWebApplication(type = SERVLET)
    static class WebMvc {

        @Bean
        @ConditionalOnMissingBean
        public WebMvcGrpcServiceHandlerMapping webMvcGrpcServiceHandlerMapping(
                ObjectProvider<BindableService> grpcServices) {
            return new WebMvcGrpcServiceHandlerMapping(
                    grpcServices.orderedStream().collect(Collectors.toList()));
        }

        @Bean
        @ConditionalOnMissingBean
        public WebMvcProtobufHandlerAdaptor webMvcProtobufHandlerAdaptor(GrpcHeaderConverter grpcHeaderConverter) {
            return new WebMvcProtobufHandlerAdaptor(grpcHeaderConverter);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnWebApplication(type = REACTIVE)
    static class WebFlux {

        @Bean
        @ConditionalOnMissingBean
        public WebFluxGrpcServiceHandlerMapping webFluxGrpcServiceHandlerMapping(
                ObjectProvider<BindableService> grpcServices) {
            return new WebFluxGrpcServiceHandlerMapping(grpcServices);
        }

        @Bean
        @ConditionalOnMissingBean
        public WebFluxProtobufHandlerAdaptor webFluxProtobufHandlerAdaptor(
                @Qualifier("webFluxAdapterRegistry") ReactiveAdapterRegistry reactiveAdapterRegistry,
                ServerCodecConfigurer serverCodecConfigurer,
                ConfigurableApplicationContext applicationContext,
                GrpcHeaderConverter grpcHeaderConverter) {
            return new WebFluxProtobufHandlerAdaptor(
                    reactiveAdapterRegistry,
                    applicationContext,
                    serverCodecConfigurer.getReaders(),
                    grpcHeaderConverter);
        }

        @Bean
        @ConditionalOnMissingBean
        public GrpcHandlerResultHandler grpcHandlerResultHandler() {
            return new GrpcHandlerResultHandler();
        }
    }
}
