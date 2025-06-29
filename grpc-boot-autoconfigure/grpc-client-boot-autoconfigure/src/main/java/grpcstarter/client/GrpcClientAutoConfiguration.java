package grpcstarter.client;

import grpcstarter.server.GrpcServerShutdownEvent;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * This class is responsible for autoconfiguring the necessary beans and components
 * for the gRPC client.
 *
 * @author Freeman
 */
@Configuration(proxyBeanMethods = false)
@ConditionOnGrpcClientEnabled
@EnableConfigurationProperties(GrpcClientProperties.class)
public class GrpcClientAutoConfiguration implements DisposableBean, ApplicationListener<ApplicationReadyEvent> {

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        var bf = event.getApplicationContext().getBeanFactory();
        if (bf instanceof BeanDefinitionRegistry bdr) {
            GrpcStubBeanRegistrar.clearBeanDefinitionCache(bdr);
        }
    }

    @Bean
    static GrpcStubBeanDefinitionRegistry grpcStubBeanDefinitionRegistry() {
        return new GrpcStubBeanDefinitionRegistry();
    }

    @Bean
    @ConditionalOnMissingBean
    public GrpcClientOptionsClientInterceptor grpcClientOptionsClientInterceptor() {
        return new GrpcClientOptionsClientInterceptor();
    }

    @Bean
    @ConditionalOnMissingBean
    public ManagedChannels grpcClientManagedChannels(
            BeanFactory beanFactory, GrpcClientProperties grpcClientProperties) {
        return new ManagedChannelsImpl(beanFactory, grpcClientProperties);
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(RefreshScopeRefreshedEvent.class)
    static class RefreshConfiguration {
        @Bean
        @ConditionalOnMissingBean
        @ConditionalOnProperty(prefix = GrpcClientProperties.Refresh.PREFIX, name = "enabled", havingValue = "true")
        public GrpcClientRefreshScopeRefreshedEventListener grpcClientRefreshScopeRefreshedEventListener() {
            return new GrpcClientRefreshScopeRefreshedEventListener();
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(GrpcServerShutdownEvent.class)
    static class ChannelCloserConfiguration {

        @Bean
        public ShutdownEventBasedChannelCloser shutdownEventBasedChannelCloser() {
            return new ShutdownEventBasedChannelCloser();
        }
    }

    @Override
    public void destroy() {
        Cache.clear();
        GrpcStubBeanDefinitionRegistry.scanInfo.clear();
    }

    // Native image support
    @Bean
    static GrpcClientBeanFactoryInitializationAotProcessor grpcClientBeanFactoryInitializationAotProcessor() {
        return new GrpcClientBeanFactoryInitializationAotProcessor();
    }
}
