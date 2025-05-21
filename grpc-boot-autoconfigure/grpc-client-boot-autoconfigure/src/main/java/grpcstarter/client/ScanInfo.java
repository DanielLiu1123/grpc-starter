package grpcstarter.client;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.annotation.Nullable;
import java.util.LinkedHashSet;

/**
 * @author Freeman
 */
// https://spotbugs.readthedocs.io/en/stable/bugDescriptions.html#pa-primitive-field-is-public-pa-public-primitive-attribute
@SuppressFBWarnings("PA_PUBLIC_PRIMITIVE_ATTRIBUTE")
final class ScanInfo {
    public final LinkedHashSet<String> basePackages = new LinkedHashSet<>();
    public final LinkedHashSet<Class<?>> clients = new LinkedHashSet<>();

    @Nullable
    public Class<? extends GrpcClientBeanDefinitionHandler> beanDefinitionHandler;

    public void clear() {
        basePackages.clear();
        clients.clear();
        beanDefinitionHandler = null;
    }
}
