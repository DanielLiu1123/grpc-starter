## Run

```bash
cd "$(git rev-parse --show-toplevel)"
./gradlew :examples:transcoding:best-practice:bp-server:bootRun
```

Use grpcurl:

```bash
grpcurl -plaintext -d '{"id": 111}' localhost:9090 user.v1.UserService/GetUser
```

Use curl:

```bash
curl http://localhost:8080/v1/users/111
```