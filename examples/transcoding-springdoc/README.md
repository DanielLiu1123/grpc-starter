## Run

```bash
cd "$(git rev-parse --show-toplevel)"
./gradlew :examples:transcoding-springdoc:bootRun
```

## Fetch OpenAPI

```bash
curl http://localhost:8080/v3/api-docs
```
