package grpcstarter.client;

import static grpcstarter.client.Util.matchPattern;
import static org.assertj.core.api.Assertions.assertThat;

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
}
