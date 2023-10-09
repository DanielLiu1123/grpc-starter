package com.freemanan.starter.grpc.extensions.test;

import io.grpc.Channel;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.stub.AbstractStub;
import java.lang.reflect.Method;
import lombok.experimental.UtilityClass;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * @author Freeman
 */
@UtilityClass
public class ChannelUtil {

    private static final String NEW_BLOCKING_STUB_METHOD = "newBlockingStub";
    private static final String NEW_FUTURE_STUB_METHOD = "newFutureStub";
    private static final String NEW_STUB_METHOD = "newStub";
    private static final String BLOCKING_STUB = "BlockingStub";
    private static final String FUTURE_STUB = "FutureStub";

    @SuppressWarnings("unchecked")
    public static <T extends AbstractStub<T>> T stub(String inProcessName, Class<T> stubClass) {
        Class<?> grpcClass = stubClass.getEnclosingClass();
        Assert.notNull(grpcClass, "grpcClass must not be null");
        Method stubMethod = ReflectionUtils.findMethod(grpcClass, getStubMethodName(stubClass), Channel.class);
        Assert.notNull(stubMethod, "stubMethod must not be null");
        Object stub = ReflectionUtils.invokeMethod(
                stubMethod, null, InProcessChannelBuilder.forName(inProcessName).build());
        Assert.notNull(stub, "stub must not be null");
        return (T) stub;
    }

    private static String getStubMethodName(Class<?> stubClass) {
        if (stubClass.getName().endsWith(BLOCKING_STUB)) {
            return NEW_BLOCKING_STUB_METHOD;
        } else if (stubClass.getName().endsWith(FUTURE_STUB)) {
            return NEW_FUTURE_STUB_METHOD;
        } else {
            return NEW_STUB_METHOD;
        }
    }
}
