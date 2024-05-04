package grpcstarter.extensions.transcoding;

import java.net.ServerSocket;
import lombok.SneakyThrows;
import org.springframework.boot.test.web.client.TestRestTemplate;

/**
 * @author Freeman
 */
class TestUtil {

    /**
     * @return a random port
     */
    @SneakyThrows
    public static int randomPort() {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            return serverSocket.getLocalPort();
        }
    }

    public static TestRestTemplate restTemplate() {
        return new TestRestTemplate();
    }
}
