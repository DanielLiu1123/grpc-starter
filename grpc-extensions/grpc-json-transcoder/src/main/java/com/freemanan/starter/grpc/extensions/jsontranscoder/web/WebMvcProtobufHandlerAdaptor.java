package com.freemanan.starter.grpc.extensions.jsontranscoder.web;

import static com.freemanan.starter.grpc.extensions.jsontranscoder.util.JsonTranscoderUtil.anyCompatible;
import static com.freemanan.starter.grpc.extensions.jsontranscoder.util.JsonTranscoderUtil.getAccept;
import static com.freemanan.starter.grpc.extensions.jsontranscoder.util.JsonTranscoderUtil.isGrpcHandleMethod;
import static com.freemanan.starter.grpc.extensions.jsontranscoder.util.JsonTranscoderUtil.isJson;
import static com.freemanan.starter.grpc.extensions.jsontranscoder.util.JsonTranscoderUtil.notAcceptableException;
import static com.freemanan.starter.grpc.extensions.jsontranscoder.util.ProtoUtil.toJson;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import com.freemanan.starter.grpc.extensions.jsontranscoder.AbstractHandlerAdaptor;
import com.freemanan.starter.grpc.extensions.jsontranscoder.GrpcHeaderConverter;
import com.google.protobuf.Message;
import io.grpc.Metadata;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.MetadataUtils;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerAdapter;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.method.AbstractHandlerMethodAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

/**
 * @author Freeman
 */
public class WebMvcProtobufHandlerAdaptor extends AbstractHandlerAdaptor implements HandlerAdapter {

    private static final String NEW_BLOCKING_STUB = "newBlockingStub";

    private final GrpcHeaderConverter grpcHeaderConverter;

    public WebMvcProtobufHandlerAdaptor(GrpcHeaderConverter grpcHeaderConverter) {
        this.grpcHeaderConverter = grpcHeaderConverter;
    }

    @Override
    public boolean supports(Object handler) {
        return isGrpcHandleMethod(handler);
    }

    @Override
    public ModelAndView handle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        HandlerMethod hm = (HandlerMethod) handler;
        Method method = hm.getMethod();
        Class<?> beanClass = hm.getBeanType();

        // create message
        Message message = convert2ProtobufMessage(method.getParameterTypes()[0], request.getInputStream());

        // create metadata
        ServletServerHttpRequest req = new ServletServerHttpRequest(request);
        Metadata metadata = grpcHeaderConverter.toRequestMetadata(req.getHeaders());

        // get gRPC blocking stub to use
        Object stub = getStub(beanClass);

        // apply metadata to stub
        stub = applyInterceptor4Stub(MetadataUtils.newAttachHeadersInterceptor(metadata), stub);

        // capture gRPC response header/trailer
        AtomicReference<Metadata> responseHeader = new AtomicReference<>();
        AtomicReference<Metadata> responseTrailer = new AtomicReference<>();
        stub = applyInterceptor4Stub(
                MetadataUtils.newCaptureMetadataInterceptor(responseHeader, responseTrailer), stub);

        // find gRPC stub method to call
        Method stubMethod = getInvokeMethod(stub, method, message);

        // call gRPC stub method
        Message grpcResponse;
        try {
            grpcResponse = (Message) stubMethod.invoke(stub, message);
        } catch (InvocationTargetException ite) {
            Throwable te = ite.getTargetException();
            if (te instanceof StatusRuntimeException) {
                throw (StatusRuntimeException) te;
            }
            throw ite;
        }

        // convert gRPC response header to HTTP header
        Metadata responseMetadata = responseHeader.get();
        if (responseMetadata != null) {
            HttpHeaders headers = grpcHeaderConverter.toResponseHeader(responseMetadata);
            headers.forEach((k, values) -> values.forEach(v -> response.addHeader(k, v)));
        }

        try (ServletServerHttpResponse resp = new ServletServerHttpResponse(response)) {
            // convert gRPC response message (Protobuf) to JSON
            String json = toJson(grpcResponse);
            if (isJson(json)) {
                if (anyCompatible(getAccept(req.getHeaders()), APPLICATION_JSON)) {
                    resp.getHeaders().setContentType(APPLICATION_JSON);
                    resp.getBody().write(json.getBytes(StandardCharsets.UTF_8));
                    resp.getBody().flush();
                    return null;
                }
                throw notAcceptableException();
            }

            MediaType mt = new MediaType(MediaType.TEXT_PLAIN, StandardCharsets.UTF_8);
            if (anyCompatible(getAccept(req.getHeaders()), mt)) {
                resp.getHeaders().setContentType(mt);
                resp.getBody().write(json.getBytes(StandardCharsets.UTF_8));
                resp.getBody().flush();
                return null;
            }
            throw notAcceptableException();
        }
    }

    /**
     * @see RequestMappingHandlerAdapter#getLastModifiedInternal
     */
    @Override
    @Deprecated
    public long getLastModified(HttpServletRequest request, Object handler) {
        return -1;
    }

    /**
     * Must before {@link RequestMappingHandlerAdapter}
     *
     * @see AbstractHandlerMethodAdapter#getOrder()
     */
    @Override
    public int getOrder() {
        return ORDER;
    }

    @Override
    public String getNewStubMethodName() {
        return NEW_BLOCKING_STUB;
    }
}
