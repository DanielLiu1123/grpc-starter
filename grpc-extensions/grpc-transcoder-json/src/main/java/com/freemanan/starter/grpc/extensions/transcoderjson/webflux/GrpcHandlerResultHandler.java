/*
 * Copyright 2002-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.freemanan.starter.grpc.extensions.transcoderjson.webflux;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.SneakyThrows;
import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.reactive.HandlerResultHandler;
import org.springframework.web.reactive.accept.RequestedContentTypeResolver;
import org.springframework.web.reactive.result.method.annotation.AbstractMessageWriterResultHandler;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * @author Freeman
 */
public class GrpcHandlerResultHandler extends AbstractMessageWriterResultHandler implements HandlerResultHandler {

    private static final JsonFormat.Printer printer = JsonFormat.printer().omittingInsignificantWhitespace();
    private static final ObjectMapper om =
            new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private static final MethodParameter returnType;

    static {
        Method method;
        try {
            method = GrpcHandlerResultHandler.class.getDeclaredMethod("map");
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
        returnType = new MethodParameter(method, -1);
    }

    /**
     * Constructor with an {@link ReactiveAdapterRegistry} instance.
     *
     * @param writers  the writers for serializing to the response body
     * @param resolver to determine the requested content type
     * @param registry for adaptation to reactive types
     */
    public GrpcHandlerResultHandler(
            List<HttpMessageWriter<?>> writers,
            RequestedContentTypeResolver resolver,
            ReactiveAdapterRegistry registry) {

        super(writers, resolver, registry);
        // ResponseBodyResultHandler order is 100
        setOrder(0);
    }

    @Override
    public boolean supports(HandlerResult result) {
        MethodParameter retType = result.getReturnTypeSource();
        return retType.getParameterType() == Void.TYPE && result.getReturnValue() instanceof Message;
    }

    @Override
    @SneakyThrows
    public Mono<Void> handleResult(ServerWebExchange exchange, HandlerResult result) {
        Message message = (Message) result.getReturnValue();
        String json = printer.print(message);
        return writeBody(om.readValue(json, Object.class), returnType, exchange);
    }

    /**
     * Just a dummy method to get a {@link MethodParameter} instance.
     */
    protected static Map<String, Object> map() {
        return Collections.emptyMap();
    }
}
