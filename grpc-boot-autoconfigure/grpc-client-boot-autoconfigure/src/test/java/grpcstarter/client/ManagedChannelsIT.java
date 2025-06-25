package grpcstarter.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import grpcstarter.server.GrpcServerProperties;
import io.grpc.ManagedChannel;
import io.grpc.health.v1.HealthGrpc;
import io.grpc.testing.protobuf.SimpleServiceGrpc;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
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
    void testManagedChannels_whenMultipleChannelsConfigured() {
        String inProcessName = UUID.randomUUID().toString();
        try (var ctx = new SpringApplicationBuilder(Cfg.class)
                .properties(GrpcServerProperties.InProcess.PREFIX + ".name=" + inProcessName)
                .properties(GrpcClientProperties.PREFIX + ".base-packages[0]=io")
                .properties(GrpcClientProperties.PREFIX + ".channels[0].name=channel1")
                .properties(GrpcClientProperties.PREFIX + ".channels[0].authority=localhost:9090")
                .properties(GrpcClientProperties.PREFIX + ".channels[0].stubs[0]=io.grpc.health.v1.**")
                .properties(GrpcClientProperties.PREFIX + ".channels[1].name=channel2")
                .properties(GrpcClientProperties.PREFIX + ".channels[1].authority=localhost:9091")
                .properties(GrpcClientProperties.PREFIX + ".channels[1].stubs[0]=io.grpc.testing.protobuf.**")
                .run()) {

            // Trigger channel creation by getting stub beans
            HealthGrpc.HealthBlockingStub healthStub = ctx.getBean(HealthGrpc.HealthBlockingStub.class);
            SimpleServiceGrpc.SimpleServiceBlockingStub simpleStub =
                    ctx.getBean(SimpleServiceGrpc.SimpleServiceBlockingStub.class);

            assertThat(healthStub).isNotNull();
            assertThat(simpleStub).isNotNull();

            ManagedChannels managedChannels = ctx.getBean(ManagedChannels.class);

            // Test getChannelNames
            Set<String> channelNames = managedChannels.getChannelNames();
            assertThat(channelNames).containsExactlyInAnyOrder("channel1", "channel2");

            // Test hasChannel
            assertThat(managedChannels.hasChannel("channel1")).isTrue();
            assertThat(managedChannels.hasChannel("channel2")).isTrue();
            assertThat(managedChannels.hasChannel("nonexistent")).isFalse();
            assertThat(managedChannels.hasChannel(null)).isFalse();
            assertThat(managedChannels.hasChannel("")).isFalse();

            // Test getChannel
            ManagedChannel channel1 = managedChannels.getChannel("channel1");
            ManagedChannel channel2 = managedChannels.getChannel("channel2");

            assertThat(channel1).isNotNull();
            assertThat(channel2).isNotNull();
            assertThat(channel1).isNotSameAs(channel2);

            // Test getChannel with invalid names
            assertThatThrownBy(() -> managedChannels.getChannel("nonexistent"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("No channel found with name: nonexistent");

            assertThatThrownBy(() -> managedChannels.getChannel(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Channel name must not be null or empty");

            assertThatThrownBy(() -> managedChannels.getChannel(""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Channel name must not be null or empty");
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

            ManagedChannels managedChannels = ctx.getBean(ManagedChannels.class);

            // When no named channels are configured, should return empty set
            Set<String> channelNames = managedChannels.getChannelNames();
            assertThat(channelNames).isEmpty();

            assertThat(managedChannels.hasChannel("any")).isFalse();

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
