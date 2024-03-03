## Run Example

1. Generate certificates:

    ```shell
    openssl genpkey -algorithm RSA -out ca.key
    openssl req -new -key ca.key -out ca.csr
    openssl x509 -req -in ca.csr -signkey ca.key -out ca.crt
    
    openssl genpkey -algorithm RSA -out server.key
    openssl req -new -key server.key -out server.csr
    
    openssl x509 -req -days 3650 -in server.csr -CA ca.crt -CAkey ca.key -out server.crt
    ```

2. Run tests
