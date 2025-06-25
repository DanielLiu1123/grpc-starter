package grpcstarter.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import io.grpc.ManagedChannel;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ManagedChannelsImpl}.
 *
 * @author Freeman
 */
class ManagedChannelsTest {

    private ManagedChannels managedChannels;

    @BeforeEach
    void setUp() {
        Cache.clear();
        managedChannels = new ManagedChannelsImpl();
    }

    @AfterEach
    void tearDown() {
        Cache.clear();
    }

    @Test
    void testGetChannel_whenChannelExists() {
        // Given
        ManagedChannel mockChannel = mock(ManagedChannel.class);
        GrpcClientProperties.Channel channelConfig = new GrpcClientProperties.Channel();
        channelConfig.setName("test-channel");
        channelConfig.setShutdownTimeout(5000L);

        // Simulate channel creation and caching
        Cache.getOrSupplyChannel(channelConfig, () -> mockChannel);

        // When
        ManagedChannel result = managedChannels.getChannel("test-channel");

        // Then
        assertThat(result).isSameAs(mockChannel);
    }

    @Test
    void testGetChannel_whenChannelDoesNotExist() {
        // When & Then
        assertThatThrownBy(() -> managedChannels.getChannel("nonexistent"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("No channel found with name: nonexistent");
    }

    @Test
    void testGetChannel_whenNameIsNull() {
        // When & Then
        assertThatThrownBy(() -> managedChannels.getChannel(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Channel name must not be null or empty");
    }

    @Test
    void testGetChannel_whenNameIsEmpty() {
        // When & Then
        assertThatThrownBy(() -> managedChannels.getChannel(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Channel name must not be null or empty");
    }

    @Test
    void testGetChannelNames_whenNoChannels() {
        // When
        Set<String> result = managedChannels.getChannelNames();

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void testGetChannelNames_whenMultipleChannels() {
        // Given
        ManagedChannel mockChannel1 = mock(ManagedChannel.class);
        ManagedChannel mockChannel2 = mock(ManagedChannel.class);

        GrpcClientProperties.Channel channelConfig1 = new GrpcClientProperties.Channel();
        channelConfig1.setName("channel1");
        channelConfig1.setShutdownTimeout(5000L);
        GrpcClientProperties.Channel channelConfig2 = new GrpcClientProperties.Channel();
        channelConfig2.setName("channel2");
        channelConfig2.setShutdownTimeout(5000L);

        Cache.getOrSupplyChannel(channelConfig1, () -> mockChannel1);
        Cache.getOrSupplyChannel(channelConfig2, () -> mockChannel2);

        // When
        Set<String> result = managedChannels.getChannelNames();

        // Then
        assertThat(result).containsExactlyInAnyOrder("channel1", "channel2");
    }

    @Test
    void testHasChannel_whenChannelExists() {
        // Given
        ManagedChannel mockChannel = mock(ManagedChannel.class);
        GrpcClientProperties.Channel channelConfig = new GrpcClientProperties.Channel();
        channelConfig.setName("test-channel");
        channelConfig.setShutdownTimeout(5000L);

        Cache.getOrSupplyChannel(channelConfig, () -> mockChannel);

        // When & Then
        assertThat(managedChannels.hasChannel("test-channel")).isTrue();
    }

    @Test
    void testHasChannel_whenChannelDoesNotExist() {
        // When & Then
        assertThat(managedChannels.hasChannel("nonexistent")).isFalse();
    }

    @Test
    void testHasChannel_whenNameIsNull() {
        // When & Then
        assertThat(managedChannels.hasChannel(null)).isFalse();
    }

    @Test
    void testHasChannel_whenNameIsEmpty() {
        // When & Then
        assertThat(managedChannels.hasChannel("")).isFalse();
    }

    @Test
    void testDefaultChannelNotIncluded() {
        // Given - create a default channel (should not be included in named channels)
        ManagedChannel mockChannel = mock(ManagedChannel.class);
        GrpcClientProperties.Channel defaultChannelConfig = new GrpcClientProperties.Channel();
        defaultChannelConfig.setName("__default__");
        defaultChannelConfig.setShutdownTimeout(5000L);

        Cache.getOrSupplyChannel(defaultChannelConfig, () -> mockChannel);

        // When
        Set<String> result = managedChannels.getChannelNames();

        // Then - default channel should not be included
        assertThat(result).isEmpty();
        assertThat(managedChannels.hasChannel("__default__")).isFalse();
    }
}
