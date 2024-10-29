package grpcstarter.example;

import static transcoding.flux.SimpleServiceGrpc.SimpleServiceImplBase;
import static transcoding.flux.Simpleservice.SimpleRequest;
import static transcoding.flux.Simpleservice.SimpleResponse;

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
import org.springframework.context.annotation.Bean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.env.Environment;
import org.springframework.util.Assert;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * @author Freeman
 */
@Slf4j
@SpringBootApplication
public class TranscodingWebFluxApp extends SimpleServiceImplBase {

    public static void main(String[] args) {
        var ctx = SpringApplication.run(TranscodingWebFluxApp.class, args);

        // We need to do `./gradlew nativeRun` in CI, it needs to be closed.
        // When running the example locally, no need to close it.
        // See
        // https://docs.github.com/en/actions/writing-workflows/choosing-what-your-workflow-does/store-information-in-variables#default-environment-variables
        if (System.getenv("CI") != null) {
            ctx.close();
        }
    }

    @Override
    public void unaryRpc(SimpleRequest request, StreamObserver<SimpleResponse> r) {
        if (request.getRequestMessage().contains("err")) {
            var metadata = new Metadata();
            metadata.put(Metadata.Key.of("error", Metadata.ASCII_STRING_MARSHALLER), "invalid argument");
            throw new StatusRuntimeException(Status.INVALID_ARGUMENT, metadata);
        }

        r.onNext(SimpleResponse.newBuilder()
                .setResponseMessage("Hello " + request.getRequestMessage())
                .build());
        r.onCompleted();
    }

    @Override
    @SneakyThrows
    public void serverStreamingRpc(SimpleRequest request, StreamObserver<SimpleResponse> r) {
        for (int i = 0; i < 10; i++) {
            r.onNext(SimpleResponse.newBuilder()
                    .setResponseMessage("Hello %s %d".formatted(request.getRequestMessage(), i))
                    .build());
            Thread.sleep(1000);
        }
        r.onCompleted();
    }

    @Bean
    ApplicationRunner runner(WebClient.Builder builder, Environment env) {
        var port = env.getProperty("server.port", int.class, 8080);
        return args -> {
            var client = builder.baseUrl("http://localhost:" + port).build();

            var response = client.post()
                    .uri("/unary")
                    .bodyValue(Map.of("requestMessage", "World"))
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();
            log.info("response: {}", response);

            Assert.notNull(response, "response is null");
            Assert.isTrue(Objects.equals(response.get("responseMessage"), "Hello World"), "response message not match");
        };
    }
}
