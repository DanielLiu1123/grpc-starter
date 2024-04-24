package grpcstarter.server.feature.exceptionhandling.annotation;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@link GrpcExceptionHandler} is used to mark an exception handler method.
 *
 * <p> Supported return types are:
 * <ul>
 *     <li>{@link io.grpc.Status}</li>
 *     <li>{@link io.grpc.StatusException}</li>
 *     <li>{@link io.grpc.StatusRuntimeException}</li>
 *     <li>{@link Throwable}</li>
 * </ul>
 *
 * <p> Basic usage:
 * <pre>{@code
 * @GrpcAdvice
 * public class ExceptionAdvice {
 *     @GrpcExceptionHandler
 *     public StatusRuntimeException handleRuntimeException(RuntimeException e) {
 *         return Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException();
 *     }
 * }
 * }</pre>
 *
 * <p> Inject {@link ServerCall} and {@link Metadata}:
 *
 * <pre>{@code
 * @GrpcAdvice
 * public class ExceptionAdvice {
 *     @GrpcExceptionHandler
 *     public StatusRuntimeException handleRuntimeException(RuntimeException e, ServerCall<?, ?> call, Metadata headers) {
 *         return Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException();
 *     }
 * }
 * }</pre>
 *
 * @author Freeman
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface GrpcExceptionHandler {
    /**
     * Exceptions handled by the annotated method. If empty, will default to any
     * exceptions listed in the method argument list.
     */
    Class<? extends Throwable>[] value() default {};
}
