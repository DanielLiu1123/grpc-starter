package com.freemanan.starter.grpc.extensions.test;

import java.io.IOException;
import java.net.ServerSocket;
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
    public static int getRandomPort() {
        try (ServerSocket ss = new ServerSocket(0)) {
            return ss.getLocalPort();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
