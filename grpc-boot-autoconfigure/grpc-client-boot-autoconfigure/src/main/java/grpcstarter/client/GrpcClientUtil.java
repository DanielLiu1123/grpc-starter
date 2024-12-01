package grpcstarter.client;

import static org.springframework.core.NativeDetector.inNativeImage;

import io.grpc.stub.AbstractStub;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionOverrideException;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.aot.AbstractAotProcessor;

/**
 * @author Freeman
 */
public final class GrpcClientUtil {

    private static final Logger log = LoggerFactory.getLogger(GrpcClientUtil.class);

    private GrpcClientUtil() {
        throw new UnsupportedOperationException("Cannot instantiate utility class");
    }

    /**
     * Register a gRPC client bean for the given class.
     *
     * @param beanFactory {@link DefaultListableBeanFactory}
     * @param clz         gRPC client class
     */
    public static void registerGrpcClientBean(@Nonnull DefaultListableBeanFactory beanFactory, @Nonnull Class<?> clz) {
        if (!AbstractStub.class.isAssignableFrom(clz)) {
            throw new IllegalArgumentException(clz + " is not a gRPC client");
        }

        var className = clz.getName();

        // Refresh scope is not supported with native images, see
        // https://docs.spring.io/spring-cloud-config/reference/server/aot-and-native-image-support.html
        var supportRefresh = !isAotProcessing() && !inNativeImage();

        var abd = BeanDefinitionBuilder.genericBeanDefinition(
                        clz, () -> new GrpcClientCreator(beanFactory, clz).create(supportRefresh))
                .getBeanDefinition();

        abd.setLazyInit(true);
        abd.setAttribute(GrpcClientBeanFactoryInitializationAotProcessor.IS_CREATED_BY_FRAMEWORK, true);
        abd.setResourceDescription("registered by grpc-client-boot-starter");

        try {
            BeanDefinitionReaderUtils.registerBeanDefinition(new BeanDefinitionHolder(abd, className), beanFactory);
        } catch (BeanDefinitionOverrideException ignore) {
            // clients are included in base packages
            log.warn(
                    "gRPC stub '{}' is included in base packages, you can remove it from 'clients' property.",
                    className);
        }
    }

    /**
     * @see AbstractAotProcessor#process()
     */
    private static boolean isAotProcessing() {
        return Boolean.getBoolean("spring.aot.processing");
    }
}
