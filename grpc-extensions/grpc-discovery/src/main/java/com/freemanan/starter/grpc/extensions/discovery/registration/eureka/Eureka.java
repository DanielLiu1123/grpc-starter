package com.freemanan.starter.grpc.extensions.discovery.registration.eureka;

import com.freemanan.starter.grpc.server.GrpcServer;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.netflix.eureka.serviceregistry.EurekaAutoServiceRegistration;
import org.springframework.cloud.netflix.eureka.serviceregistry.EurekaRegistration;
import org.springframework.cloud.netflix.eureka.serviceregistry.EurekaServiceRegistry;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextClosedEvent;

/**
 * @author Freeman
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(EurekaAutoServiceRegistration.class)
@ConditionalOnProperty(value = "eureka.client.register-with-eureka", matchIfMissing = true)
public class Eureka {

    @Bean
    public GrpcStubDiscoveryClientOptionalArgs grpcStubDiscoveryClientOptionalArgs() {
        return new GrpcStubDiscoveryClientOptionalArgs();
    }

    @Bean
    @ConditionalOnProperty(value = "spring.cloud.service-registry.auto-registration.enabled", matchIfMissing = true)
    GrpcEurekaAutoServiceRegistrationListener grpcEurekaAutoServiceRegistrationListener(
            EurekaServiceRegistry eurekaServiceRegistry, EurekaRegistration eurekaRegistration, GrpcServer grpcServer) {
        return new GrpcEurekaAutoServiceRegistrationListener(eurekaServiceRegistry, eurekaRegistration, grpcServer);
    }

    @Bean
    static BeanDefinitionRegistryPostProcessor grpcEurekaAutoServiceRegistrationBeanFactoryPostProcessor() {
        return new BeanDefinitionRegistryPostProcessor() {
            @Override
            public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
                // see
                // org.springframework.cloud.netflix.eureka.EurekaClientAutoConfiguration#eurekaAutoServiceRegistration
                registry.removeBeanDefinition("eurekaAutoServiceRegistration");
            }

            @Override
            public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {}
        };
    }

    @RequiredArgsConstructor
    static class GrpcEurekaAutoServiceRegistrationListener implements ApplicationListener<ApplicationEvent> {

        private final EurekaServiceRegistry eurekaServiceRegistry;
        private final EurekaRegistration eurekaRegistration;
        private final GrpcServer grpcServer;

        @Override
        public void onApplicationEvent(ApplicationEvent event) {
            if (grpcServer.getPort() > 0) {
                if (event instanceof ApplicationReadyEvent) {
                    int oldPort = eurekaRegistration.getPort();

                    eurekaRegistration.setNonSecurePort(grpcServer.getPort());

                    eurekaServiceRegistry.register(eurekaRegistration);

                    eurekaRegistration.setNonSecurePort(oldPort);
                }
                if (event instanceof ContextClosedEvent) {
                    int oldPort = eurekaRegistration.getPort();

                    eurekaRegistration.setNonSecurePort(grpcServer.getPort());

                    eurekaServiceRegistry.deregister(eurekaRegistration);

                    eurekaRegistration.setNonSecurePort(oldPort);
                }
            }
        }
    }
}
