package grpcstarter.example;

import static transcoding.mvc.SimpleServiceGrpc.SimpleServiceImplBase;

import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import java.util.Map;
import java.util.Objects;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.util.Assert;
import org.springframework.web.client.RestClient;
import transcoding.mvc.Simpleservice;
import transcoding.mvc.Simpleservice.SimpleRequest;

/**
 * @author Freeman
 */
@Slf4j
@SpringBootApplication
public class TranscodingMvcApp extends SimpleServiceImplBase {

    public static void main(String[] args) {
        var ctx = SpringApplication.run(TranscodingMvcApp.class, args);

        // We need to do `./gradlew nativeRun` in CI, it needs to be closed.
        // When running the example locally, no need to close it.
        // See
        // https://docs.github.com/en/actions/writing-workflows/choosing-what-your-workflow-does/store-information-in-variables#default-environment-variables
        if (System.getenv("CI") != null) {
            ctx.close();
        }
    }

    @Override
    public void unaryRpc(SimpleRequest request, StreamObserver<Simpleservice.SimpleResponse> r) {
        if (request.getRequestMessage().contains("err")) {
            var metadata = new Metadata();
            metadata.put(Metadata.Key.of("error", Metadata.ASCII_STRING_MARSHALLER), "invalid argument");
            throw new StatusRuntimeException(Status.INVALID_ARGUMENT, metadata);
        }

        r.onNext(Simpleservice.SimpleResponse.newBuilder()
                .setResponseMessage("Hello " + request.getRequestMessage())
                .build());
        r.onCompleted();
    }

    @Override
    @SneakyThrows
    public void serverStreamingRpc(SimpleRequest request, StreamObserver<Simpleservice.SimpleResponse> r) {
        for (int i = 0; i < 10; i++) {
            r.onNext(Simpleservice.SimpleResponse.newBuilder()
                    .setResponseMessage("Hello %s %d".formatted(request.getRequestMessage(), i))
                    .build());
            Thread.sleep(1000);
        }
        r.onCompleted();
    }

    @Bean
    ApplicationRunner runner(RestClient.Builder builder, WebServerApplicationContext ctx) {
        return args -> {
            var client = builder.baseUrl(
                            "http://localhost:" + ctx.getWebServer().getPort())
                    .build();

            var response = client.post()
                    .uri("/unary")
                    .body(Map.of("requestMessage", "World"))
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});
            log.info("response: {}", response);

            Assert.notNull(response, "response is null");
            Assert.isTrue(Objects.equals(response.get("responseMessage"), "Hello World"), "response message not match");
        };
    }
}
