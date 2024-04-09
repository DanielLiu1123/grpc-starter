package com.freeman.example;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;

/**
 * @author Freeman
 * @since 2024/4/9
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
class TranscodingIT {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate rest;

    @Test
    void testTranscoding() {
        ResponseEntity<String> response =
                rest.postForEntity("http://localhost:" + port + "/v1/foo", new Pet("Foo"), String.class);
        System.out.println(response);
    }

    record Pet(String name) {}
}
