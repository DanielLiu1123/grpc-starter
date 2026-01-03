package grpcstarter.extensions.test;

import io.grpc.Channel;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.stub.AbstractStub;
import java.lang.reflect.Method;
import java.util.Objects;
import org.springframework.util.ReflectionUtils;

/**
 * Utility class for creating gRPC stubs.
 *
 * @author Freeman
 */
public final class StubUtil {

    private StubUtil() {}

    private static final String NEW_BLOCKING_STUB_METHOD = "newBlockingStub";
    private static final String NEW_BLOCKING_V2_STUB_METHOD = "newBlockingV2Stub";
    private static final String NEW_FUTURE_STUB_METHOD = "newFutureStub";
    private static final String NEW_STUB_METHOD = "newStub";
    private static final String BLOCKING_STUB = "BlockingStub";
    private static final String BLOCKING_V2_STUB = "BlockingV2Stub";
    private static final String FUTURE_STUB = "FutureStub";

    /**
     * Create a gRPC stub instance using in-process channel.
     *
     * @param inProcessName in-process server name
     * @param stubClass     stub class
     * @param <T>           stub type
     * @return gRPC stub instance
     */
    public static <T extends AbstractStub<T>> T createStub(String inProcessName, Class<T> stubClass) {
        Class<?> grpcClass = Objects.requireNonNull(stubClass.getEnclosingClass());
        Method stubMethod = Objects.requireNonNull(
                ReflectionUtils.findMethod(grpcClass, getStubMethodName(stubClass), Channel.class));
        Object stub = Objects.requireNonNull(ReflectionUtils.invokeMethod(
                stubMethod, null, InProcessChannelBuilder.forName(inProcessName).build()));
        return stubClass.cast(stub);
    }

    private static String getStubMethodName(Class<?> stubClass) {
        if (stubClass.getName().endsWith(BLOCKING_STUB)) {
            return NEW_BLOCKING_STUB_METHOD;
        }
        if (stubClass.getName().endsWith(BLOCKING_V2_STUB)) {
            return NEW_BLOCKING_V2_STUB_METHOD;
        }
        if (stubClass.getName().endsWith(FUTURE_STUB)) {
            return NEW_FUTURE_STUB_METHOD;
        }
        return NEW_STUB_METHOD;
    }
}
