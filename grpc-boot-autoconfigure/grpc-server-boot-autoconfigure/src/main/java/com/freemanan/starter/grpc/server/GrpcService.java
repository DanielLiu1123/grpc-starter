package com.freemanan.starter.grpc.server;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Component;

/**
 * Alias for {@link Component}, for gRPC service implementation.
 *
 * @author Freeman
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Component
public @interface GrpcService {
    /**
     * @see Component#value()
     */
    @AliasFor(annotation = Component.class)
    String value() default "";
}
