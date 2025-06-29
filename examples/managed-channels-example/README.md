# ManagedChannels API Example

This example demonstrates how to use the `ManagedChannels` API to access configured gRPC channels by name.

## Configuration

```yaml
grpc:
  client:
    base-packages:
      - io.grpc
    channels:
      - name: "service-a"
        authority: "localhost:9090"
        stubs:
          - "io.grpc.health.v1.**"
      - name: "service-b"
        authority: "localhost:9091"
        stubs:
          - "io.grpc.testing.protobuf.**"
```

## Usage

```java
@Component
public class MyService {
    
    private final ManagedChannels managedChannels;
    
    public MyService(ManagedChannels managedChannels) {
        this.managedChannels = managedChannels;
    }
    
    @Bean("service-a-stub")
    public HealthGrpc.HealthBlockingStub serviceAStub() {
        ManagedChannel channel = managedChannels.getChannel("service-a");
        return HealthGrpc.newBlockingStub(channel);
    }
    
    @Bean("service-b-stub")
    public SimpleServiceGrpc.SimpleServiceBlockingStub serviceBStub() {
        ManagedChannel channel = managedChannels.getChannel("service-b");
        return SimpleServiceGrpc.newBlockingStub(channel);
    }
    
    public void listAllChannels() {
        Set<String> channelNames = managedChannels.getChannelNames();
        System.out.println("Available channels: " + channelNames);
    }
}
```

## Benefits

- **Centralized channel management**: Access all configured channels through a single API
- **Type safety**: Get compile-time checking for channel names
- **Flexibility**: Easily create custom stub beans with specific configurations
- **Consistency**: Follows Spring's established patterns (similar to SslBundles)
