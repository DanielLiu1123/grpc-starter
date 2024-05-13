package grpcstarter.extensions.transcoding;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.grpc.Metadata;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpHeaders;
import org.springframework.util.ReflectionUtils;

/**
 * @author Freeman
 */
public class DefaultHeaderConverter implements HeaderConverter {

    private final Set<String> removeHeaders; // lower case

    @SuppressFBWarnings("CT_CONSTRUCTOR_THROW")
    public DefaultHeaderConverter() {
        this.removeHeaders = getRemoveHeaders();
    }

    @Override
    public Metadata toMetadata(HttpHeaders headers) {
        Metadata metadata = new Metadata();
        headers.forEach((k, values) -> {
            if (!removeHeaders.contains(k.toLowerCase())) {
                values.forEach(v -> metadata.put(Metadata.Key.of(k, Metadata.ASCII_STRING_MARSHALLER), v));
            }
        });
        return metadata;
    }

    @Override
    public HttpHeaders toHttpHeaders(Metadata headers) {
        HttpHeaders result = new HttpHeaders();
        for (String key : headers.keys()) {
            if (!removeHeaders.contains(key.toLowerCase())
                    && !key.startsWith("grpc-")
                    && !key.endsWith(Metadata.BINARY_HEADER_SUFFIX)) {
                Optional.ofNullable(headers.getAll(Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER)))
                        .ifPresent(values -> values.forEach(value -> result.add(key, value)));
            }
        }
        return result;
    }

    private Set<String> getRemoveHeaders() {
        Set<String> result = new LinkedHashSet<>(findPublicStaticFinalStringFieldNames(HttpHeaders.class));

        result.removeIf(HttpHeaders.AUTHORIZATION::equalsIgnoreCase); // keep authorization
        return result;
    }

    private static Set<String> findPublicStaticFinalStringFieldNames(Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredFields())
                .filter(f -> Modifier.isPublic(f.getModifiers())
                        && Modifier.isStatic(f.getModifiers())
                        && Modifier.isFinal(f.getModifiers())
                        && f.getType() == String.class)
                .map(f -> ReflectionUtils.getField(f, null))
                .filter(Objects::nonNull)
                .map(Object::toString)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
    }
}
