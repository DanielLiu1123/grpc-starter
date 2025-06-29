package grpcstarter.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import grpcstarter.server.GrpcServerProperties;
import io.grpc.ManagedChannel;
import io.grpc.health.v1.HealthGrpc;
import io.grpc.testing.protobuf.SimpleServiceGrpc;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Configuration;

/**
 * Integration tests for {@link ManagedChannels}.
 *
 * @author Freeman
 */
class ManagedChannelsIT {

    @Test
    void testManagedChannels_whenNoStubsConfigured() {
        String inProcessName = UUID.randomUUID().toString();
        try (var ctx = new SpringApplicationBuilder(Cfg.class)
                .properties(GrpcServerProperties.InProcess.PREFIX + ".name=" + inProcessName)
                .properties(GrpcClientProperties.PREFIX + ".base-packages[0]=io")
                .properties(GrpcClientProperties.PREFIX + ".channels[0].name=channel1")
                .properties(GrpcClientProperties.PREFIX + ".channels[0].authority=localhost:9090")
                .properties(GrpcClientProperties.PREFIX + ".channels[1].name=channel2")
                .properties(GrpcClientProperties.PREFIX + ".channels[1].authority=localhost:9091")
                .properties(GrpcClientProperties.PREFIX + ".channels[2].authority=localhost:9092")
                .run()) {

            // No default channel, so should throw exception
            assertThatExceptionOfType(BeanCreationException.class)
                    .isThrownBy(() -> ctx.getBean(HealthGrpc.HealthBlockingStub.class));

            var managedChannels = ctx.getBean(ManagedChannels.class);

            assertThat(managedChannels.getChannel("channel1")).isSameAs(ctx.getBean("grpc-channel-channel1"));
            assertThat(managedChannels.getChannel("channel2")).isSameAs(ctx.getBean("grpc-channel-channel2"));
            assertThat(managedChannels.getChannel("unnamed-0")).isSameAs(ctx.getBean("grpc-channel-unnamed-0"));
        }
    }

    @Test
    void testManagedChannels_whenNoStubsAndProvideDefaultChannel() {
        String inProcessName = UUID.randomUUID().toString();
        try (var ctx = new SpringApplicationBuilder(Cfg.class)
                .properties(GrpcServerProperties.InProcess.PREFIX + ".name=" + inProcessName)
                .properties(GrpcClientProperties.PREFIX + ".base-packages[0]=io")
                .properties(GrpcClientProperties.PREFIX + ".authority=localhost:9090")
                .properties(GrpcClientProperties.PREFIX + ".channels[0].name=channel1")
                .properties(GrpcClientProperties.PREFIX + ".channels[0].authority=localhost:9090")
                .run()) {

            // Provide default channel, so should not throw exception
            assertThatNoException().isThrownBy(() -> ctx.getBean(HealthGrpc.HealthBlockingStub.class));
        }
    }

    @Test
    void testManagedChannels_whenMultipleChannelsConfigured() {
        String inProcessName = UUID.randomUUID().toString();
        try (var ctx = new SpringApplicationBuilder(Cfg.class)
                .properties(GrpcServerProperties.InProcess.PREFIX + ".name=" + inProcessName)
                .properties(GrpcClientProperties.PREFIX + ".base-packages[0]=io")
                .properties(GrpcClientProperties.PREFIX + ".channels[0].name=channel1")
                .properties(GrpcClientProperties.PREFIX + ".channels[0].authority=localhost:9090")
                .properties(GrpcClientProperties.PREFIX + ".channels[0].stubs[0]=io.grpc.health.**")
                .properties(GrpcClientProperties.PREFIX + ".channels[1].authority=localhost:9091")
                .properties(GrpcClientProperties.PREFIX + ".channels[1].stubs[0]=io.grpc.testing.protobuf.**")
                .run()) {

            // Trigger channel creation by getting stub beans
            var healthStub = ctx.getBean(HealthGrpc.HealthBlockingStub.class);
            var simpleStub = ctx.getBean(SimpleServiceGrpc.SimpleServiceBlockingStub.class);

            assertThat(healthStub).isNotNull();
            assertThat(simpleStub).isNotNull();

            // Channel bean names
            assertThat(ctx.getBeanNamesForType(ManagedChannel.class))
                    .containsExactlyInAnyOrder("grpc-channel-channel1", "grpc-channel-unnamed-0");

            ManagedChannels managedChannels = ctx.getBean(ManagedChannels.class);

            // Test getChannel
            ManagedChannel channel1 = managedChannels.getChannel("channel1");
            ManagedChannel channel2 = managedChannels.getChannel("unnamed-0");

            assertThat(channel1).isNotNull();
            assertThat(channel2).isNotNull();
            assertThat(channel1).isNotSameAs(channel2);

            // Test getChannel with invalid names
            assertThatThrownBy(() -> managedChannels.getChannel("nonexistent"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("No channel found with name: nonexistent");
        }
    }

    @Test
    void testManagedChannels_whenNoNamedChannels() {
        String inProcessName = UUID.randomUUID().toString();
        try (var ctx = new SpringApplicationBuilder(Cfg.class)
                .properties(GrpcServerProperties.InProcess.PREFIX + ".name=" + inProcessName)
                .properties(GrpcClientProperties.PREFIX + ".base-packages[0]=io")
                .properties(GrpcClientProperties.PREFIX + ".authority=localhost:9090")
                .run()) {

            var managedChannels = ctx.getBean(ManagedChannels.class);

            // Auto generated channel name is "unnamed-channel-0"
            assertThatNoException().isThrownBy(() -> managedChannels.getChannel("__default__"));
            assertThatNoException().isThrownBy(() -> ctx.getBean("grpc-channel-__default__"));

            assertThatThrownBy(() -> managedChannels.getChannel("any"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("No channel found with name: any");
        }
    }

    @Test
    void testManagedChannels_channelReuse() {
        String inProcessName = UUID.randomUUID().toString();
        try (var ctx = new SpringApplicationBuilder(Cfg.class)
                .properties(GrpcServerProperties.InProcess.PREFIX + ".name=" + inProcessName)
                .properties(GrpcClientProperties.PREFIX + ".base-packages[0]=io")
                .properties(GrpcClientProperties.PREFIX + ".channels[0].name=shared")
                .properties(GrpcClientProperties.PREFIX + ".channels[0].authority=localhost:9090")
                .properties(GrpcClientProperties.PREFIX + ".channels[0].stubs[0]=io.grpc.**")
                .run()) {

            // Trigger channel creation by getting stub beans
            HealthGrpc.HealthBlockingStub healthStub = ctx.getBean(HealthGrpc.HealthBlockingStub.class);
            SimpleServiceGrpc.SimpleServiceBlockingStub simpleStub =
                    ctx.getBean(SimpleServiceGrpc.SimpleServiceBlockingStub.class);

            assertThat(healthStub).isNotNull();
            assertThat(simpleStub).isNotNull();

            ManagedChannels managedChannels = ctx.getBean(ManagedChannels.class);

            // Multiple calls should return the same channel instance
            ManagedChannel channel1 = managedChannels.getChannel("shared");
            ManagedChannel channel2 = managedChannels.getChannel("shared");

            assertThat(channel1).isSameAs(channel2);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    static class Cfg {}
}
