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

package com.freemanan.starter.grpc.extensions.jsontranscoder.webflux;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.KotlinDetector;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.method.ControllerAdviceBean;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.annotation.ExceptionHandlerMethodResolver;
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolver;
import org.springframework.web.reactive.result.method.InvocableHandlerMethod;
import org.springframework.web.reactive.result.method.annotation.ContinuationHandlerMethodArgumentResolver;
import org.springframework.web.reactive.result.method.annotation.CookieValueMethodArgumentResolver;
import org.springframework.web.reactive.result.method.annotation.ErrorsMethodArgumentResolver;
import org.springframework.web.reactive.result.method.annotation.ExpressionValueMethodArgumentResolver;
import org.springframework.web.reactive.result.method.annotation.HttpEntityMethodArgumentResolver;
import org.springframework.web.reactive.result.method.annotation.MatrixVariableMapMethodArgumentResolver;
import org.springframework.web.reactive.result.method.annotation.MatrixVariableMethodArgumentResolver;
import org.springframework.web.reactive.result.method.annotation.ModelAttributeMethodArgumentResolver;
import org.springframework.web.reactive.result.method.annotation.ModelMethodArgumentResolver;
import org.springframework.web.reactive.result.method.annotation.PathVariableMapMethodArgumentResolver;
import org.springframework.web.reactive.result.method.annotation.PathVariableMethodArgumentResolver;
import org.springframework.web.reactive.result.method.annotation.PrincipalMethodArgumentResolver;
import org.springframework.web.reactive.result.method.annotation.RequestAttributeMethodArgumentResolver;
import org.springframework.web.reactive.result.method.annotation.RequestBodyMethodArgumentResolver;
import org.springframework.web.reactive.result.method.annotation.RequestHeaderMapMethodArgumentResolver;
import org.springframework.web.reactive.result.method.annotation.RequestHeaderMethodArgumentResolver;
import org.springframework.web.reactive.result.method.annotation.RequestParamMapMethodArgumentResolver;
import org.springframework.web.reactive.result.method.annotation.RequestParamMethodArgumentResolver;
import org.springframework.web.reactive.result.method.annotation.RequestPartMethodArgumentResolver;
import org.springframework.web.reactive.result.method.annotation.ServerWebExchangeMethodArgumentResolver;
import org.springframework.web.reactive.result.method.annotation.SessionAttributeMethodArgumentResolver;
import org.springframework.web.reactive.result.method.annotation.SessionStatusMethodArgumentResolver;
import org.springframework.web.reactive.result.method.annotation.WebSessionMethodArgumentResolver;

/**
 * Copy form {@link org.springframework.web.reactive.result.method.annotation.ControllerMethodResolver}.
 *
 * @author Freeman
 */
class ControllerMethodResolver {

    private final List<HandlerMethodArgumentResolver> exceptionHandlerResolvers;

    private final Map<Class<?>, ExceptionHandlerMethodResolver> exceptionHandlerCache = new ConcurrentHashMap<>(64);

    private final Map<ControllerAdviceBean, ExceptionHandlerMethodResolver> exceptionHandlerAdviceCache =
            new LinkedHashMap<>(64);

    @SuppressFBWarnings("CT_CONSTRUCTOR_THROW")
    ControllerMethodResolver(
            ReactiveAdapterRegistry adapterRegistry,
            ConfigurableApplicationContext context,
            List<HttpMessageReader<?>> readers) {

        Assert.notNull(context, "ApplicationContext is required");
        Assert.notNull(readers, "HttpMessageReader List is required");

        this.exceptionHandlerResolvers = exceptionHandlerResolvers(adapterRegistry, context);

        initControllerAdviceCaches(context);
    }

    private static List<HandlerMethodArgumentResolver> exceptionHandlerResolvers(
            ReactiveAdapterRegistry adapterRegistry, ConfigurableApplicationContext context) {

        return initResolvers(adapterRegistry, context, false, Collections.emptyList());
    }

