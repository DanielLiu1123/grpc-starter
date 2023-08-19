package com.freemanan.starter.grpc.server.feature.exceptionhandling.annotation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.freemanan.starter.grpc.server.GrpcService;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.protobuf.SimpleRequest;
import io.grpc.testing.protobuf.SimpleResponse;
import io.grpc.testing.protobuf.SimpleServiceGrpc;
import java.util.MissingResourceException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;

/**
 * @author Freeman
 */
@SpringBootTest(
        classes = AnnotationBasedExceptionHandlingIT.Cfg.class,
        properties = {
            "grpc.server.in-process.name=AnnotationBasedIT",
            "grpc.client.in-process.name=AnnotationBasedIT",
            "grpc.client.base-packages[0]=io.grpc"
        })
@ExtendWith(OutputCaptureExtension.class)
class AnnotationBasedExceptionHandlingIT {

    @Autowired
    SimpleServiceGrpc.SimpleServiceBlockingStub stub;

    @Test
    void testAnnotationBasedExceptionHandler(CapturedOutput output) {
        assertThatCode(() -> stub.unaryRpc(SimpleRequest.newBuilder()
                        .setRequestMessage("IllegalArgumentException")
                        .build()))
                .isInstanceOf(StatusRuntimeException.class)
                .hasMessage("INVALID_ARGUMENT");
        assertThatCode(() -> stub.unaryRpc(SimpleRequest.newBuilder()
                        .setRequestMessage("MissingResourceException")
                        .build()))
                .isInstanceOf(StatusRuntimeException.class)
                .hasMessage("NOT_FOUND");
        assertThatCode(() -> stub.unaryRpc(SimpleRequest.newBuilder()
                        .setRequestMessage("RuntimeException")
                        .build()))
                .isInstanceOf(StatusRuntimeException.class)
                .hasMessage("INTERNAL");
        assertThatCode(() -> stub.unaryRpc(SimpleRequest.newBuilder()
                        .setRequestMessage("NestedException")
                        .build()))
                .isInstanceOf(StatusRuntimeException.class)
                .hasMessage("INVALID_ARGUMENT: java.lang.RuntimeException");

        assertThat(output).contains("The one with higher priority will be used:");
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @GrpcAdvice
    @GrpcService
    @Import(ExceptionAdvice2.class)
    static class Cfg extends SimpleServiceGrpc.SimpleServiceImplBase {

        @Override
        public void unaryRpc(SimpleRequest request, StreamObserver<SimpleResponse> responseObserver) {
            String msg = request.getRequestMessage();
            switch (msg) {
                case "IllegalArgumentException":
                    throw new IllegalArgumentException();
                case "MissingResourceException":
                    throw new MissingResourceException(null, "SimpleService", "unaryRpc");
                case "RuntimeException":
                    throw new RuntimeException();
                case "NestedException":
                    throw new IllegalArgumentException(new RuntimeException());
                default:
                    responseObserver.onNext(
                            SimpleResponse.newBuilder().setResponseMessage(msg).build());
                    responseObserver.onCompleted();
                    break;
            }
        }

        @GrpcExceptionHandler
        public StatusRuntimeException illegalArgumentExceptionHandler(IllegalArgumentException e, Metadata headers) {
            return Status.INVALID_ARGUMENT
                    .withDescription(e.getMessage())
                    .withCause(e)
                    .asRuntimeException();
        }

        @GrpcExceptionHandler(MissingResourceException.class)
        public StatusRuntimeException missingResourceExceptionHandler(
                ServerCall<?, ?> call, RuntimeException e, Metadata headers) {
            return Status.NOT_FOUND.withDescription(e.getMessage()).withCause(e).asRuntimeException();
        }

        @GrpcExceptionHandler(RuntimeException.class)
        public StatusRuntimeException runtimeExceptionHandler(RuntimeException e) {
            return Status.INTERNAL.withDescription(e.getMessage()).withCause(e).asRuntimeException();
        }
    }

    @GrpcAdvice
    @Order(1)
    static class ExceptionAdvice2 {
        @GrpcExceptionHandler
        public StatusRuntimeException illegalArgumentExceptionHandler(IllegalArgumentException e) {
            return Status.INVALID_ARGUMENT
                    .withDescription(e.getMessage())
                    .withCause(e)
                    .asRuntimeException();
        }

        @GrpcExceptionHandler
        public StatusRuntimeException exceptionHandler(Exception e) {
            return Status.INTERNAL.withDescription(e.getMessage()).withCause(e).asRuntimeException();
        }
    }
}
