package com.freemanan.starter.grpc.extensions.jsontranscoder.web;

import static com.freemanan.starter.grpc.extensions.jsontranscoder.ServletTranscodingRouterFunction.snakeToPascal;
import static org.assertj.core.api.Assertions.assertThat;

import com.freemanan.starter.grpc.extensions.jsontranscoder.ServletTranscodingRouterFunction;
import org.junit.jupiter.api.Test;

/**
 * {@link ServletTranscodingRouterFunction} tester.
 */
class ServletTranscodingRouterFunctionTest {

    @Test
    void testSnakeToPascal() {
        assertThat(snakeToPascal("snake_case")).isEqualTo("SnakeCase");
        assertThat(snakeToPascal("s_s")).isEqualTo("SS");
        assertThat(snakeToPascal("s")).isEqualTo("S");
        assertThat(snakeToPascal("Ss")).isEqualTo("Ss");
        assertThat(snakeToPascal("SS")).isEqualTo("SS");
        assertThat(snakeToPascal("Ss_ss")).isEqualTo("SsSs");
        assertThat(snakeToPascal("SsSs")).isEqualTo("SsSs");
        assertThat(snakeToPascal("ssSs")).isEqualTo("SsSs");
    }
}