package grpcstarter.extensions.transcoding;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.protobuf.SimpleRequest;
import io.grpc.testing.protobuf.SimpleResponse;
import io.grpc.testing.protobuf.SimpleServiceGrpc;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * {@link TranscodingExceptionResolver}/{@link ReactiveTranscodingExceptionResolver} tester.
 */
class TranscodingExceptionHandlerIT {

    @Nested
    @SpringBootTest(
            classes = TranscodingExceptionHandlerIT.Cfg.class,
            webEnvironment = RANDOM_PORT,
            properties = "spring.main.web-application-type=servlet")
    class Servlet {

        @LocalServerPort
        int port;

        final RestTestClient client = RestTestClient.bindToServer().build();

        @Test
        void testTranscodingExceptionResolver() {
            var response = client.post()
                    .uri("http://localhost:" + port + "/grpc.testing.SimpleService/UnaryRpc")
                    .exchange();

            response.expectStatus().isEqualTo(BAD_REQUEST);
            response.expectBody(String.class).isEqualTo("Ops!");
        }
    }

    @Nested
    @SpringBootTest(
            classes = TranscodingExceptionHandlerIT.Cfg.class,
            webEnvironment = RANDOM_PORT,
            properties = "spring.main.web-application-type=reactive")
    class Reactive {

        @LocalServerPort
        int port;

        final RestTestClient client = RestTestClient.bindToServer().build();

        @Test
        void testTranscodingExceptionResolver() {
            var resp = client.post()
                    .uri("http://localhost:" + port + "/grpc.testing.SimpleService/UnaryRpc")
                    .exchange();

            resp.expectStatus().isEqualTo(BAD_REQUEST);
            resp.expectBody(String.class).isEqualTo("Ops!");
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    static class Cfg extends SimpleServiceGrpc.SimpleServiceImplBase {

        @Bean
        public TranscodingExceptionResolver transcodingExceptionResolver() {
            return exception -> ServerResponse.status(TranscodingUtil.toHttpStatus(exception.getStatus()))
                    .body("Ops!");
        }

        @Bean
        public ReactiveTranscodingExceptionResolver reactiveTranscodingExceptionResolver() {
            return (sink, exception) -> org.springframework.web.reactive.function.server.ServerResponse.status(
                            TranscodingUtil.toHttpStatus(exception.getStatus()))
                    .bodyValue("Ops!")
                    .subscribe(sink::success);
        }

        @Override
        public void unaryRpc(SimpleRequest request, StreamObserver<SimpleResponse> responseObserver) {
            throw new StatusRuntimeException(Status.INVALID_ARGUMENT);
        }
    }
}
