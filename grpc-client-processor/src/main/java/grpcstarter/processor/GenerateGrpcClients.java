package grpcstarter.processor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to trigger generation of gRPC client configuration.
 *
 * <p>When this annotation is placed on a class, the annotation processor will scan
 * the specified packages for gRPC client stubs and generate a Spring Configuration
 * class that registers all found clients as beans.
 *
 * <p>Example usage:
 * <pre>{@code
 * @GenerateGrpcClients(
 *     basePackages = {"io.grpc.testing", "com.example.grpc"},
 *     authority = "localhost:9090"
 * )
 * public class GrpcClientMarker {
 * }
 * }</pre>
 *
 * @author Freeman
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface GenerateGrpcClients {

    /**
     * Base packages to scan for gRPC client stubs.
     *
     * @return array of package names
     */
    String[] basePackages();

    /**
     * gRPC server authority (host:port).
     *
     * @return authority string
     */
    String authority() default "localhost:9090";

    /**
     * Name of the generated configuration class.
     *
     * @return class name
     */
    String configurationName() default "GrpcClientsConfiguration";

    @interface Group {
        String name();

        String[] basePackages();
    }
}
