```shell
cd $(git rev-parse --show-toplevel)/examples/metrics
cd prometheus-grafana-stack
mkdir -p config prometheus-data grafana-data
chmod 777 prometheus-data grafana-data
```

```shell
docker compose up -d
```