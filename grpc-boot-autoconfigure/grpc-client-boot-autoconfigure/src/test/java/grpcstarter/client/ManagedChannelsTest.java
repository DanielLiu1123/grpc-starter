package grpcstarter.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import io.grpc.ManagedChannel;
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
}
