package grpcstarter.example;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.test.LocalServerPort;
import org.springframework.boot.web.server.test.client.TestRestTemplate;
import org.springframework.http.HttpStatus;

/**
 * @author Freeman
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
class TranscodingFluxIT {

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate rest;

    @Test
    void testTranscoding() {
        var response =
                rest.postForEntity("http://localhost:" + port + "/unary", new SimpleRequest("foo"), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType()).hasToString("application/json");
        assertThat(response.getBody()).isEqualTo("""
                {"responseMessage":"Hello foo"}""");
    }

    record SimpleRequest(String requestMessage) {}
}
