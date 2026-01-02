package grpcstarter.client;

import static org.assertj.core.api.Assertions.assertThat;

import io.grpc.health.v1.HealthGrpc;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Configuration;

/**
 * @author Freeman
 */
@ExtendWith(OutputCaptureExtension.class)
class MultipleMatchedChannelsIT {

    @Test
    void whenMatchedMultipleChannels_shouldUseFirstOneAndLogWarning(CapturedOutput output) {
        try (var ctx = new SpringApplicationBuilder(Cfg.class)
                .properties("grpc.client.base-packages[0]=io.grpc")
                .properties("grpc.client.channels[0].name=ch1")
                .properties("grpc.client.channels[0].authority=localhost:9091")
                .properties("grpc.client.channels[0].classes[0]=io.grpc.health.v1.HealthGrpc$HealthBlockingStub")
                .properties("grpc.client.channels[1].name=ch2")
                .properties("grpc.client.channels[1].authority=localhost:9092")
                .properties("grpc.client.channels[1].stubs[0]=io.grpc.health.v1.HealthGrpc$HealthBlockingStub")
                .properties("grpc.client.channels[2].name=ch3")
                .properties("grpc.client.channels[2].authority=localhost:9093")
                .properties("grpc.client.channels[2].services[0]=grpc.health.v1.Health")
                .run()) {

            HealthGrpc.HealthBlockingStub stub = ctx.getBean(HealthGrpc.HealthBlockingStub.class);

            // Channel should be the first matched one (ch1)
            assertThat(stub.getChannel().authority()).isEqualTo("localhost:9091");

            assertThat(output.getOut())
                    .contains(
                            "gRPC client [io.grpc.health.v1.HealthGrpc$HealthBlockingStub] matched multiple channels: [ch1, ch2, ch3], using 'ch1' with authority:");
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    static class Cfg {}
}
