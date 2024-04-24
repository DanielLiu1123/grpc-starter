package grpcstarter.server;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;

/**
 * Mark a gRPC service implementation.
 *
 * <p> This annotation is an alias for {@link Component}.
 *
 * @author Freeman
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Component
public @interface GrpcService {
    /**
     * @see Controller#value()
     */
    @AliasFor(annotation = Component.class)
    String value() default "";
}
