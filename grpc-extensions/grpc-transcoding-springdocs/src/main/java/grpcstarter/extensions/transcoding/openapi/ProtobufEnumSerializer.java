package grpcstarter.extensions.transcoding.openapi;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.protobuf.ProtocolMessageEnum;
import java.io.IOException;

/**
 * Protobuf enum serializer, use {@link ProtocolMessageEnum#getNumber()} to serialize protobuf enum.
 *
 * @param <T> protobuf enum type
 */
final class ProtobufEnumSerializer<T extends Enum<T> & ProtocolMessageEnum> extends JsonSerializer<T> {

    @Override
    public void serialize(T value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeNumber(value.getNumber());
    }
}
