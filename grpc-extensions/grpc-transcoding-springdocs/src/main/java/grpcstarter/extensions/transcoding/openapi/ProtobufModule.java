package grpcstarter.extensions.transcoding.openapi;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.deser.Deserializers;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.Serializers;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.ProtocolMessageEnum;

final class ProtobufModule extends SimpleModule {
    @Override
    public void setupModule(SetupContext context) {
        context.addSerializers(new Serializers.Base() {
            @Override
            public JsonSerializer<?> findSerializer(
                    SerializationConfig config, JavaType type, BeanDescription beanDesc) {
                if (MessageOrBuilder.class.isAssignableFrom(type.getRawClass())) {
                    return new ProtobufMessageSerializer<>();
                }
                if (ProtocolMessageEnum.class.isAssignableFrom(type.getRawClass()) && type.isEnumType()) {
                    return new ProtobufEnumSerializer<>();
                }
                return super.findSerializer(config, type, beanDesc);
            }
        });
        context.addDeserializers(new Deserializers.Base() {
            @Override
            @SuppressWarnings({"rawtypes", "unchecked"})
            public JsonDeserializer<?> findEnumDeserializer(
                    Class<?> type, DeserializationConfig config, BeanDescription beanDesc) throws JsonMappingException {
                if (ProtocolMessageEnum.class.isAssignableFrom(type) && type.isEnum()) {
                    return new ProtobufEnumDeserializer(type);
                }
                return super.findEnumDeserializer(type, config, beanDesc);
            }

            @Override
            @SuppressWarnings({"rawtypes", "unchecked"})
            public JsonDeserializer<?> findBeanDeserializer(
                    JavaType type, DeserializationConfig config, BeanDescription beanDesc) throws JsonMappingException {
                if (MessageOrBuilder.class.isAssignableFrom(type.getRawClass())) {
                    return new ProtobufMessageDeserializer(type.getRawClass());
                }
                return super.findBeanDeserializer(type, config, beanDesc);
            }
        });
    }
}
