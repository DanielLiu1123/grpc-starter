package grpcstarter.extensions.security;

import io.grpc.ForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import org.springframework.core.Ordered;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * A gRPC {@link ServerInterceptor} that integrates with Spring Security.
 *
 * <p>For each incoming call, it attempts to read an {@link Authentication} token via
 * {@link GrpcAuthenticationReader}. If credentials are found, they are authenticated using
 * the {@link AuthenticationManager} and the result is stored in {@link SecurityContextHolder}.
 * If authentication fails, the call is closed with {@link Status#UNAUTHENTICATED}.
 *
 * <p>If no credentials are present the call proceeds anonymously, allowing method-level
 * security (e.g. {@code @PreAuthorize}) to control access.
 *
 * <p>The {@link SecurityContextHolder} is always cleared after the call completes or is cancelled.
 *
 * @author Freeman
 */
public class GrpcSecurityServerInterceptor implements ServerInterceptor, Ordered {

    private final GrpcAuthenticationReader authenticationReader;
    private final AuthenticationManager authenticationManager;
    private final int order;

    public GrpcSecurityServerInterceptor(
            GrpcAuthenticationReader authenticationReader, AuthenticationManager authenticationManager, int order) {
        this.authenticationReader = authenticationReader;
        this.authenticationManager = authenticationManager;
        this.order = order;
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        Authentication token = authenticationReader.readAuthentication(call, headers);
        if (token != null) {
            try {
                Authentication authenticated = authenticationManager.authenticate(token);
                SecurityContext ctx = SecurityContextHolder.createEmptyContext();
                ctx.setAuthentication(authenticated);
                SecurityContextHolder.setContext(ctx);
            } catch (AuthenticationException e) {
                call.close(Status.UNAUTHENTICATED.withDescription(e.getMessage()), new Metadata());
                return new ServerCall.Listener<>() {};
            }
        }
        try {
            return new SecurityContextClearingListener<>(next.startCall(call, headers));
        } catch (Exception e) {
            SecurityContextHolder.clearContext();
            throw e;
        }
    }

    @Override
    public int getOrder() {
        return order;
    }

    private static final class SecurityContextClearingListener<ReqT>
            extends ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT> {

        SecurityContextClearingListener(ServerCall.Listener<ReqT> delegate) {
            super(delegate);
        }

        @Override
        public void onComplete() {
            try {
                super.onComplete();
            } finally {
                SecurityContextHolder.clearContext();
            }
        }

        @Override
        public void onCancel() {
            try {
                super.onCancel();
            } finally {
                SecurityContextHolder.clearContext();
            }
        }
    }
}
