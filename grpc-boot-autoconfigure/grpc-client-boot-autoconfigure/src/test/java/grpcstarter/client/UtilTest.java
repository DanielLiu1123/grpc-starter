package grpcstarter.client;

import static grpcstarter.client.Util.matchPattern;
import static org.assertj.core.api.Assertions.assertThat;

import io.grpc.health.v1.HealthGrpc;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * {@link Util} tester.
 */
class UtilTest {

    /**
     * {@link Util#matchPattern(String, String)}
     */
    @Test
    void testMatchPattern() {
        assertThat(matchPattern("pet.v*.*Service", "pet.v1.PetService")).isTrue();
        assertThat(matchPattern("pet.v*.*Service", "pet.v10.PetService")).isTrue();
        assertThat(matchPattern("pet.v*.*Service", "pet.v1.PetService2")).isFalse();
        assertThat(matchPattern("pet.v*.*Service", "pet.PetService")).isFalse();

        assertThat(matchPattern("pet.**", "pet.v2.PetService")).isTrue();
        assertThat(matchPattern("pet.**", "pet")).isTrue();

        assertThat(matchPattern("pet.*.PetService", "pet.v1.PetService")).isTrue();
        assertThat(matchPattern("pet.*.PetService", "pet.PetService")).isFalse();
        assertThat(matchPattern("pet.**.PetService", "pet.PetService")).isTrue();
        assertThat(matchPattern("pet.*.PetService", "pet.v1.foo.PetService2")).isFalse();

        assertThat(matchPattern("pet.v?.PetService", "pet.v1.PetService")).isTrue();
        assertThat(matchPattern("pet.v?.PetService", "pet.v10.PetService")).isFalse();

        assertThat(matchPattern("pet.v1", "pet.v1.PetService")).isFalse();
    }

    @Test
    void testFindMatchedConfig_multipleMatches() {
        GrpcClientProperties properties = new GrpcClientProperties();

        GrpcClientProperties.Channel channel1 = new GrpcClientProperties.Channel();
        channel1.setName("channel1");
        channel1.setAuthority("localhost:1001");
        channel1.setServices(List.of("grpc.health.v1.Health"));

        GrpcClientProperties.Channel channel2 = new GrpcClientProperties.Channel();
        channel2.setName("channel2");
        channel2.setAuthority("localhost:1002");
        channel2.setServices(List.of("grpc.health.v*.*"));

        properties.setChannels(List.of(channel1, channel2));

        // Should return the first matched one
        var matched = Util.findMatchedConfig(HealthGrpc.HealthBlockingStub.class, properties);

        assertThat(matched).isPresent();
        assertThat(matched.get().getName()).isEqualTo("channel1");
    }
}
