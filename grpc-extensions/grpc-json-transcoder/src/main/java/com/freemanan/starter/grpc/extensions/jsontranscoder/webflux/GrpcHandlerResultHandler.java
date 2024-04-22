package com.freemanan.starter.grpc.extensions.jsontranscoder.webflux;

import static com.freemanan.starter.grpc.extensions.jsontranscoder.JsonTranscoderUtil.isJson;
import static com.freemanan.starter.grpc.extensions.jsontranscoder.ProtoUtil.toJson;

import com.freemanan.starter.grpc.extensions.jsontranscoder.JsonTranscoderUtil;
import com.google.protobuf.Message;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Supplier;
import org.springframework.core.MethodParameter;
import org.springframework.core.Ordered;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.reactive.HandlerResultHandler;
import org.springframework.web.reactive.result.method.annotation.ResponseBodyResultHandler;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * @author Freeman
 */
public class GrpcHandlerResultHandler implements HandlerResultHandler, Ordered {

    public static final int ORDER = 0;

    @Override
    public boolean supports(HandlerResult result) {
        MethodParameter retType = result.getReturnTypeSource();
        return retType.getParameterType() == Void.TYPE && result.getReturnValue() instanceof Message;
    }

    @Override
    public Mono<Void> handleResult(ServerWebExchange exchange, HandlerResult result) {
        Message message = (Message) result.getReturnValue();
        Supplier<Throwable> errorSupplier = JsonTranscoderUtil::notAcceptableException;
        String json = toJson(message);
        if (isJson(json)) {
            if (anyCompatible(exchange, MediaType.APPLICATION_JSON)) {
                exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
                return exchange.getResponse()
                        .writeWith(Mono.just(
                                exchange.getResponse().bufferFactory().wrap(json.getBytes(StandardCharsets.UTF_8))));
            }
            return Mono.error(errorSupplier);
        }
        // simple value using content-type text/plain;charset=UTF-8
        MediaType mt = new MediaType(MediaType.TEXT_PLAIN, StandardCharsets.UTF_8);
        if (anyCompatible(exchange, mt)) {
            exchange.getResponse().getHeaders().setContentType(mt);
            return exchange.getResponse()
                    .writeWith(Mono.just(
                            exchange.getResponse().bufferFactory().wrap(json.getBytes(StandardCharsets.UTF_8))));
        }
        return Mono.error(errorSupplier);
    }

    private static boolean anyCompatible(ServerWebExchange exchange, MediaType otherMediaType) {
        List<MediaType> mediaTypes =
                JsonTranscoderUtil.getAccept(exchange.getRequest().getHeaders());
        for (MediaType mediaType : mediaTypes) {
            if (mediaType.isCompatibleWith(otherMediaType)) {
                return true;
            }
        }
        return false;
    }

    /**
     * {@link ResponseBodyResultHandler} order is 100.
     *
     * @see ResponseBodyResultHandler
     */
    @Override
    public int getOrder() {
        return ORDER;
    }
}
