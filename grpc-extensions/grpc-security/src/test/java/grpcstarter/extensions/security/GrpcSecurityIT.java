package grpcstarter.extensions.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import grpcstarter.server.feature.exceptionhandling.annotation.GrpcAdvice;
import grpcstarter.server.feature.exceptionhandling.annotation.GrpcExceptionHandler;
import io.grpc.CallCredentials;
import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.protobuf.SimpleRequest;
import io.grpc.testing.protobuf.SimpleResponse;
import io.grpc.testing.protobuf.SimpleServiceGrpc;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Integration tests for gRPC Spring Security integration.
 *
 * <p>Covers: JWT authentication, anonymous access, invalid token rejection,
 * role-based access control, and {@link AccessDeniedException} mapping to
 * {@link Status#PERMISSION_DENIED} via {@link GrpcAdvice}.
 */
@SpringBootTest(
        classes = GrpcSecurityIT.Cfg.class,
        properties = {
            "grpc.client.base-packages=io.grpc",
            "grpc.server.in-process.name=GrpcSecurityIT",
            "grpc.client.in-process.name=GrpcSecurityIT"
        })
class GrpcSecurityIT {

    @Autowired
    SimpleServiceGrpc.SimpleServiceBlockingStub stub;

    @Autowired
    JwtEncoder jwtEncoder;

    @Test
    void testValidBearerToken_principalSetInSecurityContext() {
        String jwt = generateJwt("alice", List.of("ROLE_USER"));
        SimpleResponse resp = stub.withCallCredentials(bearerToken(jwt))
                .unaryRpc(SimpleRequest.newBuilder().setRequestMessage("whoami").build());
        assertThat(resp.getResponseMessage()).isEqualTo("alice");
    }

    @Test
    void testNoToken_anonymousAccessSucceeds() {
        SimpleResponse resp = stub.unaryRpc(
                SimpleRequest.newBuilder().setRequestMessage("whoami").build());
        assertThat(resp.getResponseMessage()).isEqualTo("anonymous");
    }

    @Test
    void testInvalidToken_returnsUnauthenticated() {
        assertThatCode(() -> stub.withCallCredentials(bearerToken("not-a-valid-jwt"))
                        .unaryRpc(SimpleRequest.newBuilder()
                                .setRequestMessage("whoami")
                                .build()))
                .isInstanceOf(StatusRuntimeException.class)
                .hasMessageContaining("UNAUTHENTICATED");
    }

    @Test
    void testAdminEndpoint_withAdminRole_succeeds() {
        String jwt = generateJwt("admin-user", List.of("ROLE_ADMIN"));
        SimpleResponse resp = stub.withCallCredentials(bearerToken(jwt))
                .unaryRpc(SimpleRequest.newBuilder()
                        .setRequestMessage("admin-only")
                        .build());
        assertThat(resp.getResponseMessage()).isEqualTo("admin-user");
    }

    @Test
    void testAdminEndpoint_withoutAdminRole_returnsPermissionDenied() {
        String jwt = generateJwt("alice", List.of("ROLE_USER"));
        assertThatCode(() -> stub.withCallCredentials(bearerToken(jwt))
                        .unaryRpc(SimpleRequest.newBuilder()
                                .setRequestMessage("admin-only")
                                .build()))
                .isInstanceOf(StatusRuntimeException.class)
                .hasMessageContaining("PERMISSION_DENIED");
    }

    private String generateJwt(String subject, List<String> roles) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(subject)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .claim("roles", roles)
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    private static CallCredentials bearerToken(String token) {
        return new CallCredentials() {
            @Override
            public void applyRequestMetadata(RequestInfo requestInfo, Executor appExecutor, MetadataApplier applier) {
                Metadata metadata = new Metadata();
                metadata.put(BearerTokenGrpcAuthenticationReader.AUTHORIZATION, "Bearer " + token);
                applier.apply(metadata);
            }
        };
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EnableMethodSecurity
    static class Cfg {

        static final RSAKey RSA_KEY;

        static {
            try {
                RSA_KEY = new RSAKeyGenerator(2048).generate();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Bean
        JwtEncoder jwtEncoder() {
            return new NimbusJwtEncoder(
                    new com.nimbusds.jose.jwk.source.ImmutableJWKSet<>(new com.nimbusds.jose.jwk.JWKSet(RSA_KEY)));
        }

        @Bean
        JwtDecoder jwtDecoder() throws Exception {
            return NimbusJwtDecoder.withPublicKey(RSA_KEY.toRSAPublicKey()).build();
        }

        @Bean
        AuthenticationManager authenticationManager(JwtDecoder jwtDecoder) {
            JwtAuthenticationProvider provider = new JwtAuthenticationProvider(jwtDecoder);
            provider.setJwtAuthenticationConverter(jwt -> {
                List<String> roles = jwt.getClaimAsStringList("roles");
                List<GrantedAuthority> authorities = roles == null
                        ? List.of()
                        : roles.stream()
                                .map(SimpleGrantedAuthority::new)
                                .map(GrantedAuthority.class::cast)
                                .toList();
                return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
            });
            return new ProviderManager(provider);
        }

        @Bean
        SecuredService securedService() {
            return new SecuredService();
        }

        @Bean
        AccessDeniedHandler accessDeniedHandler() {
            return new AccessDeniedHandler();
        }
    }

    /**
     * Test gRPC service that reads the SecurityContext to verify authentication
     * and enforces role-based access for "admin-only" requests.
     */
    static class SecuredService extends SimpleServiceGrpc.SimpleServiceImplBase {

        @Override
        public void unaryRpc(SimpleRequest request, StreamObserver<SimpleResponse> responseObserver) {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            String principal = "anonymous";
            if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
                principal = auth.getName();
                if ("admin-only".equals(request.getRequestMessage())) {
                    boolean isAdmin =
                            auth.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
                    if (!isAdmin) {
                        throw new AccessDeniedException("Access denied: requires ROLE_ADMIN");
                    }
                }
            } else if ("admin-only".equals(request.getRequestMessage())) {
                throw new AccessDeniedException("Access denied: requires ROLE_ADMIN");
            }

            responseObserver.onNext(
                    SimpleResponse.newBuilder().setResponseMessage(principal).build());
            responseObserver.onCompleted();
        }
    }

    /** Maps {@link AccessDeniedException} to {@link Status#PERMISSION_DENIED}. */
    @GrpcAdvice
    static class AccessDeniedHandler {

        @GrpcExceptionHandler
        Status handle(AccessDeniedException e) {
            return Status.PERMISSION_DENIED.withDescription(e.getMessage());
        }
    }
}
