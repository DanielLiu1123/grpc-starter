package com.freemanan.starter.grpc.extensions.jsontranscoder;

import io.grpc.Metadata;
import io.grpc.internal.GrpcUtil;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.springframework.http.HttpHeaders;
import org.springframework.util.ReflectionUtils;

/**
 * @author Freeman
 */
public class DefaultGrpcHeaderConverter implements GrpcHeaderConverter {

    private final Set<String> removeHeaders;

    public DefaultGrpcHeaderConverter() {
        this.removeHeaders = getRemoveHeaders();
    }

    @Override
    public Metadata toRequestMetadata(HttpHeaders headers) {
        // remove http internal headers
        new HashSet<>(headers.keySet()).stream().filter(removeHeaderPredicate()).forEach(headers::remove);

        Metadata metadata = new Metadata();
        headers.forEach((k, values) -> {
            Metadata.Key<String> key = Metadata.Key.of(k, Metadata.ASCII_STRING_MARSHALLER);
            values.forEach(v -> metadata.put(key, v));
        });
        return metadata;
    }

    @Override
    public HttpHeaders toResponseHeader(Metadata headers) {
        // remove grpc internal headers
        new HashSet<>(headers.keys())
                .stream()
                        .filter(removeMetadataPredicate())
                        .forEach(key -> headers.removeAll(Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER)));

        // remove content-type
        headers.removeAll(GrpcUtil.CONTENT_TYPE_KEY);

        HttpHeaders result = new HttpHeaders();
        headers.keys().forEach(key -> Optional.ofNullable(
                        headers.getAll(Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER)))
                .ifPresent(values -> values.forEach(value -> result.add(key, value))));
        return result;
    }

    protected Set<String> getRemoveHeaders() {
        Set<String> headerKeys = findPublicStaticFinalStringFieldNames(HttpHeaders.class);
        Set<String> result = new TreeSet<>(headerKeys);

        // do not remove cookies
        result.removeIf(HttpHeaders.COOKIE::equalsIgnoreCase);
        return result;
    }

    protected static Set<String> findPublicStaticFinalStringFieldNames(Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredFields())
                .filter(f -> Modifier.isPublic(f.getModifiers())
                        && Modifier.isStatic(f.getModifiers())
                        && Modifier.isFinal(f.getModifiers())
                        && f.getType() == String.class)
                .map(f -> ReflectionUtils.getField(f, null))
                .filter(Objects::nonNull)
                .map(Object::toString)
                .collect(Collectors.toSet());
    }

    protected Predicate<String> removeHeaderPredicate() {
        return key -> removeHeaders.stream().anyMatch(key::equalsIgnoreCase);
    }

    protected Predicate<String> removeMetadataPredicate() {
        return key -> key.toLowerCase().startsWith("grpc-");
    }
}
