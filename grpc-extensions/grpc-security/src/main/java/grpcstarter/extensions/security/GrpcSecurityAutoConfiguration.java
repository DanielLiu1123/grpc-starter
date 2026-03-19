package grpcstarter.extensions.security;

import grpcstarter.server.ConditionOnGrpcServerEnabled;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Auto-configuration for gRPC Spring Security integration.
 *
 * @author Freeman
 */
@AutoConfiguration
@ConditionalOnClass(SecurityContextHolder.class)
@ConditionOnGrpcServerEnabled
@ConditionalOnProperty(prefix = GrpcSecurityProperties.PREFIX, name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(GrpcSecurityProperties.class)
public class GrpcSecurityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public GrpcAuthenticationReader grpcAuthenticationReader() {
        return new BearerTokenGrpcAuthenticationReader();
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = GrpcSecurityProperties.Server.PREFIX, name = "enabled", matchIfMissing = true)
    @ConditionalOnBean(AuthenticationManager.class)
    static class ServerSecurity {

        @Bean
        @ConditionalOnMissingBean
        public GrpcSecurityServerInterceptor grpcSecurityServerInterceptor(
                GrpcAuthenticationReader reader, AuthenticationManager authManager, GrpcSecurityProperties props) {
            return new GrpcSecurityServerInterceptor(
                    reader, authManager, props.getServer().getOrder());
        }
    }
}
