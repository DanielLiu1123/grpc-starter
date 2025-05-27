package grpcstarter.extensions.transcoding.openapi;

import com.google.api.AnnotationsProto;
import com.google.api.HttpRule;
import com.google.api.pathtemplate.PathTemplate;
import com.google.protobuf.Descriptors;
import grpcstarter.extensions.transcoding.GrpcTranscodingProperties;
import io.grpc.BindableService;
import io.grpc.ServerServiceDefinition;
import io.grpc.protobuf.ProtoFileDescriptorSupplier;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.jackson.ModelResolver;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import jakarta.annotation.Nullable;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.core.providers.ObjectMapperProvider;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * @author Freeman
 */
public class GrpcTranscodingOpenApiCustomizer implements OpenApiCustomizer {

    private static final Logger log = LoggerFactory.getLogger(GrpcTranscodingOpenApiCustomizer.class);
    private final List<ServerServiceDefinition> serviceDefinitions;
    private final GrpcTranscodingProperties grpcTranscodingProperties;
    private final ModelConverters modelConverters;
    private final Map</*service*/ String, Map</*method name*/ String, Method>> methodMap;

    public GrpcTranscodingOpenApiCustomizer(
            List<BindableService> services,
            GrpcTranscodingProperties grpcTranscodingProperties,
            SpringDocConfigProperties springDocConfigProperties,
            ObjectMapperProvider objectMapperProvider) {
        this.serviceDefinitions =
                services.stream().map(BindableService::bindService).toList();
        this.grpcTranscodingProperties = grpcTranscodingProperties;
        this.modelConverters = buildModelConverters(springDocConfigProperties, objectMapperProvider);
        this.methodMap = buildMethodMap(services);
    }

    @Override
    public void customise(OpenAPI openApi) {

        initializeOpenApiComponents(openApi);

        for (var sd : serviceDefinitions) {
            var descriptor = mustGetServiceDescriptor(sd);
            if (descriptor == null) {
                continue;
            }

            for (var md : descriptor.getMethods()) {
                if (md.isClientStreaming() || md.isServerStreaming()) {
                    continue;
                }

                HttpRule httpRule = extractHttpRule(md);
                if (httpRule == null) {
                    continue;
                }

                String path = getPath(httpRule);
                if (path == null) {
                    continue;
                }

                PathItem pathItem = openApi.getPaths().getOrDefault(path, new PathItem());

                Operation operation = createOperation(md);

                PathTemplate pathTemplate = PathTemplate.create(path);
                Set<String> pathVars = new HashSet<>(pathTemplate.vars());

                // Handle path parameters
                addPathParameters(operation, pathVars);

                // Handle query parameters
                addQueryParameters(operation, httpRule, md, pathVars);

                // Handle request body
                handleRequestBody(operation, httpRule, md);

                // Handle responses
                handleResponses(operation, md);

                // Assign operation to HTTP method
                assignOperationToMethod(pathItem, operation, httpRule);

                // Add PathItem to OpenAPI Paths
                openApi.getPaths().addPathItem(path, pathItem);

                // Add schemas to OpenAPI Components
                addSchemas(openApi, md);
            }
        }
    }

    private static void initializeOpenApiComponents(OpenAPI openApi) {
        if (openApi.getPaths() == null) {
            openApi.setPaths(new Paths());
        }
        if (openApi.getComponents() == null) {
            openApi.setComponents(new Components());
        }
    }

    @Nullable
    private HttpRule extractHttpRule(Descriptors.MethodDescriptor md) {
        HttpRule httpRule = null;

        if (md.getOptions().hasExtension(AnnotationsProto.http)) {
            httpRule = md.getOptions().getExtension(AnnotationsProto.http);
        }

        if (httpRule == null && grpcTranscodingProperties.isAutoMapping()) {
            String path = "/" + md.getService().getFullName() + "/" + md.getName();
            httpRule = HttpRule.newBuilder().setPost(path).setBody("*").build();
        }

        return httpRule;
    }

    private static Operation createOperation(Descriptors.MethodDescriptor md) {
        return new Operation().operationId(md.getName());
    }

    private static void addPathParameters(Operation operation, Set<String> pathVars) {
        for (String var : pathVars) {
            Parameter parameter =
                    new Parameter().in("path").name(var).required(true).schema(new StringSchema());
            operation.addParametersItem(parameter);
        }
    }

