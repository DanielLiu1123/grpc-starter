package grpcstarter.client;

import io.grpc.stub.AbstractStub;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * gRPC client enabled condition.
 *
 * @author Freeman
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@ConditionalOnClass(AbstractStub.class)
@ConditionalOnProperty(prefix = GrpcClientProperties.PREFIX, name = "enabled", matchIfMissing = true)
public @interface ConditionOnGrpcClientEnabled {}
