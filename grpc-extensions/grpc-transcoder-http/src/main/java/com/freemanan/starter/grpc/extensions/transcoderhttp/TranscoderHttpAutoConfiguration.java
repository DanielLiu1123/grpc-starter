package com.freemanan.starter.grpc.extensions.transcoderhttp;

import static org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type.REACTIVE;
import static org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type.SERVLET;

import com.freemanan.starter.grpc.extensions.transcoderhttp.processor.DefaultHeaderTransformProcessor;
import com.freemanan.starter.grpc.extensions.transcoderhttp.processor.HeaderTransformProcessor;
import com.freemanan.starter.grpc.extensions.transcoderhttp.web.WebMvcGrpcServiceHandlerMapping;
import com.freemanan.starter.grpc.extensions.transcoderhttp.web.WebMvcProtobufHandlerAdaptor;
import com.freemanan.starter.grpc.extensions.transcoderhttp.webflux.GrpcHandlerResultHandler;
import com.freemanan.starter.grpc.extensions.transcoderhttp.webflux.WebFluxGrpcServiceHandlerMapping;
import com.freemanan.starter.grpc.extensions.transcoderhttp.webflux.WebFluxProtobufHandlerAdaptor;
import io.grpc.BindableService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.reactive.accept.RequestedContentTypeResolver;

/**
 * @author Freeman
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = TranscoderHttpProperties.PREFIX, name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(TranscoderHttpProperties.class)
public class TranscoderHttpAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public HeaderTransformProcessor defaultHeaderTransformProcessor() {
        return new DefaultHeaderTransformProcessor();
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnWebApplication(type = SERVLET)
    static class WebMvc {

        @Bean
        @ConditionalOnMissingBean
        public WebMvcGrpcServiceHandlerMapping webMvcGrpcServiceHandlerMapping(
                ObjectProvider<BindableService> grpcServices) {
            return new WebMvcGrpcServiceHandlerMapping(grpcServices);
        }

        @Bean
        @ConditionalOnMissingBean
        public WebMvcProtobufHandlerAdaptor webMvcProtobufHandlerAdaptor(
                HeaderTransformProcessor headerTransformProcessor) {
            return new WebMvcProtobufHandlerAdaptor(headerTransformProcessor);
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
                HeaderTransformProcessor headerTransformProcessor) {
            return new WebFluxProtobufHandlerAdaptor(
                    reactiveAdapterRegistry,
                    applicationContext,
                    serverCodecConfigurer.getReaders(),
                    headerTransformProcessor);
        }

        @Bean
        @ConditionalOnMissingBean
        public GrpcHandlerResultHandler grpcHandlerResultHandler(
                @Qualifier("webFluxAdapterRegistry") ReactiveAdapterRegistry reactiveAdapterRegistry,
                ServerCodecConfigurer serverCodecConfigurer,
                @Qualifier("webFluxContentTypeResolver") RequestedContentTypeResolver contentTypeResolver) {

            return new GrpcHandlerResultHandler(
                    serverCodecConfigurer.getWriters(), contentTypeResolver, reactiveAdapterRegistry);
        }
    }
}
