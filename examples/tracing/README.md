1. Start jaeger server

    ```shell
    docker run --rm --name jaeger \
      -p 16686:16686 \
      -p 4317:4317 \
      -p 4318:4318 \
      -p 5778:5778 \
      -p 9411:9411 \
      cr.jaegertracing.io/jaegertracing/jaeger:latest
    ```

2. Start the example

3. Visit http://localhost:16686 to view the traces
   