    private void addQueryParameters(
            Operation operation, HttpRule httpRule, Descriptors.MethodDescriptor md, Set<String> pathVars) {
        if (!httpRule.hasGet()) { // Only add query parameters for GET requests
            return;
        }

        Class<?> requestClass = mustGetRequestClass(md);
        if (requestClass == null) {
            return;
        }

        var schemaMap = modelConverters.read(requestClass);
        if (schemaMap == null || schemaMap.isEmpty()) {
            return;
        }

        Schema<?> requestSchema = schemaMap.values().iterator().next();
        var properties = requestSchema.getProperties();
        if (properties == null) {
            return;
        }

        pathVars = pathVars.stream()
                .map(e -> e.replace("_", ""))
                .collect(Collectors.toCollection(() -> new TreeSet<>(String.CASE_INSENSITIVE_ORDER)));

        for (var entry : properties.entrySet()) {
            String property = entry.getKey();
            Schema<?> schema = entry.getValue();

            if (pathVars.contains(property)) { // Skip path variables
                continue;
            }

            boolean isRequired = requestSchema.getRequired() != null
                    && requestSchema.getRequired().contains(property);
            Parameter parameter = new Parameter()
                    .in("query")
                    .name(property)
                    .required(isRequired)
                    .schema(schema);
            operation.addParametersItem(parameter);
        }
    }

    private static void handleRequestBody(Operation operation, HttpRule httpRule, Descriptors.MethodDescriptor md) {
        if (Objects.equals(httpRule.getBody(), "*")) {
            String inputTypeFullName = md.getInputType().getFullName();
            RequestBody requestBody = new RequestBody()
                    .required(true)
                    .content(new Content()
                            .addMediaType(
                                    "application/json",
                                    new MediaType()
                                            .schema(new Schema<>().$ref("#/components/schemas/" + inputTypeFullName))));
            operation.setRequestBody(requestBody);
        } else if (!httpRule.getBody().isBlank()) {
            md.getInputType().getFields().stream()
                    .filter(field -> Objects.equals(field.getName(), httpRule.getBody()))
                    .findFirst()
                    .ifPresent(field -> {
                        String inputTypeFullName = field.getMessageType().getFullName();
                        RequestBody requestBody = new RequestBody()
                                .required(!field.toProto().getProto3Optional()) // specified property is optional
                                .content(new Content()
                                        .addMediaType(
                                                "application/json",
                                                new MediaType()
                                                        .schema(new Schema<>()
                                                                .$ref("#/components/schemas/" + inputTypeFullName))));
                        operation.setRequestBody(requestBody);
                    });
        }
    }

    private static void handleResponses(Operation operation, Descriptors.MethodDescriptor md) {
        String outputTypeFullName = md.getOutputType().getFullName();
        ApiResponse apiResponse = new ApiResponse()
                .content(new Content()
                        .addMediaType(
                                "application/json",
                                new MediaType()
                                        .schema(new Schema<>().$ref("#/components/schemas/" + outputTypeFullName))));
        ApiResponses apiResponses = new ApiResponses();
        apiResponses.addApiResponse("200", apiResponse);
        operation.setResponses(apiResponses);
    }

    private static void assignOperationToMethod(PathItem pathItem, Operation operation, HttpRule httpRule) {
        switch (httpRule.getPatternCase()) {
            case GET -> pathItem.setGet(operation);
            case PUT -> pathItem.setPut(operation);
            case POST -> pathItem.setPost(operation);
            case DELETE -> pathItem.setDelete(operation);
            case PATCH -> pathItem.setPatch(operation);
            default -> log.warn("Unhandled HTTP pattern case: {}", httpRule);
        }
    }

    /**
     * Adds the necessary schemas for request and response to OpenAPI Components.
     *
     * @param openApi The OpenAPI instance
     * @param md      MethodDescriptor
     */
    private void addSchemas(OpenAPI openApi, Descriptors.MethodDescriptor md) {
        addSchema(openApi, mustGetRequestClass(md));
        addSchema(openApi, mustGetResponseClass(md));
    }

