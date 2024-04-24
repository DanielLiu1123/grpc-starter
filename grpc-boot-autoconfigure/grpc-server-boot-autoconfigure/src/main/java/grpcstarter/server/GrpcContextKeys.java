package grpcstarter.server;

import io.grpc.Context;
import io.grpc.Metadata;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.function.Consumer;
import lombok.Data;
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
    @Data
    public static class ResponseMetadataModifier {
        static final Context.Key<ResponseMetadataModifier> INSTANCE =
                Context.key("GrpcContextKeys.ResponseMetadataModifier");

        @Nonnull
        private final ArrayList<Consumer<Metadata>> consumers = new ArrayList<>();

        /**
         * Get {@link ResponseMetadataModifier} bound to current gRPC request.
         *
         * @return {@link ResponseMetadataModifier} bound to current gRPC request
         */
        @Nullable
        public static ResponseMetadataModifier get() {
            return INSTANCE.get();
        }

        /**
         * Add a consumer to modify response headers/trailers.
         *
         * @param consumer {@link Consumer} to modify response headers/trailers
         */
        public static void addConsumers(Consumer<Metadata> consumer) {
            ResponseMetadataModifier key = get();
            if (key != null) {
                key.getConsumers().add(consumer);
            }
        }
    }
}
