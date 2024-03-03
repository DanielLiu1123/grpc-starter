package com.freemanan.starter.grpc.extensions.jsontranscoder.webflux;

import com.freemanan.starter.grpc.extensions.jsontranscoder.util.JsonTranscoderUtil;
import io.grpc.BindableService;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.reactive.config.WebFluxConfigurationSupport;
import org.springframework.web.reactive.handler.AbstractHandlerMapping;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Provide default routes for those gRPC services without custom http routes.
 *
 * <p> The default route is: POST /xx.v1.ServiceName/MethodName
 *
 * @author Freeman
 */
public class WebFluxGrpcServiceHandlerMapping extends AbstractHandlerMapping {

    public static final int ORDER = 10;

    /**
     * path -> service
     *
     * <p> path: /xx.v1.ServiceName/MethodName
     */
    private final Map<String, HandlerMethod> pathToMethod;

    public WebFluxGrpcServiceHandlerMapping(ObjectProvider<BindableService> grpcServiceProvider) {
        this.pathToMethod = JsonTranscoderUtil.getPathToMethod(grpcServiceProvider);
    }

    @Override
    protected Mono<?> getHandlerInternal(ServerWebExchange exchange) {
        HttpMethod method = exchange.getRequest().getMethod();
        if (method != HttpMethod.POST) {
            return Mono.empty();
        }
        String path = exchange.getRequest().getPath().toString();
        for (Map.Entry<String, HandlerMethod> entry : pathToMethod.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(path)) {
                return Mono.just(entry.getValue());
            }
        }
        return Mono.empty();
    }

    /**
     * Must after {@link RequestMappingHandlerMapping}, it will process {@link RequestMapping} for us for free!
     *
     * @see WebFluxConfigurationSupport#requestMappingHandlerMapping
     */
    @Override
    public int getOrder() {
        return ORDER;
    }
}
