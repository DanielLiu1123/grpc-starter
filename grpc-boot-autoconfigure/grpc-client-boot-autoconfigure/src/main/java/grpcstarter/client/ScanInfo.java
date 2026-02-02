package grpcstarter.client;

import java.util.LinkedHashSet;
import org.jspecify.annotations.Nullable;

/**
 * @author Freeman
 */
final class ScanInfo {
    public final LinkedHashSet<String> basePackages = new LinkedHashSet<>();
    public final LinkedHashSet<Class<?>> clients = new LinkedHashSet<>();

    public @Nullable Class<? extends GrpcClientBeanDefinitionHandler> beanDefinitionHandler;

    public void clear() {
        basePackages.clear();
        clients.clear();
        beanDefinitionHandler = null;
    }
}
