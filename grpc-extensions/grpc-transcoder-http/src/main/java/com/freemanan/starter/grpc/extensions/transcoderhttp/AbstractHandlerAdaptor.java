package com.freemanan.starter.grpc.extensions.transcoderhttp;

import com.freemanan.starter.grpc.server.GrpcServerProperties;
import com.freemanan.starter.grpc.server.GrpcServerStartedEvent;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import io.grpc.BindableService;
import io.grpc.Channel;
import io.grpc.ClientInterceptor;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.AbstractStub;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * @author Freeman
 */
public abstract class AbstractHandlerAdaptor
        implements ApplicationListener<GrpcServerStartedEvent>, BeanFactoryAware, Ordered {

    public static final int ORDER = 0;

    private ManagedChannel channel;

    private BeanFactory beanFactory;

    private final Map<Class<?>, Object> beanClassToStub = new ConcurrentHashMap<>();
    /**
     * key: stubClassName#methodName
     */
    private final Map<String, Method> keyToMethod = new ConcurrentHashMap<>();

    private final Map<Class<?>, Message> messageClassToDefaultInstance = new ConcurrentHashMap<>();

    protected static final Method withInterceptorsMethod;
    private static final JsonFormat.Parser parser = JsonFormat.parser().ignoringUnknownFields();

    static {
        withInterceptorsMethod = getWithInterceptorsMethod();
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Override
    public void onApplicationEvent(GrpcServerStartedEvent event) {
        GrpcServerProperties properties = beanFactory.getBean(GrpcServerProperties.class);
        boolean usingInProcess = properties.getInProcess() != null
                && StringUtils.hasText(properties.getInProcess().getName());

        ManagedChannelBuilder<?> builder = usingInProcess
                ? ManagedChannelBuilder.forTarget(properties.getInProcess().getName())
                : ManagedChannelBuilder.forAddress(
                        "127.0.0.1", event.getSource().getPort());

        builder.maxInboundMessageSize((int) properties.getMaxMessageSize().toBytes());
        builder.maxInboundMetadataSize((int) properties.getMaxMetadataSize().toBytes());

        builder.usePlaintext();

        this.channel = builder.build();
    }

    private static Method getWithInterceptorsMethod() {
        try {
            return AbstractStub.class.getMethod("withInterceptors", ClientInterceptor[].class);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }

    protected Object getStub(Class<?> beanClass) {
        return beanClassToStub.computeIfAbsent(beanClass, this::doGetStub);
    }

    protected Object doGetStub(Class<?> beanClass) {
        Class<?> grpcClass = getImplBeanClass(beanClass).getEnclosingClass();
        try {
            return ReflectionUtils.invokeMethod(
                    grpcClass.getMethod(getNewStubMethodName(), Channel.class), null, channel);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }

    protected Object applyInterceptor4Stub(ClientInterceptor clientInterceptor, Object stub) {
        try {
            return withInterceptorsMethod.invoke(stub, new Object[] {new ClientInterceptor[] {clientInterceptor}});
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Class<?> getImplBeanClass(Class<?> beanClass) {
        Class<?> superclass = beanClass.getSuperclass();
        if (Arrays.stream(superclass.getInterfaces()).anyMatch(clz -> clz == BindableService.class)) {
            return superclass;
        }
        return getImplBeanClass(superclass);
    }

    protected Method getInvokeMethod(Object stubToUse, Method method, Message msg) {
        String key = stubToUse.getClass().getCanonicalName() + "#" + method.getName();
        return keyToMethod.computeIfAbsent(key, k -> doGetStubMethod(stubToUse, method, msg));
    }

    private static Method doGetStubMethod(Object stubToUse, Method method, Message msg) {
        try {
            return stubToUse.getClass().getMethod(method.getName(), msg.getClass());
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }

    protected Message convert2ProtobufMessage(Class<?> messageClass, InputStream is) {
        Message defaultInstance = messageClassToDefaultInstance.computeIfAbsent(messageClass, k -> {
            try {
                return ((Message) messageClass.getMethod("getDefaultInstance").invoke(null));
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                throw new IllegalStateException(e);
            }
        });
        Message.Builder builder = defaultInstance.toBuilder();
        try {
            InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8);
            parser.merge(reader, builder);
            return builder.build();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public int getOrder() {
        return ORDER;
    }

    public abstract String getNewStubMethodName();
}
