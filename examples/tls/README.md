# TLS Example

This example demonstrates SSL/TLS configuration for gRPC using Spring Boot SSL Bundles.

## Quick Start

1. **Generate certificates:**
   ```shell
   cd $(git rev-parse --show-toplevel)/examples/tls/src/main/resources

   # Generate CA private key
   openssl genpkey -algorithm RSA -out ca.key

   # Generate CA certificate with proper CA extensions
   openssl req -new -x509 -key ca.key -out ca.crt -days 3650 -subj "/CN=Test CA" \
     -extensions v3_ca -config <(
     echo '[req]'
     echo 'distinguished_name = req'
     echo '[v3_ca]'
     echo 'basicConstraints = CA:TRUE'
     echo 'keyUsage = keyCertSign, cRLSign'
     echo 'subjectKeyIdentifier = hash'
     )

   # Generate server private key
   openssl genpkey -algorithm RSA -out server.key

   # Generate server certificate signing request
   openssl req -new -key server.key -out server.csr -subj "/CN=localhost"

   # Generate server certificate signed by CA with SAN extension
   openssl x509 -req -days 3650 -in server.csr -CA ca.crt -CAkey ca.key -out server.crt \
     -extensions v3_req -extfile <(
     echo '[v3_req]'
     echo 'subjectKeyIdentifier = hash'
     echo 'authorityKeyIdentifier = keyid,issuer'
     echo 'subjectAltName = @alt_names'
     echo '[alt_names]'
     echo 'DNS.1 = localhost'
     echo 'IP.1 = 127.0.0.1'
     )

   # Clean up temporary files
   rm *.csr ca.key
   ```

2. **Run:**
   ```shell
   ./gradlew :examples:tls:bootRun
   ```
3. **Access:**
   ```shell
   grpcurl -cacert $(git rev-parse --show-toplevel)/examples/tls/src/main/resources/ca.crt -d '{"requestMessage": "Cool!"}' 127.0.0.1:9090 grpc.testing.SimpleService/UnaryRpc
   grpcurl --insecure -d '{"requestMessage": "Cool!"}' 127.0.0.1:9090 grpc.testing.SimpleService/UnaryRpc
   ```
