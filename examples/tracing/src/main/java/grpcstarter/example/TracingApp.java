package grpcstarter.example;

import grpcstarter.client.EnableGrpcClients;
import io.grpc.testing.protobuf.SimpleRequest;
import io.grpc.testing.protobuf.SimpleResponse;
import io.grpc.testing.protobuf.SimpleServiceGrpc;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/**
 * @author Freeman
 */
@SpringBootApplication
@EnableGrpcClients("io.grpc")
public class TracingApp {

    public static void main(String[] args) {
        SpringApplication.run(TracingApp.class, args);
    }

    @Bean
    public SimpleApi simpleApi(RestClient.Builder builder, Environment env) {
        builder.baseUrl("http://localhost:" + env.getProperty("server.port", "8080"));
        var factory = HttpServiceProxyFactory.builder()
                .exchangeAdapter(RestClientAdapter.create(builder.build()))
                .build();
        return factory.createClient(SimpleApi.class);
    }

    @Bean
    ApplicationRunner runner(SimpleServiceGrpc.SimpleServiceBlockingStub stub) {
        return args -> {
            SimpleResponse resp = stub.unaryRpc(
                    SimpleRequest.newBuilder().setRequestMessage("Hello").build());
            System.out.println(resp);
        };
    }
}
