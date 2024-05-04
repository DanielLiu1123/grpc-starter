package grpcstarter.extensions.test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation at the field that injects the gRPC server in-process name.
 *
 * <p> Example:
 *
 * <pre>{@code
 * @SpringBootTest
 * class FooTest{
 *     @InProcessName
 *     String inProcessName;
 * }
 * }</pre>
 *
 * @author Freeman
 */
@Target({ElementType.FIELD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface InProcessName {}
