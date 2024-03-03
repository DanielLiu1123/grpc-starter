package com.freemanan.starter.grpc.server;

import static io.grpc.health.v1.HealthCheckResponse.ServingStatus;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.freemanan.starter.grpc.server.feature.health.HealthChecker;
import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import io.grpc.health.v1.HealthCheckRequest;
import io.grpc.health.v1.HealthCheckResponse;
import io.grpc.health.v1.HealthGrpc;
import io.grpc.inprocess.InProcessChannelBuilder;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Freeman
 */
@ExtendWith(OutputCaptureExtension.class)
class HealthIT {

    @Test
    void testExternalHealthChecker() {
        String name = UUID.randomUUID().toString();
        ConfigurableApplicationContext ctx = new SpringApplicationBuilder(Cfg.class)
                .properties(GrpcServerProperties.InProcess.PREFIX + ".name=" + name)
                .run();

        List<HealthChecker> checkers =
                ctx.getBeanProvider(HealthChecker.class).orderedStream().collect(Collectors.toList());

        assertThat(checkers).hasSize(1);
        assertThat(checkers.get(0).service()).isEqualTo("foo");

        ManagedChannel chan =
                InProcessChannelBuilder.forName(name).directExecutor().build();
        HealthGrpc.HealthBlockingStub stub = HealthGrpc.newBlockingStub(chan);

        // check existing service
        HealthCheckResponse resp =
                stub.check(HealthCheckRequest.newBuilder().setService("foo").build());
        assertThat(resp.getStatus()).isEqualTo(ServingStatus.SERVING);

        // check unknown service
        HealthCheckRequest req1 =
                HealthCheckRequest.newBuilder().setService("bar").build();
        assertThatExceptionOfType(StatusRuntimeException.class)
                .isThrownBy(() -> stub.check(req1))
                .withMessage("NOT_FOUND: unknown service bar");

        // service is case-sensitive
        HealthCheckRequest req2 =
                HealthCheckRequest.newBuilder().setService("Foo").build();
        assertThatExceptionOfType(StatusRuntimeException.class)
                .isThrownBy(() -> stub.check(req2))
                .withMessage("NOT_FOUND: unknown service Foo");

        chan.shutdown();
        ctx.close();
    }

    @Test
    void testDuplicateService(CapturedOutput output) {
        String name = UUID.randomUUID().toString();
        assertThatCode(() -> new SpringApplicationBuilder(DuplicatedServiceCfg.class)
                        .properties(GrpcServerProperties.InProcess.PREFIX + ".name=" + name)
                        .run()
                        .close())
                .doesNotThrowAnyException();
        assertThat(output).contains("Duplicate service name for health checker:");
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    static class Cfg {

        @Bean
        HealthChecker fooHealthChecker() {
            return new HealthChecker() {
                @Override
                public String service() {
                    return "foo";
                }

                @Override
                public boolean check() {
                    return true;
                }
            };
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    static class DuplicatedServiceCfg {

        @Bean
        HealthChecker fooHealthChecker1() {
            return new HealthChecker() {
                @Override
                public String service() {
                    return "foo";
                }

                @Override
                public boolean check() {
                    return true;
                }
            };
        }

        @Bean
        HealthChecker fooHealthChecker2() {
            return new HealthChecker() {
                @Override
                public String service() {
                    return "foo";
                }

                @Override
                public boolean check() {
                    return true;
                }
            };
        }
    }
}
