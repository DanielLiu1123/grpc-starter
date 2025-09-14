package grpcstarter.example;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.test.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * @author Freeman
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
class TranscodingMvcIT {

    @LocalServerPort
    int port;

    final RestTestClient client = RestTestClient.bindToServer().build();

    @Test
    void testTranscoding() {
        var resp = client.post()
                .uri("http://localhost:" + port + "/unary")
                .body(new SimpleRequest("foo"))
                .exchange();

        resp.expectStatus().isEqualTo(HttpStatus.OK);
        resp.expectHeader().contentType(MediaType.APPLICATION_JSON);
        resp.expectBody(String.class).isEqualTo("""
                {"responseMessage":"Hello foo"}""");
    }

    record SimpleRequest(String requestMessage) {}
}
