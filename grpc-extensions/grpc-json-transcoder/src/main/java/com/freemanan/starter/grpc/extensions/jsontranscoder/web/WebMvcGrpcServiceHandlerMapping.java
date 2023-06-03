package com.freemanan.starter.grpc.extensions.jsontranscoder.web;

import com.freemanan.starter.grpc.extensions.jsontranscoder.util.JsonTranscoderUtil;
import io.grpc.BindableService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import org.springframework.web.servlet.handler.AbstractHandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Provide default routes for those gRPC services without custom http routes.
 *
 * <p> The default route is: POST /xx.v1.ServiceName/MethodName
 *
 * @author Freeman
 */
public class WebMvcGrpcServiceHandlerMapping extends AbstractHandlerMapping {
    public static final int ORDER = 10;

    /**
     * path -> service
     *
     * <p> path: /xx.v1.ServiceName/MethodName
     */
    private final Map<String, HandlerMethod> pathToMethod;

    public WebMvcGrpcServiceHandlerMapping(ObjectProvider<BindableService> grpcServiceProvider) {
        this.pathToMethod = JsonTranscoderUtil.getPathToMethod(grpcServiceProvider);
    }

    @Override
    protected HandlerMethod getHandlerInternal(HttpServletRequest request) {
        if (!HttpMethod.POST.name().equalsIgnoreCase(request.getMethod())) {
            return null;
        }
        for (Map.Entry<String, HandlerMethod> entry : pathToMethod.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(request.getRequestURI())) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * Must after {@link RequestMappingHandlerMapping}, it will process {@link RequestMapping} for us for free!
     *
     * @see WebMvcConfigurationSupport#requestMappingHandlerMapping
     */
    @Override
    public int getOrder() {
        return ORDER;
    }
}
