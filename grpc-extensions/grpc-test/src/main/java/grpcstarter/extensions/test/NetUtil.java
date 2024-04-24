package grpcstarter.extensions.test;

import java.net.ServerSocket;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

/**
 * @author Freeman
 */
@UtilityClass
public class NetUtil {

    /**
     * Get a random port.
     *
     * @return port
     */
    @SneakyThrows
    public static int getRandomPort() {
        try (ServerSocket ss = new ServerSocket(0)) {
            return ss.getLocalPort();
        }
    }
}
