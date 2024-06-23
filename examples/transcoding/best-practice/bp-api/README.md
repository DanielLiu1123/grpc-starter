## Project Configuration

Using [buf](https://buf.build/) to manage proto files.

[protoc-gen-openapi](https://github.com/google/gnostic/blob/main/cmd/protoc-gen-openapi/README.md) is not in the [Buf Schema Registry](https://buf.build/plugins), 
so it can't use it directly with buf [remote plugin](https://buf.build/docs/generate/overview#generating-with-remote-plugins).

Refer to [issues#821](https://github.com/bufbuild/plugins/issues/821).

To use `protoc-gen-openapi` with buf, you can use the following steps:

1. Install `protoc-gen-openapi`

    ```bash
    go install github.com/google/gnostic/cmd/protoc-gen-openapi@latest
    ```

2. Use as local plugin in buf.gen.yaml

    ```yaml
    plugins:
      - local: protoc-gen-openapi
        out: gen/openapi
    ```
    
    Make sure $GOPATH/bin is in your PATH: `export PATH="PATH:$(go env GOPATH)/bin"`

## Generate Code

```bash
cd examples/transcoding/best-practice/bp-api && buf generate
```