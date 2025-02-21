package issues;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.util.ReflectionTestUtils.getField;

import grpcstarter.client.EnableGrpcClients;
import io.grpc.health.v1.HealthGrpc;
import io.grpc.testing.protobuf.SimpleServiceGrpc;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Configuration;

/**
 * @author Freeman
 * @see <a href="https://github.com/DanielLiu1123/grpc-starter/issues/23">One channel config create one single channel</a>
 */
class Issue23Test {

    @Test
    void testOneConfigCreateOneChannel() {
        try (var ctx = new SpringApplicationBuilder(Cfg.class)
                .properties("grpc.client.authority=localhost:9090")
                .properties("grpc.server.enabled=false")
                .run()) {

            var simpleStub = ctx.getBean(SimpleServiceGrpc.SimpleServiceBlockingStub.class);
            var simpleFutureStub = ctx.getBean(SimpleServiceGrpc.SimpleServiceFutureStub.class);
            var healthStub = ctx.getBean(HealthGrpc.HealthBlockingStub.class);

            assertThat(getField(simpleStub, "channel"))
                    .isSameAs(getField(simpleFutureStub, "channel"))
                    .isSameAs(getField(healthStub, "channel"));
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EnableGrpcClients("io.grpc")
    static class Cfg {}
}
