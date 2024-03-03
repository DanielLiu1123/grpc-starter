package com.freemanan.starter.grpc.client.exception;

import lombok.Getter;

/**
 * @author Freeman
 */
@Getter
public class MissingChannelConfigurationException extends RuntimeException {

    private final Class<?> stubClass;

    public MissingChannelConfigurationException(Class<?> stubClass) {
        super("gRPC channel authority is not configured for stub: " + stubClass.getCanonicalName());
        this.stubClass = stubClass;
    }
}
