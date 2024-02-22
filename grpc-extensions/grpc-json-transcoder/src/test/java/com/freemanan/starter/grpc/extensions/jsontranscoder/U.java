package com.freemanan.starter.grpc.extensions.jsontranscoder;

import java.net.ServerSocket;
import lombok.SneakyThrows;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * @author Freeman
 */
public class U {

    /**
     * @return a random port
     */
    @SneakyThrows
    public static int randomPort() {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            return serverSocket.getLocalPort();
        }
    }

    public static WebTestClient webclient(int port) {
        return WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    }

    public static TestRestTemplate restTemplate() {
        return new TestRestTemplate();
    }
}
