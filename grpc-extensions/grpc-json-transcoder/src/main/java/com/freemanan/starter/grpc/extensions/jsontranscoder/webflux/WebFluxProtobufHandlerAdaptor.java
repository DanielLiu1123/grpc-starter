package com.freemanan.starter.grpc.extensions.jsontranscoder.webflux;

import static com.freemanan.starter.grpc.extensions.jsontranscoder.util.JsonTranscoderUtil.isGrpcHandleMethod;

import com.freemanan.starter.grpc.extensions.jsontranscoder.AbstractHandlerAdaptor;
import com.freemanan.starter.grpc.extensions.jsontranscoder.FutureAdapter;
import com.freemanan.starter.grpc.extensions.jsontranscoder.GrpcHeaderConverter;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.Message;
import io.grpc.Metadata;
import io.grpc.stub.MetadataUtils;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.lang.Nullable;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.HandlerAdapter;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.reactive.result.method.InvocableHandlerMethod;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * @author Freeman
 */
public class WebFluxProtobufHandlerAdaptor extends AbstractHandlerAdaptor implements HandlerAdapter {
    private static final Logger log = LoggerFactory.getLogger(WebFluxProtobufHandlerAdaptor.class);

    private static final String NEW_FUTURE_STUB = "newFutureStub";

    private final ControllerMethodResolver resolver;
    private final GrpcHeaderConverter grpcHeaderConverter;

    public WebFluxProtobufHandlerAdaptor(
            ReactiveAdapterRegistry adapterRegistry,
            ConfigurableApplicationContext context,
            List<HttpMessageReader<?>> readers,
            GrpcHeaderConverter grpcHeaderConverter) {
        this.resolver = new ControllerMethodResolver(adapterRegistry, context, readers);
        this.grpcHeaderConverter = grpcHeaderConverter;
    }

    @SuppressWarnings("unchecked")
    private static ListenableFuture<Message> getFutureStubResponse(Object futureStub, Message msg, Method method) {
        return (ListenableFuture<Message>) ReflectionUtils.invokeMethod(method, futureStub, msg);
    }

    @Override
    public boolean supports(Object handler) {
        return isGrpcHandleMethod(handler);
    }

    @Override
    public Mono<HandlerResult> handle(ServerWebExchange exchange, Object handler) {
        HandlerMethod hm = ((HandlerMethod) handler);
        Method method = hm.getMethod();
        Class<?> messageType = method.getParameterTypes()[0];
        Class<?> beanClass = hm.getBeanType();

        Function<Throwable, Mono<HandlerResult>> exceptionHandler =
                ex -> handleException(exchange, ex, hm, new BindingContext());

        AtomicReference<Metadata> responseHeader = new AtomicReference<>();
        AtomicReference<Metadata> responseTrailer = new AtomicReference<>();

        return DataBufferUtils.join(exchange.getRequest().getBody())
                .map(dataBuffer -> convert2ProtobufMessage(messageType, dataBuffer.asInputStream()))
                // invoke grpc method
                .flatMap(msg -> {
                    Object stub = getStub(beanClass);

                    HttpHeaders headers = exchange.getRequest().getHeaders();
                    Metadata metadata = grpcHeaderConverter.toRequestMetadata(headers);

                    // apply metadata to stub
                    stub = applyInterceptor4Stub(MetadataUtils.newAttachHeadersInterceptor(metadata), stub);

                    // capture gRPC response header/trailer
                    stub = applyInterceptor4Stub(
                            MetadataUtils.newCaptureMetadataInterceptor(responseHeader, responseTrailer), stub);

                    Method m = getInvokeMethod(stub, method, msg);
                    ListenableFuture<Message> resp = getFutureStubResponse(stub, msg, m);
                    return Mono.fromFuture(FutureAdapter.toCompletable(resp));
                })
                .doOnNext(msg -> {
                    // convert gRPC response header to HTTP header
                    Metadata responseMetadata = responseHeader.get();
                    if (responseMetadata != null) {
                        HttpHeaders headers = grpcHeaderConverter.toResponseHeader(responseMetadata);
                        headers.forEach((k, values) -> values.forEach(
                                v -> exchange.getResponse().getHeaders().add(k, v)));
                    }
                })
                .map(message -> new HandlerResult(hm, message, hm.getReturnType()))
                .doOnNext(handlerResult -> handlerResult.setExceptionHandler(exceptionHandler))
                .onErrorResume(exceptionHandler);
    }

    private Mono<HandlerResult> handleException(
            ServerWebExchange exchange,
            Throwable exception,
            @Nullable HandlerMethod handlerMethod,
            @Nullable BindingContext bindingContext) {

        // Success and error responses may use different content types
        exchange.getAttributes().remove(HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE);
        exchange.getResponse().getHeaders().clearContentHeaders();

        InvocableHandlerMethod invocable = this.resolver.getExceptionHandlerMethod(exception, handlerMethod);

        if (invocable != null) {
            ArrayList<Throwable> exceptions = new ArrayList<>();
            try {
                if (log.isDebugEnabled()) {
                    log.debug(exchange.getLogPrefix() + "Using @ExceptionHandler " + invocable);
                }
                if (bindingContext != null) {
                    bindingContext.getModel().asMap().clear();
                } else {
                    bindingContext = new BindingContext();
                }

                // Expose causes as provided arguments as well
                Throwable exToExpose = exception;
                while (exToExpose != null) {
                    exceptions.add(exToExpose);
                    Throwable cause = exToExpose.getCause();
                    exToExpose = (cause != exToExpose ? cause : null);
                }
                Object[] arguments = new Object[exceptions.size() + 1];
                exceptions.toArray(arguments); // efficient arraycopy call in ArrayList
                arguments[arguments.length - 1] = handlerMethod;

                return invocable.invoke(exchange, bindingContext, arguments);
            } catch (Throwable invocationEx) {
                // Any other than the original exception (or a cause) is unintended here,
                // probably an accident (e.g. failed assertion or the like).
                if (!exceptions.contains(invocationEx) && log.isWarnEnabled()) {
                    log.warn(exchange.getLogPrefix() + "Failure in @ExceptionHandler " + invocable, invocationEx);
                }
            }
        }
        return Mono.error(exception);
    }

    /**
     * Must before {@link RequestMappingHandlerAdapter}!
     *
     * @see RequestMappingHandlerAdapter
     */
    @Override
    public int getOrder() {
        return ORDER;
    }

    @Override
    public String getNewStubMethodName() {
        return NEW_FUTURE_STUB;
    }
}
