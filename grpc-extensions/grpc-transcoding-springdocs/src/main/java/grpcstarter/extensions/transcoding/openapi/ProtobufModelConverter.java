package grpcstarter.extensions.transcoding.openapi;

import static grpcstarter.extensions.transcoding.openapi.ProtobufTypeNameResolver.getDescriptor;

import com.fasterxml.jackson.databind.JavaType;
import com.google.protobuf.ProtocolMessageEnum;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.oas.models.media.Schema;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Objects;
import org.springdoc.core.providers.ObjectMapperProvider;

/**
 * @author Freeman
 */
@SuppressWarnings("rawtypes")
class ProtobufModelConverter implements ModelConverter {

    private final ObjectMapperProvider springDocObjectMapper;

    public ProtobufModelConverter(ObjectMapperProvider springDocObjectMapper) {
        this.springDocObjectMapper = springDocObjectMapper;
    }

    @Override
    public Schema resolve(AnnotatedType type, ModelConverterContext context, Iterator<ModelConverter> chain) {
        if (chain.hasNext()) {
            var schema = chain.next().resolve(type, context, chain);
            var javaType = springDocObjectMapper.jsonMapper().constructType(type.getType());
            if (schema == null || javaType == null) {
                return schema;
            }

            // Protobuf generate a UNRECOGNIZED enum value, should remove it
            removeUnrecognizedEnum(schema, javaType);

            // Set required parameter
            setRequiredParameter(schema, javaType);

            return schema;
        }
        return null;
    }

    private static void setRequiredParameter(Schema<?> schema, JavaType javaType) {
        var descriptor = getDescriptor(javaType.getRawClass());
        if (descriptor == null) {
            return;
        }

        var requiredFields = new ArrayList<String>();
        for (var field : descriptor.getFields()) {
            if (!field.toProto().getProto3Optional()) {
                requiredFields.add(field.getJsonName());
            }
        }

        schema.setRequired(requiredFields);
    }

    @SuppressWarnings("unchecked")
    private static void removeUnrecognizedEnum(Schema schema, JavaType javaType) {
        if (ProtocolMessageEnum.class.isAssignableFrom(javaType.getRawClass())
                && Enum.class.isAssignableFrom(javaType.getRawClass())
                && Objects.equals(schema.getType(), "string")
                && schema.getEnum() != null) {
            var newList = new ArrayList<>(schema.getEnum());
            newList.removeIf("UNRECOGNIZED"::equals);
            schema.setEnum(newList);
        }
    }
}
