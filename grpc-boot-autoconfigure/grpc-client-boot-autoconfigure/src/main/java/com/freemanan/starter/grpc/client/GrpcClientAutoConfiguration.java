package com.freemanan.starter.grpc.client;

import io.grpc.stub.AbstractStub;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Freeman
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(AbstractStub.class)
@ConditionalOnProperty(prefix = GrpcClientProperties.PREFIX, name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(GrpcClientProperties.class)
public class GrpcClientAutoConfiguration implements SmartInitializingSingleton {
    private static final Logger log = LoggerFactory.getLogger(GrpcClientAutoConfiguration.class);

    private final GrpcClientProperties properties;

    public GrpcClientAutoConfiguration(GrpcClientProperties properties) {
        this.properties = properties;
    }

    @Bean
    public static GrpcStubBeanDefinitionRegistry genGrpcBeanDefinitionRegistry() {
        return new GrpcStubBeanDefinitionRegistry();
    }

    @Override
    public void afterSingletonsInstantiated() {
        warningUnusedConfigurations();
    }

    @SuppressWarnings("rawtypes")
    private void warningUnusedConfigurations() {
        Set<Class<?>> stubClasses = Cache.getStubClasses();
        Set<String> services = Cache.getServices();
        List<GrpcClientProperties.Channel> channels = properties.getChannels();
        for (int i = 0; i < channels.size(); i++) {
            GrpcClientProperties.Channel chan = channels.get(i);
            List<String> chanServices = chan.getServices();
            for (int j = 0; j < chanServices.size(); j++) {
                String service = chanServices.get(j);
                if (!services.contains(service)) {
                    log.warn(
                            "Configuration item '{}.channels[{}].services[{}]: {}' doesn't take effect, please remove it.",
                            GrpcClientProperties.PREFIX,
                            i,
                            j,
                            service);
                }
            }
            List<Class<? extends AbstractStub>> stubs = chan.getStubs();
            for (int j = 0; j < stubs.size(); j++) {
                Class<?> stubClass = stubs.get(j);
                if (!stubClasses.contains(stubClass)) {
                    log.warn(
                            "Configuration item '{}.channels[{}].stubs[{}]: {}' doesn't take effect, please remove it.",
                            GrpcClientProperties.PREFIX,
                            i,
                            j,
                            stubClass.getCanonicalName());
                }
            }
        }
    }
}
