package grpcstarter.extensions.transcoding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.protobuf.Descriptors;
import com.google.protobuf.ProtocolStringList;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import org.junit.jupiter.api.Test;
import transcoding.TranscoderTest;

/**
 * Test class for {@link ProtobufJavaTypeUtil}.
 *
 * @author Freeman
 */
class ProtobufJavaTypeUtilTest {

    @Test
    void testFindJavaClass_SimpleMessage() {
        // Given
        Descriptors.Descriptor descriptor = TranscoderTest.SimpleRequest.getDescriptor();

        // When
        Class<?> javaClass = ProtobufJavaTypeUtil.findJavaClass(descriptor);

        // Then
        assertThat(javaClass).isEqualTo(TranscoderTest.SimpleRequest.class);
    }

    @Test
    void testFindJavaClass_NestedMessage() {
        // Given
        Descriptors.Descriptor descriptor = TranscoderTest.UseSubMessageRequestRpcRequest.SubMessage.getDescriptor();

        // When
        Class<?> javaClass = ProtobufJavaTypeUtil.findJavaClass(descriptor);

        // Then
        assertThat(javaClass).isEqualTo(TranscoderTest.UseSubMessageRequestRpcRequest.SubMessage.class);
    }

    @Test
    void testFindJavaFieldType_StringField() {
        // Given
        Class<?> javaClass = TranscoderTest.SimpleRequest.class;
        Descriptors.FieldDescriptor fieldDescriptor =
                TranscoderTest.SimpleRequest.getDescriptor().findFieldByName("requestMessage");

        // When
        Type fieldType = ProtobufJavaTypeUtil.findJavaFieldType(javaClass, fieldDescriptor);

        // Then
        assertThat(fieldType).isEqualTo(String.class);
    }

    @Test
    void testFindJavaFieldType_StringFieldWithUnderscore() {
        // Given
        Class<?> javaClass = TranscoderTest.SimpleRequest.class;
        Descriptors.FieldDescriptor fieldDescriptor =
                TranscoderTest.SimpleRequest.getDescriptor().findFieldByName("some_message");

        // When
        Type fieldType = ProtobufJavaTypeUtil.findJavaFieldType(javaClass, fieldDescriptor);

        // Then
        assertThat(fieldType).isEqualTo(String.class);
    }

    @Test
    void testFindJavaFieldType_MessageField() {
        // Given
        Class<?> javaClass = TranscoderTest.SimpleRequest.class;
        Descriptors.FieldDescriptor fieldDescriptor =
                TranscoderTest.SimpleRequest.getDescriptor().findFieldByName("nested");

        // When
        Type fieldType = ProtobufJavaTypeUtil.findJavaFieldType(javaClass, fieldDescriptor);

        // Then
        assertThat(fieldType).isEqualTo(TranscoderTest.SimpleRequest.class);
    }

    @Test
    void testFindJavaFieldType_RepeatedStringField() {
        // Given
        Class<?> javaClass = TranscoderTest.SimpleRequest.class;
        Descriptors.FieldDescriptor fieldDescriptor =
                TranscoderTest.SimpleRequest.getDescriptor().findFieldByName("repeated_string");

        // When
        Type fieldType = ProtobufJavaTypeUtil.findJavaFieldType(javaClass, fieldDescriptor);

        // Then
        // For repeated string fields, we expect to get ProtocolStringList
        assertThat(fieldType).isEqualTo(ProtocolStringList.class);
    }

    @Test
    void testFindJavaFieldType_RepeatedMessageField() {
        // Given
        Class<?> javaClass = TranscoderTest.SimpleRequest.class;
        Descriptors.FieldDescriptor fieldDescriptor =
                TranscoderTest.SimpleRequest.getDescriptor().findFieldByName("repeated_message");

        // When
        Type fieldType = ProtobufJavaTypeUtil.findJavaFieldType(javaClass, fieldDescriptor);

        // Then
        // For repeated message fields, we expect to get a parameterized List type
        assertThat(fieldType).isInstanceOf(ParameterizedType.class);
        ParameterizedType parameterizedType = (ParameterizedType) fieldType;
        assertThat(parameterizedType.getRawType()).isEqualTo(List.class);
        assertThat(parameterizedType.getActualTypeArguments()[0]).isEqualTo(TranscoderTest.SimpleRequest.class);
    }

    @Test
    void testFindJavaFieldType_NonExistentField() {
        // Given
        Class<?> javaClass = TranscoderTest.SimpleRequest.class;
        // Create a field descriptor for a field that doesn't exist in SimpleRequest
        Descriptors.FieldDescriptor fieldDescriptor =
                TranscoderTest.UseSubMessageRequestRpcRequest.SubMessage.getDescriptor()
                        .findFieldByName("message"); // This field doesn't exist in SimpleRequest

        // When & Then
        assertThatThrownBy(() -> ProtobufJavaTypeUtil.findJavaFieldType(javaClass, fieldDescriptor))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void testCaching() {
        // Given
        Descriptors.Descriptor descriptor = TranscoderTest.SimpleRequest.getDescriptor();

        // When - call multiple times
        Class<?> result1 = ProtobufJavaTypeUtil.findJavaClass(descriptor);
        Class<?> result2 = ProtobufJavaTypeUtil.findJavaClass(descriptor);

        // Then - should return same instance (cached)
        assertThat(result1).isSameAs(result2);
        assertThat(result1).isEqualTo(TranscoderTest.SimpleRequest.class);
    }

    @Test
    void testFieldTypeCaching() {
        // Given
        Class<?> javaClass = TranscoderTest.SimpleRequest.class;
        Descriptors.FieldDescriptor fieldDescriptor =
                TranscoderTest.SimpleRequest.getDescriptor().findFieldByName("requestMessage");

        // When - call multiple times
        Type result1 = ProtobufJavaTypeUtil.findJavaFieldType(javaClass, fieldDescriptor);
        Type result2 = ProtobufJavaTypeUtil.findJavaFieldType(javaClass, fieldDescriptor);

        // Then - should return same instance (cached)
        assertThat(result1).isSameAs(result2);
        assertThat(result1).isEqualTo(String.class);
    }
}
