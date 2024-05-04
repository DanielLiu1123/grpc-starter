package grpcstarter.client;

import static grpcstarter.client.Util.matchPattern;
import static grpcstarter.client.Util.matchStubConfig;

import io.grpc.stub.AbstractStub;
import java.util.List;
import java.util.Set;
import lombok.experimental.UtilityClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Freeman
 */
@UtilityClass
class Checker {
    private static final Logger log = LoggerFactory.getLogger(Checker.class);

    public static void checkUnusedConfig(GrpcClientProperties properties) {
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
