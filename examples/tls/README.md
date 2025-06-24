# TLS/SSL Configuration Example

This example demonstrates how to configure SSL/TLS for gRPC clients and servers using Spring Boot's SSL Bundle feature.

## SSL Bundle Configuration

The example uses Spring Boot 3.1+ SSL Bundles, which provide a standardized way to configure SSL/TLS connections.

## Run Example

### 1. Generate certificates:

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

### 2. Run the application:

```shell
./gradlew :examples:tls:bootRun
```

### 3. Run tests:
```shell
./gradlew :examples:tls:test
```

## Configuration

The example uses SSL Bundle configuration:

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

## Benefits of SSL Bundle

1. **Consistency**: Aligns with Spring Boot's standard SSL configuration
2. **Reusability**: SSL bundles can be shared across different components
3. **Simplified Configuration**: Reduces boilerplate configuration
4. **Better Management**: Centralized SSL configuration management
5. **Hot Reloading**: Leverage Spring Boot's SSL bundle reloading capabilities
