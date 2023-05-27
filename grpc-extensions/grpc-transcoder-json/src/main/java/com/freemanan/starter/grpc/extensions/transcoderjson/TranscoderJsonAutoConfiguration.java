package com.freemanan.starter.grpc.extensions.transcoderjson;

import static org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type.REACTIVE;
import static org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type.SERVLET;

import com.freemanan.starter.grpc.extensions.transcoderjson.processor.DefaultHeaderTransformProcessor;
import com.freemanan.starter.grpc.extensions.transcoderjson.processor.HeaderTransformProcessor;
import com.freemanan.starter.grpc.extensions.transcoderjson.web.WebMvcGrpcServiceHandlerMapping;
import com.freemanan.starter.grpc.extensions.transcoderjson.web.WebMvcProtobufHandlerAdaptor;
import com.freemanan.starter.grpc.extensions.transcoderjson.webflux.GrpcHandlerResultHandler;
import com.freemanan.starter.grpc.extensions.transcoderjson.webflux.WebFluxGrpcServiceHandlerMapping;
import com.freemanan.starter.grpc.extensions.transcoderjson.webflux.WebFluxProtobufHandlerAdaptor;
import io.grpc.BindableService;
import io.grpc.Metadata;
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
@ConditionalOnProperty(prefix = TranscoderJsonProperties.PREFIX, name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(TranscoderJsonProperties.class)
public class TranscoderJsonAutoConfiguration {

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
        public GrpcHandlerResultHandler grpcHandlerResultHandler() {
            return new GrpcHandlerResultHandler();
        }
    }
}
