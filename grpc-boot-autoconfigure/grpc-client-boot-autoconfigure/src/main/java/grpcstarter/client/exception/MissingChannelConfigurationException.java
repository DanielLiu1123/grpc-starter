package grpcstarter.client.exception;

/**
 * gRPC channel authority not configured exception.
 *
 * @author Freeman
 */
public class MissingChannelConfigurationException extends RuntimeException {

    private final Class<?> stubClass;

    public MissingChannelConfigurationException(Class<?> stubClass) {
        super("gRPC channel authority is not configured for stub: " + stubClass.getCanonicalName());
        this.stubClass = stubClass;
    }

    public Class<?> getStubClass() {
        return stubClass;
    }
}
