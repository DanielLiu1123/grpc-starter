package grpcstarter.extensions.transcoding;

import static org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type.REACTIVE;
import static org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type.SERVLET;

import grpcstarter.server.ConditionOnGrpcServerEnabled;
import grpcstarter.server.GrpcServerProperties;
import io.grpc.BindableService;
import io.grpc.Metadata;
import java.util.List;
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
@ConditionalOnClass({Metadata.class, HttpHeaders.class, GrpcServerProperties.class})
@ConditionOnGrpcServerEnabled
@ConditionalOnProperty(prefix = GrpcTranscodingProperties.PREFIX, name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(GrpcTranscodingProperties.class)
public class GrpcTranscodingAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public HeaderConverter defaultGrpcTranscodingHeaderConverter() {
        return new DefaultHeaderConverter();
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnWebApplication(type = SERVLET)
    static class WebMvc {

        @Bean
        @ConditionalOnMissingBean(TranscodingExceptionResolver.class)
        public DefaultTranscodingExceptionResolver defaultTranscodingExceptionResolver(
                HeaderConverter headerConverter) {
            return new DefaultTranscodingExceptionResolver(headerConverter);
        }

        @Bean
        @ConditionalOnMissingBean(ServletTranscoder.class)
        public DefaultServletTranscoder grpcStarterDefaultServletTranscoder(
                List<BindableService> services,
                HeaderConverter headerConverter,
                GrpcTranscodingProperties grpcTranscodingProperties,
                GrpcServerProperties grpcServerProperties,
                TranscodingExceptionResolver transcodingExceptionResolver) {
            return new DefaultServletTranscoder(
                    services,
                    headerConverter,
                    grpcTranscodingProperties,
                    grpcServerProperties,
                    transcodingExceptionResolver);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnWebApplication(type = REACTIVE)
    static class WebFlux {

        @Bean
        @ConditionalOnMissingBean(ReactiveTranscodingExceptionResolver.class)
        public DefaultReactiveTranscodingExceptionResolver defaultReactiveTranscodingExceptionResolver(
                HeaderConverter headerConverter) {
            return new DefaultReactiveTranscodingExceptionResolver(headerConverter);
        }

        @Bean
        @ConditionalOnMissingBean(ReactiveTranscoder.class)
        public DefaultReactiveTranscoder grpcStarterDefaultReactiveTranscoder(
                List<BindableService> services,
                HeaderConverter headerConverter,
                GrpcTranscodingProperties grpcTranscodingProperties,
                GrpcServerProperties grpcServerProperties,
                ReactiveTranscodingExceptionResolver transcodingExceptionResolver) {
            return new DefaultReactiveTranscoder(
                    services,
                    headerConverter,
                    grpcTranscodingProperties,
                    grpcServerProperties,
                    transcodingExceptionResolver);
        }
    }
}
