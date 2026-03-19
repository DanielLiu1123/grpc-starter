package grpcstarter.extensions.security;

import static io.grpc.Metadata.ASCII_STRING_MARSHALLER;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import java.util.Locale;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;

/**
 * A {@link GrpcAuthenticationReader} that extracts a Bearer token from the {@code Authorization}
 * gRPC metadata header.
 *
 * <p>Looks for a header of the form {@code Authorization: Bearer <token>} (case-insensitive
 * prefix check). Returns a {@link BearerTokenAuthenticationToken} if found, or {@code null}
 * if the header is absent or not in Bearer format.
 *
 * @author Freeman
 */
public class BearerTokenGrpcAuthenticationReader implements GrpcAuthenticationReader {

    static final Metadata.Key<String> AUTHORIZATION = Metadata.Key.of("authorization", ASCII_STRING_MARSHALLER);

    private static final String BEARER_PREFIX = "bearer ";

    @Override
    @Nullable public Authentication readAuthentication(ServerCall<?, ?> call, Metadata headers) {
        String authHeader = headers.get(AUTHORIZATION);
        if (authHeader == null) {
            return null;
        }
        if (!authHeader.toLowerCase(Locale.ROOT).startsWith(BEARER_PREFIX)) {
            return null;
        }
        String token = authHeader.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty()) {
            return null;
        }
        return new BearerTokenAuthenticationToken(token);
    }
}
