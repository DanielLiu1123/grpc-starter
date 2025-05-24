package grpcstarter.extensions.transcoding.openapi;

import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import io.swagger.v3.core.jackson.TypeNameResolver;
import org.springframework.util.ReflectionUtils;

/**
 * @author Freeman
 */
class ProtobufTypeNameResolver extends TypeNameResolver {
    @Override
    protected String getNameOfClass(Class<?> cls) {
        if (Message.class.isAssignableFrom(cls)) {
            var desc = getDescriptor(cls);
            if (desc != null) {
                return desc.getFullName();
            }
        }
        return super.getNameOfClass(cls);
    }

    static Descriptors.Descriptor getDescriptor(Class<?> cls) {
        if (Message.class.isAssignableFrom(cls)) {
            var m = ReflectionUtils.findMethod(cls, "getDescriptor");
            if (m != null) {
                var result = ReflectionUtils.invokeMethod(m, null);
                if (result instanceof Descriptors.Descriptor desc) {
                    return desc;
                }
            }
        }
        return null;
    }
}
