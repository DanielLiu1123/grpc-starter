```yaml
grpc:
  client:
    authority: localhost:9090     # global default authority
    max-inbound-message-size: 4MB         # global default max message size
    max-inbound-metadata-size: 8KB        # global default max metadata size
    metadata:                     # global default metadata
      - key: foo1
        values: [bar1, bar2]
    channels:
      - authority: localhost:9090 # override default authority
        max-inbound-message-size: 8MB     # override default max message size
        max-inbound-metadata-size: 16KB   # override default max metadata size
        metadata:                 # merge with default metadata, result is {foo1=[bar1, bar2], foo2=[bar3, bar4]}
          - key: foo2
            values: [bar3, bar4]
        services:                 # services to apply this channel
          - fm.foo.v1.FooService
        stubs:                    # stub classes to apply this channel, use this or services, not this first if both set
          - com.freemanan.foo.v1.api.FooServiceGrpc.FooServiceBlockingStub
          - com.freemanan.foo.v1.api.FooServiceGrpc.FooServiceStub
```

see [example](https://github.com/DanielLiu1123/grpc-starter/blob/main/grpc-boot-autoconfigure/grpc-client-boot-autoconfigure/src/main/resources/application-grpc-client-boot-starter-example.yaml).
