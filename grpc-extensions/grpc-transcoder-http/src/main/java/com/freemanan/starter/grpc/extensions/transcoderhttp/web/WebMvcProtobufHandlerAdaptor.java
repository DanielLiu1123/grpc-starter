package com.freemanan.starter.grpc.extensions.transcoderhttp.web;

import static com.freemanan.starter.grpc.extensions.transcoderhttp.util.Util.isGrpcHandleMethod;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import com.freemanan.starter.grpc.extensions.transcoderhttp.AbstractHandlerAdaptor;
import com.freemanan.starter.grpc.extensions.transcoderhttp.processor.HeaderTransformProcessor;
import com.google.protobuf.Message;
import io.grpc.Metadata;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.MetadataUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.protobuf.ProtobufJsonFormatHttpMessageConverter;
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

    private static final HttpMessageConverter<Message> converter = new ProtobufJsonFormatHttpMessageConverter();

    private final HeaderTransformProcessor headerTransformProcessor;

    public WebMvcProtobufHandlerAdaptor(HeaderTransformProcessor headerTransformProcessor) {
        this.headerTransformProcessor = headerTransformProcessor;
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
        Metadata metadata = buildMetadata(new ServletServerHttpRequest(request).getHeaders());

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
        Object grpcResponse;
        try {
            grpcResponse = stubMethod.invoke(stub, message);
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
            HttpHeaders headers = headerTransformProcessor.toResponseHeader(responseMetadata);
            headers.forEach((k, values) -> values.forEach(v -> response.addHeader(k, v)));
        }

        // convert gRPC response message (Protobuf) to JSON
        try (ServletServerHttpResponse outputMessage = new ServletServerHttpResponse(response)) {
            converter.write((Message) grpcResponse, APPLICATION_JSON, outputMessage);
        }

        return null;
    }

    private Metadata buildMetadata(HttpHeaders headers) {
        return headerTransformProcessor.toRequestMetadata(headers);
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
