package grpcstarter.extensions.security;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.Authentication;

/**
 * Strategy interface for reading an {@link Authentication} token from an incoming gRPC call.
 *
 * <p>Implementations extract credentials from the gRPC {@link Metadata} (headers) and return
 * an unauthenticated {@link Authentication} token that will be passed to Spring Security's
 * {@link org.springframework.security.authentication.AuthenticationManager} for verification.
 *
 * <p>Returning {@code null} means no credentials were found; the request is treated as anonymous
 * and continues without authentication (subject to method-level security).
 *
 * @author Freeman
 * @see BearerTokenGrpcAuthenticationReader
 */
public interface GrpcAuthenticationReader {

    /**
     * Attempt to read an authentication token from the given call and its metadata.
     *
     * @param call    the incoming gRPC server call
     * @param headers the call metadata (headers)
     * @return an unauthenticated {@link Authentication} token, or {@code null} if no credentials found
     */
    @Nullable Authentication readAuthentication(ServerCall<?, ?> call, Metadata headers);
}