    private static List<HandlerMethodArgumentResolver> initResolvers(
            ReactiveAdapterRegistry adapterRegistry,
            ConfigurableApplicationContext context,
            boolean supportDataBinding,
            List<HttpMessageReader<?>> readers) {

        ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
        boolean requestMappingMethod = !readers.isEmpty() && supportDataBinding;

        // Annotation-based...
        List<HandlerMethodArgumentResolver> result = new ArrayList<>(30);
        result.add(new RequestParamMethodArgumentResolver(beanFactory, adapterRegistry, false));
        result.add(new RequestParamMapMethodArgumentResolver(adapterRegistry));
        result.add(new PathVariableMethodArgumentResolver(beanFactory, adapterRegistry));
        result.add(new PathVariableMapMethodArgumentResolver(adapterRegistry));
        result.add(new MatrixVariableMethodArgumentResolver(beanFactory, adapterRegistry));
        result.add(new MatrixVariableMapMethodArgumentResolver(adapterRegistry));
        if (!readers.isEmpty()) {
            result.add(new RequestBodyMethodArgumentResolver(readers, adapterRegistry));
            result.add(new RequestPartMethodArgumentResolver(readers, adapterRegistry));
        }
        if (supportDataBinding) {
            result.add(new ModelAttributeMethodArgumentResolver(adapterRegistry, false));
        }
        result.add(new RequestHeaderMethodArgumentResolver(beanFactory, adapterRegistry));
        result.add(new RequestHeaderMapMethodArgumentResolver(adapterRegistry));
        result.add(new CookieValueMethodArgumentResolver(beanFactory, adapterRegistry));
        result.add(new ExpressionValueMethodArgumentResolver(beanFactory, adapterRegistry));
        result.add(new SessionAttributeMethodArgumentResolver(beanFactory, adapterRegistry));
        result.add(new RequestAttributeMethodArgumentResolver(beanFactory, adapterRegistry));

        // Type-based...
        if (!readers.isEmpty()) {
            result.add(new HttpEntityMethodArgumentResolver(readers, adapterRegistry));
        }
        result.add(new ModelMethodArgumentResolver(adapterRegistry));
        if (supportDataBinding) {
            result.add(new ErrorsMethodArgumentResolver(adapterRegistry));
        }
        result.add(new ServerWebExchangeMethodArgumentResolver(adapterRegistry));
        result.add(new PrincipalMethodArgumentResolver(adapterRegistry));
        if (requestMappingMethod) {
            result.add(new SessionStatusMethodArgumentResolver());
        }
        result.add(new WebSessionMethodArgumentResolver(adapterRegistry));
        if (KotlinDetector.isKotlinPresent()) {
            result.add(new ContinuationHandlerMethodArgumentResolver());
        }

        // Custom...
        // result.addAll(customResolvers.getCustomResolvers());

        // Catch-all...
        result.add(new RequestParamMethodArgumentResolver(beanFactory, adapterRegistry, true));
        if (supportDataBinding) {
            result.add(new ModelAttributeMethodArgumentResolver(adapterRegistry, true));
        }

        return result;
    }

    private void initControllerAdviceCaches(ApplicationContext applicationContext) {
        List<ControllerAdviceBean> beans = ControllerAdviceBean.findAnnotatedBeans(applicationContext);
        for (ControllerAdviceBean bean : beans) {
            Class<?> beanType = bean.getBeanType();
            if (beanType != null) {
                ExceptionHandlerMethodResolver resolver = new ExceptionHandlerMethodResolver(beanType);
                if (resolver.hasExceptionMappings()) {
                    this.exceptionHandlerAdviceCache.put(bean, resolver);
                }
            }
        }
    }

    /**
     * Look for an {@code @ExceptionHandler} method within the class of the given
     * controller method, and also within {@code @ControllerAdvice} classes that
     * are applicable to the class of the given controller method.
     *
     * @param ex            the exception to find a handler for
     * @param handlerMethod the controller method that raised the exception, or
     *                      if {@code null}, check only {@code @ControllerAdvice} classes.
     */
    @Nullable
    public InvocableHandlerMethod getExceptionHandlerMethod(Throwable ex, @Nullable HandlerMethod handlerMethod) {

        Class<?> handlerType = (handlerMethod != null ? handlerMethod.getBeanType() : null);
        Object exceptionHandlerObject = null;
        Method exceptionHandlerMethod = null;

        if (handlerType != null) {
            // Controller-local first...
            exceptionHandlerObject = handlerMethod.getBean();
            exceptionHandlerMethod = this.exceptionHandlerCache
                    .computeIfAbsent(handlerType, ExceptionHandlerMethodResolver::new)
                    .resolveMethodByThrowable(ex);
        }

        if (exceptionHandlerMethod == null) {
            // Global exception handlers...
            for (Map.Entry<ControllerAdviceBean, ExceptionHandlerMethodResolver> entry :
                    this.exceptionHandlerAdviceCache.entrySet()) {
                ControllerAdviceBean advice = entry.getKey();
                if (advice.isApplicableToBeanType(handlerType)) {
                    exceptionHandlerMethod = entry.getValue().resolveMethodByThrowable(ex);
                    if (exceptionHandlerMethod != null) {
                        exceptionHandlerObject = advice.resolveBean();
                        break;
                    }
                }
            }
        }

        if (exceptionHandlerObject == null || exceptionHandlerMethod == null) {
            return null;
        }

        InvocableHandlerMethod invocable = new InvocableHandlerMethod(exceptionHandlerObject, exceptionHandlerMethod);
        invocable.setArgumentResolvers(this.exceptionHandlerResolvers);
        return invocable;
    }
}
