package com.freemanan.starter.grpc.server;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Controller;

/**
 * Alias for {@link Controller}, for gRPC service implementation.
 *
 * @author Freeman
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Controller
public @interface GrpcService {
    /**
     * @see Controller#value()
     */
    @AliasFor(annotation = Controller.class)
    String value() default "";
}
