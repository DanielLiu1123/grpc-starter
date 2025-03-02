package grpcstarter.client;

import io.grpc.stub.AbstractAsyncStub;
import io.grpc.stub.AbstractBlockingStub;
import io.grpc.stub.AbstractFutureStub;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.config.BeanDefinition;

/**
 * {@link GrpcClientBeanDefinitionHandler} uses to handle gRPC client bean definitions before registering.
 *
 * <p> If you only want to use blocking stubs, you can use {@link Blocking}.
 *
 * @author Freeman
 * @since 3.4.3.1
 */
public interface GrpcClientBeanDefinitionHandler {

    /**
     * Handle the given gRPC client bean definition.
     *
     * @param beanDefinition bean definition
     * @param clazz gRPC client class
     * @return the handled bean definition, or {@code null} if you want to skip this bean definition to be registered
     */
    @Nullable
    BeanDefinition handle(BeanDefinition beanDefinition, Class<?> clazz);

    /**
     * Default {@link GrpcClientBeanDefinitionHandler} implementation.
     *
     * <p> This implementation does nothing and just returns the given bean definition.
     */
    class Default implements GrpcClientBeanDefinitionHandler {
        @Nullable
        @Override
        public BeanDefinition handle(BeanDefinition beanDefinition, Class<?> clazz) {
            return beanDefinition;
        }
    }

    /**
     * {@link Blocking} only registers blocking stubs.
     *
     * <p> This is a convenience class for users who only want to use blocking stubs.
     */
    class Blocking implements GrpcClientBeanDefinitionHandler {
        @Nullable
        @Override
        public BeanDefinition handle(BeanDefinition beanDefinition, Class<?> clazz) {
            return AbstractBlockingStub.class.isAssignableFrom(clazz) ? beanDefinition : null;
        }
    }

    /**
     * {@link Future} only registers future stubs.
     *
     * <p> This is a convenience class for users who only want to use future stubs.
     */
    class Future implements GrpcClientBeanDefinitionHandler {
        @Nullable
        @Override
        public BeanDefinition handle(BeanDefinition beanDefinition, Class<?> clazz) {
            return AbstractFutureStub.class.isAssignableFrom(clazz) ? beanDefinition : null;
        }
    }

    /**
     * {@link Async} only registers async stubs.
     *
     * <p> This is a convenience class for users who only want to use async stubs.
     */
    class Async implements GrpcClientBeanDefinitionHandler {
        @Nullable
        @Override
        public BeanDefinition handle(BeanDefinition beanDefinition, Class<?> clazz) {
            return AbstractAsyncStub.class.isAssignableFrom(clazz) ? beanDefinition : null;
        }
    }
}
