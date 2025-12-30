package grpcstarter.server;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Component;

/**
 * Mark a gRPC service implementation, an alias for {@link Component}.
 *
 * <p> This annotation is optional, can be replaced by any {@link Component} based annotation.
 *
 * @author Freeman
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Component
public @interface GrpcService {
    /**
     * Alias for {@link Component#value}.
     *
     * @see Component#value()
     */
    @AliasFor(annotation = Component.class)
    String value() default "";
}
