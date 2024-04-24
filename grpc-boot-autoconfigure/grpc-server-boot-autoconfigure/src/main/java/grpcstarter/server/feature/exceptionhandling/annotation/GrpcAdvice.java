package grpcstarter.server.feature.exceptionhandling.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AliasFor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Specialization of {@link Component @Component} for classes that declare
 * {@link GrpcExceptionHandler @GrpcExceptionHandler} methods to handle exceptions.
 *
 * <p> Usually used with {@link GrpcExceptionHandler @GrpcExceptionHandler} together.
 *
 * <p> {@link GrpcAdvice} can work with {@link Order} and {@link Ordered},
 * exceptions will be handled in order.
 *
 * @author Freeman
 * @see GrpcExceptionHandler
 * @see Order
 * @see Ordered
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Component
public @interface GrpcAdvice {
    /**
     * @see Component#value()
     */
    @AliasFor(annotation = Component.class)
    String value() default "";
}
