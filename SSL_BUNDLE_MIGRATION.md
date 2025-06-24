# SSL Bundle Support for gRPC Client

This document describes the new SSL Bundle support for gRPC client configuration, introduced in version 3.5.0.

## Overview

Spring Boot 3.1 introduced [SSL Bundles](https://spring.io/blog/2023/06/07/securing-spring-boot-applications-with-ssl/) as a standardized way to configure SSL/TLS connections. SSL Bundles provide a more convenient and consistent approach to SSL configuration across different Spring Boot components.

The gRPC starter now supports SSL Bundles as the preferred way to configure SSL/TLS for gRPC clients, while maintaining backward compatibility with the existing TLS configuration.

## Configuration

### SSL Bundle Configuration (Standard Spring Boot)

First, configure your SSL bundles in the standard Spring Boot way:

```yaml
spring:
  ssl:
    bundle:
      jks:
        server:
          key:
            alias: "server"
          keystore:
            location: "classpath:server.p12"
            password: "secret"
            type: "PKCS12"
        client:
          truststore:
            location: "classpath:ca.p12"
            password: "secret"
      pem:
        grpc-client:
          keystore:
            certificate: "classpath:client.crt"
            private-key: "classpath:client.key"
          truststore:
            certificate: "classpath:ca.crt"
```

### gRPC Client Configuration with SSL Bundle

#### Global SSL Bundle Configuration

Apply SSL bundle to all gRPC clients by default:

```yaml
grpc:
  client:
    ssl-bundle: client  # Reference to spring.ssl.bundle.jks.client
    base-packages:
      - com.example.grpc
```

#### Channel-Specific SSL Bundle Configuration

Override global settings for specific channels:

```yaml
grpc:
  client:
    ssl-bundle: client  # Global default
    channels:
      - authority: secure-service:443
        ssl-bundle: server  # Override global setting
        services:
          - com.example.SecureService
      - authority: another-service:443
        # Uses global ssl-bundle: client
        services:
          - com.example.AnotherService
```

## Migration from TLS Configuration

### Before (Deprecated)

```yaml
grpc:
  client:
    tls:
      trust-manager:
        root-certs: classpath:ca.crt
      key-manager:
        cert-chain: classpath:client.crt
        private-key: classpath:client.key
```

### After (Recommended)

```yaml
spring:
  ssl:
    bundle:
      pem:
        grpc-client:
          keystore:
            certificate: "classpath:client.crt"
            private-key: "classpath:client.key"
          truststore:
            certificate: "classpath:ca.crt"

grpc:
  client:
    ssl-bundle: grpc-client
```

## Configuration Priority

The configuration priority order is:

1. Channel-specific `ssl-bundle`
2. Channel-specific `tls` (deprecated)
3. Global `ssl-bundle`
4. Global `tls` (deprecated)
5. Plain text

## Benefits

1. **Consistency**: Aligns with Spring Boot's standard SSL configuration approach
2. **Reusability**: SSL bundles can be shared across different components (web server, data sources, etc.)
3. **Simplified Configuration**: Reduces boilerplate configuration
4. **Better Management**: Centralized SSL configuration management
5. **Hot Reloading**: Leverage Spring Boot's SSL bundle reloading capabilities (if supported)

## Deprecation Notice

The existing `tls` configuration is deprecated as of version 3.5.0 and will be removed in a future version. When using the deprecated `tls` configuration, you will see a warning message:

```
Using deprecated 'tls' configuration for gRPC client. Please migrate to 'ssl-bundle' configuration. The 'tls' configuration will be removed in a future version.
```

## Examples

### Complete Example with Multiple Services

```yaml
spring:
  ssl:
    bundle:
      jks:
        secure-service:
          keystore:
            location: "classpath:secure-service.p12"
            password: "secure-password"
        public-service:
          truststore:
            location: "classpath:public-ca.p12"
            password: "public-password"

grpc:
  client:
    ssl-bundle: public-service  # Default for all services
    base-packages:
      - com.example.grpc
    channels:
      - authority: secure-internal:443
        ssl-bundle: secure-service  # Override for internal service
        services:
          - com.example.grpc.SecureInternalService
      - authority: public-api:443
        # Uses default ssl-bundle: public-service
        services:
          - com.example.grpc.PublicApiService
      - authority: insecure-dev:9090
        # No ssl-bundle specified, uses plaintext
        services:
          - com.example.grpc.DevService
```

### In-Process Configuration

In-process channels ignore SSL bundle configuration:

```yaml
grpc:
  server:
    in-process:
      name: test-server
  client:
    in-process:
      name: test-server
    ssl-bundle: client  # Ignored for in-process channels
```

## Requirements

- Spring Boot 3.1 or later
- grpc-starter 3.5.0 or later

## Troubleshooting

### SSL Bundle Not Found

If you see an error like "SSL bundle name 'xxx' cannot be found", make sure:

1. The SSL bundle is properly configured under `spring.ssl.bundle.*`
2. The bundle name matches exactly (case-sensitive)
3. You're using Spring Boot 3.1 or later

### SSL Context Creation Failed

If SSL context creation fails, check:

1. Certificate and key files exist and are readable
2. Passwords are correct
3. Certificate formats are supported (PEM, JKS, PKCS12)
