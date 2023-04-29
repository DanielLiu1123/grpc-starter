# gRPC Starter

[![Build](https://img.shields.io/github/actions/workflow/status/DanielLiu1123/httpexchange-spring-boot-starter/build.yml?branch=main)](https://github.com/DanielLiu1123/httpexchange-spring-boot-starter/actions)
[![Maven Central](https://img.shields.io/maven-central/v/com.freemanan/httpexchange-spring-boot-starter)](https://search.maven.org/artifact/com.freemanan/httpexchange-spring-boot-starter)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

The missing starter for Spring 6.x declarative HTTP stub. 

The goal is to provide a Spring Boot Starter for declarative HTTP stub similar to `Spring Cloud OpenFeign`, but completely driven by configuration and without the need for any additional annotations.

## What is it

Spring 6.0 has provided its own support for declarative HTTP clients.

```java

@HttpExchange("https://my-json-server.typicode.com")
public interface PostApi {
    @GetExchange("/typicode/demo/posts/{id}")
    Post getPost(@PathVariable("id") int id);
}
```

This is basic usage:

```java

@SpringBootApplication
public class App {

    public static void main(String[] args) {
        var ctx = SpringApplication.run(App.class, args);

        PostApi postApi = ctx.getBean(PostApi.class);
        Post post = postApi.getPost(1);
    }

    @Bean
    HttpServiceProxyFactory httpServiceProxyFactory(WebClient.Builder builder) {
        return HttpServiceProxyFactory
                .builder(WebClientAdapter.forClient(builder.build()))
                .build();
    }

    @Bean
    PostApi postApi(HttpServiceProxyFactory factory) {
        return factory.createClient(UserClient.class);
    }

}

```

## Quick Start

```groovy
// gradle
implementation 'com.freemanan:httpexchange-spring-boot-starter:3.0.9'
```

```xml
<!-- maven -->
<dependency>
    <groupId>com.freemanan</groupId>
    <artifactId>httpexchange-spring-boot-starter</artifactId>
    <version>3.0.9</version>
</dependency>
```

You can simplify the code as follows:

```java

@SpringBootApplication
@EnableExchangeClients
public class App {

    public static void main(String[] args) {
        var ctx = SpringApplication.run(App.class, args);

        PostApi postApi = ctx.getBean(PostApi.class);
        Post post = postApi.getPost(1);
    }

}
```

If you have experiences with `Spring Cloud OpenFeign`, you will find that the usage is very similar.

`httpexhange-spring-boot-starter` will automatically scan the interfaces annotated with `@HttpExchange` and create the
corresponding beans.

## Core Features

- Spring web annotations support

  `httpexhange-spring-boot-starter` supports to use spring web annotations to generate HTTP stub. e.g. `@RequestMapping`, `@GetMapping`, `@PostMapping` etc.

  ```java
  @RequestMapping("/foo")
  public interface PostApi {
      @GetMapping("/{id}")
      Post getPost(@PathVariable int id);
  }
  ```
  
  The advantage of this feature is that the behavior of the server and stub can be consistent.

- Automatically scan interfaces annotated with `@HttpExchange` and create corresponding beans.

  All you need to do is add the `@EnableExchangeClients` annotation to your main class.

- Support url variables.

  ```java
  @HttpExchange("${api.post.url}")
  public interface PostApi {
      @GetExchange("/typicode/demo/posts/{id}")
      Post getPost(@PathVariable("id") int id);
  }
  ```

- Support validation.

  ```java
  @HttpExchange("${api.post.url}")
  @Validated
  public interface PostApi {
      @GetExchange("/typicode/demo/posts/{id}")
      Post getPost(@PathVariable("id") @Min(1) @Max(3) int id);
  }
  ```
  NOTE: this feature needs `spring-boot` version >= 3.0.3,
  see [issue](https://github.com/spring-projects/spring-framework/issues/29782)
  and [tests](src/test/java/com/freemanan/starter/httpexchange/ValidationTests.java)

- Convert Java Bean to Query String.

  In Spring Web/WebFlux (server side), it will automatically convert query string to Java Bean,
  but `Spring Cloud OpenFeign` or `Exchange stub of Spring 6` does not support to convert Java bean to query string by
  default. In `Spring Cloud OpenFeign` you need `@SpringQueryMap` to achieve this feature.

  `httpexhange-spring-boot-starter` supports this feature by default, and you don't need any another annotation.

  ```java
  public interface PostApi {
      @GetExchange
      List<Post> findAll(Post condition);
  }
  ```

  Auto convert non-null fields of `condition` to query string.

- Customize Resolvers.

  ```java
  @Bean
  HttpServiceArgumentResolver yourHttpServiceArgumentResolver() {
      return new YourHttpServiceArgumentResolver();
  }
  
  @Bean
  StringValueResolver yourStringValueResolver() {
      return new YourStringValueResolver();
  }
  ```
  
  see `org.springframework.web.service.invoker.HttpServiceProxyFactory.Builder`, `httpexhange-spring-boot-starter` will detect all of the `HttpServiceArgumentResolver` beans and `StringValueResolver` (only one), then apply them to build the `HttpServiceProxyFactory`.

- Configuration Driven.

  `httpexhange-spring-boot-starter` provides a lot of configuration properties to customize the behavior of the stub.

  You can configure the `base-url`, `timeout` and `headers` for each stub, `httpexhange-spring-boot-starter` will
  reuse `WebClient` as much as possible.

  ```yaml
  http-exchange:
    base-url: http://api-gateway # global base-url
    response-timeout: 10000      # global timeout
    headers:                     # global headers
      - key: X-App-Name
        values: ${spring.application.name}
    clients:
      - name: OrderApi
        base-url: http://order   # stub specific base-url, will override global base-url
        response-timeout: 1000   # stub specific timeout, will override global timeout
        headers:                 # stub specific headers, will merge with global headers
          - key: X-Key
            values: [value1, value2]
      - name: UserApi
        base-url: user
        response-timeout: 2000
      - stub-class: com.example.FooApi
        base-url: service-foo.namespace
  ```

  `httpexhange-spring-boot-starter` use property `name` or `stub-class` to identify the stub, use `stub-class`
  first if configured, otherwise use `name` to identify the stub.

  For example, there is a stub interface: `com.example.PostApi`, you can
  use `name: PostApi`, `name: com.example.PostApi`, `name: post-api` or `stub-class: com.example.PostApi` to identify
  the stub.

## Version

The major/minor version of this project is consistent with the version of `Spring Boot`. Therefore, `3.0.x` of this
project should work with `3.0.x` of `Spring Boot`. Please always use the latest version.

| Spring Boot | httpexchange-spring-boot-starter |
|-------------|----------------------------------|
| 3.0.x       | 3.0.9                            |

## License

The MIT License.
