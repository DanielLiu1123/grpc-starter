package com.freemanan.starter.grpc.client;

import io.grpc.stub.AbstractStub;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AliasFor;

/**
 * @author Freeman
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Import({GrpcClientsRegistrar.class})
public @interface EnableGrpcClients {
    /**
     * Scan base packages.
     *
     * <p> Scan the package of the annotated class by default.
     * <p> Alias for the {@link #basePackages()} attribute.
     *
     * @return the base packages to scan
     */
    @AliasFor("basePackages")
    String[] value() default {};

    /**
     * Alias for the {@link #value()} attribute.
     *
     * @return the base packages to scan
     * @see #value()
     */
    @AliasFor("value")
    String[] basePackages() default {};

    /**
     * The classes to register as gRPC client beans.
     *
     * <p> clients and {@link #basePackages} <strong>can</strong> be used together.
     *
     * @return the classes to register as gRPC client beans.
     */
    Class<? extends AbstractStub>[] clients() default {};
}
