## Setting up Prometheus and Grafana
```shell
cd $(git rev-parse --show-toplevel)/examples/metrics/prometheus-grafana-stack
mkdir -p config prometheus-data grafana-data
chmod 777 prometheus-data grafana-data

docker compose up -d
```

## Start the App
```shell
cd $(git rev-parse --show-toplevel)

./gradlew :examples:metrics:bootRun
```

## Access Grafana Dashboard

```shell
open http://localhost:3000
```

## Clean Up

```shell
cd $(git rev-parse --show-toplevel)/examples/metrics/prometheus-grafana-stack

docker compose down
```