# TLS/SSL Configuration Example

This example demonstrates how to configure SSL/TLS for gRPC clients and servers using Spring Boot's SSL Bundle feature.

## SSL Bundle Configuration (Recommended)

The example uses Spring Boot 3.1+ SSL Bundles, which provide a standardized way to configure SSL/TLS connections. This is the preferred approach over the legacy TLS configuration.

### Configuration Files

- `application.yaml` - SSL Bundle configuration using PEM format (recommended)
- `application-jks.yaml` - SSL Bundle configuration using JKS/PKCS12 format
- `application-legacy.yaml` - Legacy TLS configuration (deprecated, shows deprecation warnings)

## Run Example

### 1. Generate certificates:

#### For PEM format (default configuration):

```shell
# Generate CA private key
openssl genpkey -algorithm RSA -out ca.key

# Generate CA certificate request
openssl req -new -key ca.key -out ca.csr -subj "/CN=Test CA"

# Generate self-signed CA certificate
openssl x509 -req -in ca.csr -signkey ca.key -out ca.crt -days 3650

# Generate server private key
openssl genpkey -algorithm RSA -out server.key

# Generate server certificate request
openssl req -new -key server.key -out server.csr -subj "/CN=localhost"

# Generate server certificate signed by CA
openssl x509 -req -days 3650 -in server.csr -CA ca.crt -CAkey ca.key -out server.crt -extensions v3_req
```

#### For JKS/PKCS12 format (application-jks.yaml):

```shell
# Convert PEM certificates to PKCS12 format
openssl pkcs12 -export -out server.p12 -inkey server.key -in server.crt -password pass:server-password

# Create truststore with CA certificate
keytool -import -file ca.crt -alias ca -keystore ca.p12 -storetype PKCS12 -storepass ca-password -noprompt
```

### 2. Run the application:

#### Using SSL Bundle configuration (default):
```shell
./gradlew :examples:tls:bootRun
```

#### Using JKS format:
```shell
./gradlew :examples:tls:bootRun --args='--spring.profiles.active=jks'
```

#### Using legacy TLS configuration:
```shell
./gradlew :examples:tls:bootRun --args='--spring.profiles.active=legacy'
```

### 3. Run tests:
```shell
./gradlew :examples:tls:test
```

## Configuration Comparison

### SSL Bundle (Recommended)
```yaml
spring:
  ssl:
    bundle:
      pem:
        server-bundle:
          keystore:
            certificate: "classpath:server.crt"
            private-key: "classpath:server.key"
        client-bundle:
          truststore:
            certificate: "classpath:ca.crt"

grpc:
  server:
    ssl-bundle: server-bundle
  client:
    ssl-bundle: client-bundle
```

### Legacy TLS (Deprecated)
```yaml
grpc:
  server:
    tls:
      key-manager:
        cert-chain: classpath:server.crt
        private-key: classpath:server.key
  client:
    tls:
      trust-manager:
        root-certs: classpath:ca.crt
```

## Benefits of SSL Bundle

1. **Consistency**: Aligns with Spring Boot's standard SSL configuration
2. **Reusability**: SSL bundles can be shared across different components
3. **Simplified Configuration**: Reduces boilerplate configuration
4. **Better Management**: Centralized SSL configuration management
5. **Hot Reloading**: Leverage Spring Boot's SSL bundle reloading capabilities

## Migration Guide

See [SSL_BUNDLE_MIGRATION.md](../../SSL_BUNDLE_MIGRATION.md) for detailed migration instructions from legacy TLS configuration to SSL Bundle.
