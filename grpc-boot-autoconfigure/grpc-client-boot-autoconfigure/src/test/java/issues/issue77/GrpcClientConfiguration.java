package issues.issue77;

import io.grpc.ManagedChannelBuilder;
import io.grpc.testing.protobuf.SimpleServiceGrpc;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class GrpcClientConfiguration {

    @Bean
    public SimpleServiceGrpc.SimpleServiceBlockingStub simpleServiceBlockingStub(
            @Value("${grpc.server.port}") int port) {
        var channel = ManagedChannelBuilder.forAddress("localhost", port)
                .usePlaintext()
                .build();
        return SimpleServiceGrpc.newBlockingStub(channel);
    }
}
