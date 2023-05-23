package com.freemanan.starter.grpc.extensions.transcoderhttp.webflux;

import com.google.protobuf.Message;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import lombok.EqualsAndHashCode;
import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.http.HttpStatusCode;
import org.springframework.lang.Nullable;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.reactive.result.method.InvocableHandlerMethod;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * @author Freeman
 */
@EqualsAndHashCode(callSuper = true)
public class ProtobufHandlerMethod extends InvocableHandlerMethod {
    private final ReactiveAdapterRegistry reactiveAdapterRegistry = ReactiveAdapterRegistry.getSharedInstance();

    private final Message returnValue;

    public ProtobufHandlerMethod(HandlerMethod handlerMethod, Message returnValue) {
        super(handlerMethod);
        this.returnValue = returnValue;
    }

    @Override
    @SuppressWarnings({"unchecked"})
    public Mono<HandlerResult> invoke(
            ServerWebExchange exchange, BindingContext bindingContext, Object... providedArgs) {
        HttpStatusCode status = getResponseStatus();
        if (status != null) {
            exchange.getResponse().setStatusCode(status);
        }

        MethodParameter returnType = getReturnType();
        if (isResponseHandled(exchange)) {
            Class<?> parameterType = returnType.getParameterType();
            ReactiveAdapter adapter = reactiveAdapterRegistry.getAdapter(parameterType);
            boolean asyncVoid = isAsyncVoidReturnType(returnType, adapter);
            if (returnValue == null || asyncVoid) {
                return (asyncVoid ? Mono.from(adapter.toPublisher(returnValue)) : Mono.empty());
            }
            if (parameterType == void.class) {
                return (Mono<HandlerResult>) returnValue;
            }
        }

        HandlerResult result = new HandlerResult(this, returnValue, returnType, bindingContext);
        return Mono.just(result);
    }

    private static boolean isAsyncVoidReturnType(MethodParameter returnType, @Nullable ReactiveAdapter adapter) {
        if (adapter != null && adapter.supportsEmpty()) {
            if (adapter.isNoValue()) {
                return true;
            }
            Type parameterType = returnType.getGenericParameterType();
            if (parameterType instanceof ParameterizedType type && (type.getActualTypeArguments().length == 1)) {
                return Void.class.equals(type.getActualTypeArguments()[0]);
            }
        }
        return false;
    }

    private boolean isResponseHandled(ServerWebExchange exchange) {
        return getResponseStatus() != null || exchange.isNotModified();
    }
}
