package com.freemanan.starter.grpc.extensions.jsontranscoder;

import com.freemanan.starter.grpc.server.GrpcServerProperties;
import com.freemanan.starter.grpc.server.GrpcServerShutdownEvent;
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
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/**
 * @author Freeman
 */
public abstract class AbstractHandlerAdaptor
        implements ApplicationListener<ApplicationEvent>, BeanFactoryAware, Ordered {
    private static final Logger log = LoggerFactory.getLogger(AbstractHandlerAdaptor.class);

    public static final int ORDER = 0;
    private static final String GET_DEFAULT_INSTANCE = "getDefaultInstance";
    private static final Method withInterceptorsMethod;
    private static final JsonFormat.Parser parser = JsonFormat.parser().ignoringUnknownFields();

    static {
        withInterceptorsMethod = getWithInterceptorsMethod();
    }

    private final Map<Class<?>, Object> beanClassToStub = new ConcurrentHashMap<>();
    /**
     * key: stubClassName#methodName
     */
    private final Map<String, Method> keyToMethod = new ConcurrentHashMap<>();

    private final Map<Class<?>, Message> messageClassToDefaultInstance = new ConcurrentHashMap<>();
    protected BeanFactory beanFactory;
    private ManagedChannel channel;

    private static Method doGetStubMethod(Object stubToUse, Method method, Message msg) {
        try {
            return stubToUse.getClass().getMethod(method.getName(), msg.getClass());
        } catch (NoSuchMethodException e) {
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

    private static Method getWithInterceptorsMethod() {
        try {
            return AbstractStub.class.getMethod("withInterceptors", ClientInterceptor[].class);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }

    @SneakyThrows
    protected Object applyInterceptor4Stub(ClientInterceptor clientInterceptor, Object stub) {
        return withInterceptorsMethod.invoke(stub, (Object) new ClientInterceptor[] {clientInterceptor});
    }

    protected Message convert2ProtobufMessage(Class<?> messageClass, InputStream is) {
        Message defaultInstance = messageClassToDefaultInstance.computeIfAbsent(messageClass, k -> {
            try {
                return ((Message) messageClass.getMethod(GET_DEFAULT_INSTANCE).invoke(null));
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                if (log.isWarnEnabled()) {
                    log.warn("Failed to invoke method '{}' of class {}", GET_DEFAULT_INSTANCE, messageClass, e);
                }
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
            }
        });

        Message.Builder builder = defaultInstance.toBuilder();
        try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
            parser.merge(reader, builder);
            return builder.build();
        } catch (IOException e) {
            if (log.isWarnEnabled()) {
                log.warn("Failed to parse JSON to Message {}", messageClass, e);
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
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

    protected Method getInvokeMethod(Object stubToUse, Method method, Message msg) {
        String key = stubToUse.getClass().getCanonicalName() + "#" + method.getName();
        return keyToMethod.computeIfAbsent(key, k -> doGetStubMethod(stubToUse, method, msg));
    }

    protected Object getStub(Class<?> beanClass) {
        return beanClassToStub.computeIfAbsent(beanClass, this::doGetStub);
    }

    public abstract String getNewStubMethodName();

    @Override
    public int getOrder() {
        return ORDER;
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof GrpcServerStartedEvent) {
            onGrpcServerStartedEvent((GrpcServerStartedEvent) event);
        }
        if (event instanceof GrpcServerShutdownEvent && channel != null) {
            channel.shutdown();
        }
    }

    private void onGrpcServerStartedEvent(GrpcServerStartedEvent event) {
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

        setChannel(builder);
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    /**
     * Give a chance to customize the channel.
     *
     * @param builder ManagedChannelBuilder
     */
    protected void setChannel(ManagedChannelBuilder<?> builder) {
        this.channel = builder.build();
    }
}
