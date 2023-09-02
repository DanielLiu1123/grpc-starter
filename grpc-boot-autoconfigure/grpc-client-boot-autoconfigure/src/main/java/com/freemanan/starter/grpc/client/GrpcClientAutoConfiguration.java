package com.freemanan.starter.grpc.client;

import static com.freemanan.starter.grpc.client.Util.matchPattern;
import static com.freemanan.starter.grpc.client.Util.matchStubConfig;

import com.freemanan.starter.grpc.server.GrpcServerShutdownEvent;
import io.grpc.stub.AbstractStub;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Freeman
 */
@Configuration(proxyBeanMethods = false)
@ConditionOnGrpcClientEnabled
@EnableConfigurationProperties(GrpcClientProperties.class)
public class GrpcClientAutoConfiguration implements CommandLineRunner, DisposableBean {
    private static final Logger log = LoggerFactory.getLogger(GrpcClientAutoConfiguration.class);

    private final GrpcClientProperties properties;

    public GrpcClientAutoConfiguration(GrpcClientProperties properties) {
        this.properties = properties;
    }

    @Bean
    static GrpcStubBeanDefinitionRegistry genGrpcBeanDefinitionRegistry() {
        return new GrpcStubBeanDefinitionRegistry();
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(GrpcServerShutdownEvent.class)
    static class ChannelCloserConfiguration {

        @Bean
        public static ShutdownEventBasedChannelCloser shutdownEventBasedChannelCloser() {
            return new ShutdownEventBasedChannelCloser();
        }
    }

    @Override
    public void run(String... args) throws Exception {
        warningUnusedConfigurations();
    }

    @Override
    public void destroy() {
        Cache.clear();
    }

    private void warningUnusedConfigurations() {
        Set<Class<?>> stubClasses = Cache.getStubClasses();
        Set<String> services = Cache.getServices();
        List<GrpcClientProperties.Channel> channels = properties.getChannels();
        for (int i = 0; i < channels.size(); i++) {
            GrpcClientProperties.Channel chan = channels.get(i);

            checkClassesConfiguration(stubClasses, i, chan);

            checkStubsConfiguration(stubClasses, i, chan);

            checkServicesConfiguration(services, i, chan);
        }
    }

    private static void checkServicesConfiguration(Set<String> services, int i, GrpcClientProperties.Channel chan) {
        List<String> chanServices = chan.getServices();
        for (int j = 0; j < chanServices.size(); j++) {
            String servicePattern = chanServices.get(j);
            if (services.stream().noneMatch(svc -> matchPattern(servicePattern, svc))) {
                log.warn(
                        "Configuration item '{}.channels[{}].services[{}]: {}' doesn't take effect, please remove it.",
                        GrpcClientProperties.PREFIX,
                        i,
                        j,
                        servicePattern);
            }
        }
    }

    private static void checkStubsConfiguration(Set<Class<?>> stubClasses, int i, GrpcClientProperties.Channel chan) {
        List<String> stubs = chan.getStubs();
        for (int j = 0; j < stubs.size(); j++) {
            String stub = stubs.get(j);
            if (stubClasses.stream().noneMatch(stubClass -> matchStubConfig(stub, stubClass))) {
                log.warn(
                        "Configuration item '{}.channels[{}].stubs[{}]: {}' doesn't take effect, please remove it.",
                        GrpcClientProperties.PREFIX,
                        i,
                        j,
                        stub);
            }
        }
    }

    @SuppressWarnings("rawtypes")
    private static void checkClassesConfiguration(Set<Class<?>> stubClasses, int i, GrpcClientProperties.Channel chan) {
        List<Class<? extends AbstractStub>> classes = chan.getClasses();
        for (int j = 0; j < classes.size(); j++) {
            Class<?> stubClass = classes.get(j);
            if (!stubClasses.contains(stubClass)) {
                log.warn(
                        "Configuration item '{}.channels[{}].classes[{}]: {}' doesn't take effect, please remove it.",
                        GrpcClientProperties.PREFIX,
                        i,
                        j,
                        stubClass.getCanonicalName());
            }
        }
    }
}
