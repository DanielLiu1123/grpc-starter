package issues;

import static org.assertj.core.api.Assertions.assertThat;

import com.freemanan.starter.grpc.client.EnableGrpcClients;
import io.grpc.health.v1.HealthGrpc;
import io.grpc.testing.protobuf.SimpleServiceGrpc;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Freeman
 * @see <a href="https://github.com/DanielLiu1123/grpc-starter/issues/23">issue 23</a>
 */
class Issue23Test {

    @Test
    void testOneConfigCreateOneChannel() {
        ConfigurableApplicationContext ctx = new SpringApplicationBuilder(Cfg.class)
                .properties("grpc.client.authority=localhost:9090")
                .properties("grpc.server.enabled=false")
                .run();

        SimpleServiceGrpc.SimpleServiceBlockingStub simpleStub =
                ctx.getBean(SimpleServiceGrpc.SimpleServiceBlockingStub.class);
        SimpleServiceGrpc.SimpleServiceFutureStub simpleFutureStub =
                ctx.getBean(SimpleServiceGrpc.SimpleServiceFutureStub.class);
        HealthGrpc.HealthBlockingStub healthStub = ctx.getBean(HealthGrpc.HealthBlockingStub.class);

        assertThat(ReflectionTestUtils.getField(simpleStub, "channel"))
                .isSameAs(ReflectionTestUtils.getField(simpleFutureStub, "channel"))
                .isSameAs(ReflectionTestUtils.getField(healthStub, "channel"));

        ctx.close();
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EnableGrpcClients("io.grpc")
    static class Cfg {}
}
