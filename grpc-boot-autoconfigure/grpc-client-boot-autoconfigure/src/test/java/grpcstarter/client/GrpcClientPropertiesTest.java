package grpcstarter.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;

/**
 * {@link GrpcClientProperties} tester.
 */
class GrpcClientPropertiesTest {

    @Nested
    class MergeTests {

        @Test
        void shouldInheritDefaultValuesWhenChannelPropertiesAreNull() {
            // Arrange
            var properties = new GrpcClientProperties();
            properties.setAuthority("localhost:8080");
            properties.setMaxInboundMessageSize(DataSize.ofMegabytes(10));
            properties.setMaxOutboundMessageSize(DataSize.ofMegabytes(5));
            properties.setMaxInboundMetadataSize(DataSize.ofKilobytes(16));
            properties.setShutdownTimeout(3000L);
            properties.setDeadline(10000L);
            properties.setCompression("gzip");
            properties.setSslBundle("test-bundle");

            var inProcess = new GrpcClientProperties.InProcess("test-process");
            properties.setInProcess(inProcess);

            var retry = new GrpcClientProperties.Retry();
            retry.setEnabled(true);
            retry.setMaxRetryAttempts(3);
            properties.setRetry(retry);

            var defaultMetadata = List.of(new GrpcClientProperties.Metadata("default-key", List.of("default-value")));
            properties.setMetadata(defaultMetadata);

            var channel = new GrpcClientProperties.Channel();
            channel.setName("test-channel");
            // All channel properties are null, should inherit from defaults
            properties.setChannels(List.of(channel));

            // Act
            properties.merge();
            var actual = properties.getChannels().get(0);

            // Assert
            var expected = new GrpcClientProperties.Channel();
            expected.setName("test-channel");
            expected.setAuthority(null); // authority is not merged
            expected.setMaxInboundMessageSize(DataSize.ofMegabytes(10));
            expected.setMaxOutboundMessageSize(DataSize.ofMegabytes(5));
            expected.setMaxInboundMetadataSize(DataSize.ofKilobytes(16));
            expected.setShutdownTimeout(3000L);
            expected.setInProcess(inProcess);

            expected.setSslBundle("test-bundle");
            expected.setRetry(retry);
            expected.setDeadline(10000L);
            expected.setCompression("gzip");
            expected.setMetadata(defaultMetadata);
            expected.setClasses(List.of());
            expected.setStubs(List.of());
            expected.setServices(List.of());

            assertThat(actual).isEqualTo(expected);
        }
    }
}
