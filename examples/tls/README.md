# TLS Example

This example demonstrates SSL/TLS configuration for gRPC using Spring Boot SSL Bundles.

## Quick Start

1. **Generate certificates:**
   ```shell
   openssl genpkey -algorithm RSA -out ca.key
   openssl req -new -key ca.key -out ca.csr -subj "/CN=Test CA"
   openssl x509 -req -in ca.csr -signkey ca.key -out ca.crt -days 3650

   openssl genpkey -algorithm RSA -out server.key
   openssl req -new -key server.key -out server.csr -subj "/CN=localhost"
   openssl x509 -req -days 3650 -in server.csr -CA ca.crt -CAkey ca.key -out server.crt
   ```

2. **Run:**
   ```shell
   ./gradlew :examples:tls:bootRun
   ```
