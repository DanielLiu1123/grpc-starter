## Run

```bash
cd "$(git rev-parse --show-toplevel)"
./gradlew :examples:transcoding-springdocs:bootRun
```

## Fetch OpenAPI

```bash
curl http://localhost:8080/v3/api-docs
```
