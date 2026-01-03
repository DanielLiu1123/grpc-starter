package grpcstarter.extensions.transcoding.openapi;

import static grpcstarter.extensions.transcoding.ProtobufJavaTypeUtil.findJavaClass;
import static grpcstarter.extensions.transcoding.ProtobufJavaTypeUtil.findJavaFieldType;

import com.fasterxml.jackson.databind.type.TypeFactory;
import com.google.api.AnnotationsProto;
import com.google.api.HttpRule;
import com.google.api.pathtemplate.PathTemplate;
import com.google.protobuf.Descriptors;
import grpcstarter.extensions.transcoding.GrpcTranscodingProperties;
import io.grpc.BindableService;
import io.grpc.ServerServiceDefinition;
import io.grpc.protobuf.ProtoFileDescriptorSupplier;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.core.converter.ModelConverterContextImpl;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.util.RefUtils;
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
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.properties.SpringDocConfigProperties;
import springdocbridge.protobuf.ProtobufNameResolver;
import springdocbridge.protobuf.SpringDocBridgeProtobufProperties;

/**
 * OpenAPI customizer for gRPC Transcoding.
 *
 * @author Freeman
 */
public class GrpcTranscodingOpenApiCustomizer implements OpenApiCustomizer {
    private static final Logger log = LoggerFactory.getLogger(GrpcTranscodingOpenApiCustomizer.class);

    private final List<ServerServiceDefinition> serviceDefinitions;
    private final GrpcTranscodingProperties grpcTranscodingProperties;
    private final ModelConverterContext modelConverterContext;
    private final ProtobufNameResolver protobufNameResolver;

    public GrpcTranscodingOpenApiCustomizer(
            List<BindableService> services,
            GrpcTranscodingProperties grpcTranscodingProperties,
            SpringDocConfigProperties springDocConfigProperties,
            SpringDocBridgeProtobufProperties springDocBridgeProtobufProperties) {
        this.serviceDefinitions =
                services.stream().map(BindableService::bindService).toList();
        this.grpcTranscodingProperties = grpcTranscodingProperties;
        this.modelConverterContext = new ModelConverterContextImpl(
                buildModelConverters(springDocConfigProperties).getConverters());
        this.protobufNameResolver = new ProtobufNameResolver(
                springDocBridgeProtobufProperties.getSchemaNamingStrategy(), springDocConfigProperties.isUseFqn());
    }

    @Override
    public void customise(OpenAPI openApi) {

        init(openApi);

        for (var sd : serviceDefinitions) {
            var descriptor = mustGetServiceDescriptor(sd);

            if (descriptor == null) {
                continue;
            }

            for (var rpcMethod : descriptor.getMethods()) {
                if (rpcMethod.isClientStreaming() || rpcMethod.isServerStreaming()) {
                    continue;
                }

                HttpRule httpRule = extractHttpRule(rpcMethod);
                if (httpRule == null) {
                    continue;
                }

                String path = getPath(httpRule);
                if (path == null) {
                    continue;
                }

                var paths = openApi.getPaths();
                PathItem pathItem = paths.getOrDefault(path, new PathItem());

                Operation operation = createOperation(rpcMethod);

                PathTemplate pathTemplate = PathTemplate.create(path);
                Set<String> pathVars = new HashSet<>(pathTemplate.vars());

                // Handle path parameters
                addPathParameters(operation, pathVars);

                // Handle query parameters
                addQueryParameters(operation, httpRule, rpcMethod, pathVars);

                // Handle request body
                handleRequestBody(operation, httpRule, rpcMethod);

                // Handle responses
                handleResponses(operation, rpcMethod);

                // Assign operation to HTTP method
                assignOperationToMethod(pathItem, operation, httpRule);

                // Add PathItem to OpenAPI Paths
                paths.addPathItem(path, pathItem);
            }
        }

        for (var en : modelConverterContext.getDefinedModels().entrySet()) {
            openApi.getComponents().addSchemas(en.getKey(), en.getValue());
        }
    }

    private static void init(OpenAPI openApi) {
        if (openApi.getPaths() == null) {
            openApi.setPaths(new Paths());
        }
        if (openApi.getComponents() == null) {
            openApi.setComponents(new Components());
        }
    }

    private @Nullable HttpRule extractHttpRule(Descriptors.MethodDescriptor md) {
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

    @SuppressWarnings("unchecked")
    private void addQueryParameters(
            Operation operation, HttpRule httpRule, Descriptors.MethodDescriptor md, Set<String> pathVars) {
        if (!httpRule.hasGet()) { // Only add query parameters for GET requests
            return;
        }

        Class<?> requestClass = findJavaClass(md.getInputType());
        if (requestClass == null) {
            return;
        }

        var requestSchema = modelConverterContext.resolve(new AnnotatedType(requestClass));
        var properties = (Map<String, Schema<?>>) requestSchema.getProperties();
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

    private void handleRequestBody(Operation operation, HttpRule httpRule, Descriptors.MethodDescriptor md) {
        if (Objects.equals(httpRule.getBody(), "*")) {

            Class<?> requestClass = findJavaClass(md.getInputType());
            var schemaName = getSchemaName(requestClass);
            Schema<?> schema = resolveSchema(schemaName, requestClass);

            RequestBody requestBody = new RequestBody()
                    .required(true)
                    .content(new Content().addMediaType("application/json", new MediaType().schema(schema)));
            operation.setRequestBody(requestBody);
        } else if (!httpRule.getBody().isBlank()) {
            md.getInputType().getFields().stream()
                    .filter(field -> Objects.equals(field.getName(), httpRule.getBody()))
                    .findFirst()
                    .ifPresent(field -> {
                        // assume the field is a message type
                        var fieldType = findJavaFieldType(findJavaClass(md.getInputType()), field);
                        var schemaName = getSchemaName(fieldType);
                        Schema<?> schema = resolveSchema(schemaName, fieldType);

                        RequestBody requestBody = new RequestBody()
                                .required(!field.toProto().getProto3Optional()) // specified property is optional
                                .content(
                                        new Content().addMediaType("application/json", new MediaType().schema(schema)));
                        operation.setRequestBody(requestBody);
                    });
        }
    }

    private void handleResponses(Operation operation, Descriptors.MethodDescriptor md) {
        Class<?> responseClass = findJavaClass(md.getOutputType());

        var schemaName = getSchemaName(responseClass);
        Schema<?> schema = resolveSchema(schemaName, responseClass);

        ApiResponse apiResponse = new ApiResponse()
                .content(new Content().addMediaType("application/json", new MediaType().schema(schema)));
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

    private static ModelConverters buildModelConverters(SpringDocConfigProperties springDocConfigProperties) {
        return ModelConverters.getInstance(springDocConfigProperties.isOpenapi31());
    }

    private static @Nullable String getPath(HttpRule httpRule) {
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

    private Schema<?> resolveSchema(String schemaName, Type type) {

        var ref = RefUtils.constructRef(schemaName);

        if (modelConverterContext.getDefinedModels().containsKey(schemaName)) {
            return new Schema<>().$ref(ref);
        }

        var schema = modelConverterContext.resolve(new AnnotatedType(type));
        if (schema.get$ref() != null) {
            return schema;
        }

        modelConverterContext.defineModel(schemaName, schema);

        return new Schema<>().$ref(ref);
    }

    private String getSchemaName(Type type) {
        var javaType = TypeFactory.defaultInstance().constructType(type);
        return protobufNameResolver.nameForType(javaType);
    }
}
