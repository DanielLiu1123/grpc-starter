package grpcstarter.extensions.test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation at the field that injects the gRPC server port that was allocated at runtime.
 *
 * <p> Example:
 *
 * <pre>{@code
 * @SpringBootTest(properties = "grpc.test.server.port-type=RANDOM_PORT")
 * class FooTest{
 *     @LocalGrpcPort
 *     int port;
 * }
 * }</pre>
 *
 * @author Freeman
 * @see org.springframework.boot.web.server.test.LocalServerPort
 */
@Target({ElementType.FIELD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface LocalGrpcPort {}
