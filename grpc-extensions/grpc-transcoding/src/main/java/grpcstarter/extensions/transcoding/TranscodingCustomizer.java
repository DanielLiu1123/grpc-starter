package grpcstarter.extensions.transcoding;

import com.google.api.HttpRule;
import com.google.protobuf.Descriptors;

/**
 * Extension interface for customizing gRPC-HTTP transcoding rules.
 *
 * <p>This interface allows modification of {@link HttpRule} configurations for gRPC methods
 * during the transcoding setup process. It can be used to dynamically adjust HTTP routing rules,
 * such as adding path prefixes, changing HTTP methods, or modifying request/response body mappings.
 *
 * <p>Customizers are applied to both:
 * <ul>
 *   <li>Manually defined HTTP rules (via protobuf {@code google.api.http} annotations)</li>
 *   <li>Auto-mapped rules (when auto-mapping is enabled)</li>
 * </ul>
 *
 * <h3>Example: Adding a prefix to a specific service</h3>
 * <pre>{@code
 * @Bean
 * public TranscodingCustomizer userServicePrefixCustomizer() {
 *     return (httpRule, descriptor) -> {
 *         // Only apply to methods in the UserService
 *         if (!"example.UserService".equals(descriptor.getService().getFullName())) {
 *             return httpRule;
 *         }
 *
 *         // Add /api/v1 prefix to all HTTP paths
 *         String prefix = "/api/v1";
 *         HttpRule.Builder builder = httpRule.toBuilder();
 *
 *         switch (httpRule.getPatternCase()) {
 *             case GET -> builder.setGet(prefix + httpRule.getGet());
 *             case POST -> builder.setPost(prefix + httpRule.getPost());
 *             case PUT -> builder.setPut(prefix + httpRule.getPut());
 *             case DELETE -> builder.setDelete(prefix + httpRule.getDelete());
 *             case PATCH -> builder.setPatch(prefix + httpRule.getPatch());
 *         }
 *
 *         return builder.build();
 *     };
 * }
 * }</pre>
 *
 * @author Freeman
 * @since 4.0.0
 */
public interface TranscodingCustomizer {

    /**
     * Customize the HTTP rule for a gRPC method.
     *
     * <p>This method is called during the transcoding setup for each gRPC method,
     * allowing you to modify the HTTP routing configuration.
     *
     * @param httpRule the original HTTP rule
     * @param descriptor the gRPC method descriptor containing metadata about the method
     * @return the customized HTTP rule
     */
    HttpRule customize(HttpRule httpRule, Descriptors.MethodDescriptor descriptor);
}