    /**
     * Adds a single schema to OpenAPI Components.
     *
     * @param openApi The OpenAPI instance
     * @param clazz   The Java Class to convert
     */
    private void addSchema(OpenAPI openApi, Class<?> clazz) {
        if (clazz == null) {
            return;
        }

        var schemas = modelConverters.readAll(clazz);
        if (schemas == null || schemas.isEmpty()) {
            return;
        }

        Components components = openApi.getComponents();
        if (components == null) {
            components = new Components();
            openApi.setComponents(components);
        }

        for (var entry : schemas.entrySet()) {
            String schemaName = entry.getKey();
            Schema<?> schema = entry.getValue();
            components.addSchemas(schemaName, schema);
        }
    }

    private static ModelConverters buildModelConverters(
            SpringDocConfigProperties springDocConfigProperties, ObjectMapperProvider objectMapperProvider) {
        var result = ModelConverters.getInstance(springDocConfigProperties.isOpenapi31());

        var mapper = objectMapperProvider.jsonMapper();
        mapper.registerModules(new ProtobufModule(), new ProtobufPropertiesModule());

        result.addConverter(new ModelResolver(mapper, new ProtobufTypeNameResolver()));
        result.addConverter(new ProtobufModelConverter(objectMapperProvider));

        return result;
    }

    private static Map<String, Map<String, Method>> buildMethodMap(List<BindableService> services) {
        Map<String, Map<String, Method>> map = new HashMap<>();

        for (BindableService service : services) {
            var serviceDefinition = service.bindService();
            var serviceName = serviceDefinition.getServiceDescriptor().getName();

            Map<String, Method> methods = new HashMap<>();
            for (var methodDef : serviceDefinition.getMethods()) {
                String methodName = methodDef.getMethodDescriptor().getBareMethodName();
                if (methodName == null) {
                    continue;
                }
                var method = ReflectionUtils.findMethod(
                        service.getClass(), StringUtils.uncapitalize(methodName), (Class<?>[]) null);
                if (method != null) {
                    methods.put(methodName, method);
                }
            }

            map.put(serviceName, methods);
        }

        return map;
    }

    private static String getPath(HttpRule httpRule) {
        return switch (httpRule.getPatternCase()) {
            case GET -> httpRule.getGet();
            case PUT -> httpRule.getPut();
            case POST -> httpRule.getPost();
            case DELETE -> httpRule.getDelete();
            case PATCH -> httpRule.getPatch();
            case CUSTOM -> httpRule.getCustom().getKind();
            default -> null;
        };
    }

    private static Descriptors.ServiceDescriptor mustGetServiceDescriptor(ServerServiceDefinition definition) {
        Object schemaDescriptor = definition.getServiceDescriptor().getSchemaDescriptor();
        if (schemaDescriptor instanceof ProtoFileDescriptorSupplier protoFileDescriptorSupplier) {
            Descriptors.FileDescriptor fileDescriptor = protoFileDescriptorSupplier.getFileDescriptor();
            String serviceName = definition.getServiceDescriptor().getName();
            return fileDescriptor.getServices().stream()
                    .filter(serviceDescriptor -> serviceDescriptor.getFullName().equals(serviceName))
                    .findFirst()
                    .orElseThrow();
        }
        throw new IllegalStateException("Service descriptor not found");
    }

    private Class<?> mustGetRequestClass(Descriptors.MethodDescriptor methodDescriptor) {
        String serviceName = methodDescriptor.getService().getFullName();
        String methodName = methodDescriptor.getName();
        Method method = methodMap.getOrDefault(serviceName, Map.of()).get(methodName);
        if (method != null && method.getParameterCount() >= 1) {
            return method.getParameterTypes()[0];
        }
        throw new IllegalStateException("Request class not found for method: " + methodName);
    }

    private Class<?> mustGetResponseClass(Descriptors.MethodDescriptor methodDescriptor) {
        String serviceName = methodDescriptor.getService().getFullName();
        String methodName = methodDescriptor.getName();
        Method method = methodMap.getOrDefault(serviceName, Map.of()).get(methodName);
        if (method != null && method.getParameterCount() >= 2) {
            Type secondParamType = method.getGenericParameterTypes()[1];
            if (secondParamType instanceof ParameterizedType pType) {
                Type[] typeArgs = pType.getActualTypeArguments();
                if (typeArgs.length == 1 && typeArgs[0] instanceof Class<?> respClass) {
                    return respClass;
                }
            }
        }
        throw new IllegalStateException("Response class not found for method " + methodName);
    }
}
