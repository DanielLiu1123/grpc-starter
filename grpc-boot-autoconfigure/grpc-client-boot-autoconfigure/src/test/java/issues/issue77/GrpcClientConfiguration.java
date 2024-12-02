package issues.issue77;

import io.grpc.ManagedChannelBuilder;
import io.grpc.testing.protobuf.SimpleServiceGrpc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class GrpcClientConfiguration {

    @Bean
    public SimpleServiceGrpc.SimpleServiceBlockingStub simpleServiceBlockingStub() {
        var channel = ManagedChannelBuilder.forAddress("localhost", Issue77Test.port)
                .usePlaintext()
                .build();
        return SimpleServiceGrpc.newBlockingStub(channel);
    }
}
