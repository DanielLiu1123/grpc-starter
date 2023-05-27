package com.freemanan.starter.grpc.extensions.transcoderjson;

import java.io.IOException;
import java.net.ServerSocket;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * @author Freeman
 */
public class U {

    /**
     * @return a random port
     */
    public static int randomPort() {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            return serverSocket.getLocalPort();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to find available port", e);
        }
    }

    public static WebTestClient webclient(int port) {
        return WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    }

    public static TestRestTemplate restTemplate() {
        return new TestRestTemplate();
    }
}
