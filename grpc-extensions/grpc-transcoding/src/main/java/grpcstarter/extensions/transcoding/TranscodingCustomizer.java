package grpcstarter.extensions.transcoding;

import com.google.api.HttpRule;
import com.google.protobuf.Descriptors;

/**
 * Customization for gRPC-HTTP transcoding.
 *
 * @author Freeman
 * @since 4.0.0
 */
public interface TranscodingCustomizer {
    HttpRule customize(Descriptors.MethodDescriptor descriptor, HttpRule httpRule);
}
