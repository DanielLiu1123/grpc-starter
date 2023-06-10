Introduction
----

This project provides a series of Spring Boot starters for the gRPC ecosystem, offering automatic configuration and
highly extensible capabilities to enhance the integration between Spring Boot and gRPC.

## Core Features

* Automatic configuration of gRPC server

    - Health checks
    - Exception handling

* Automatic configuration of gRPC client

    - No additional annotations required, gRPC clients can be directly injected using `@Autowired`, `@Resource`, and
      other Spring annotations, following the Spring Bean lifecycle.

* Highly extensible capabilities

    - JSON transcoder: Allows using the same codebase for both gRPC and HTTP/JSON.
    - Protobuf validation: Based on [protoc-gen-validate](https://github.com/bufbuild/protoc-gen-validate).
    - Testing support: Extends `@SpringBootTest` for convenient unit and integration testing.

## Contact Author

<a href="mailto:freemanlau1228@gmail.com"> Email </a>