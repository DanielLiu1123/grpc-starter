package grpcstarter.server;

import io.grpc.Context;
import io.grpc.Metadata;
import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import lombok.experimental.UtilityClass;

/**
 * @author Freeman
 * @since 3.2.3
 */
@UtilityClass
public class GrpcContextKeys {

    /**
     * Modify gRPC response metadata, e.g. add custom headers/trailers.
     */
    public static class ResponseMetadataModifier {
        static final Context.Key<ResponseMetadataModifier> INSTANCE =
                Context.key("GrpcContextKeys.ResponseMetadataModifier");

        final List<Consumer<Metadata>> consumers = Collections.synchronizedList(new ArrayList<>());

        /**
         * Get {@link ResponseMetadataModifier} bound to current gRPC request.
         *
         * @return {@link ResponseMetadataModifier} bound to current gRPC request
         */
        @Nullable
        static ResponseMetadataModifier get() {
            return INSTANCE.get();
        }

        /**
         * Add a consumer to modify response headers/trailers.
         *
         * <p> This method is thread-safe.
         *
         * @param consumer {@link Consumer} to modify response headers/trailers
         */
        public static void addConsumer(Consumer<Metadata> consumer) {
            ResponseMetadataModifier key = get();
            if (key != null) {
                key.consumers.add(consumer);
            }
        }
    }
}
