package grpcstarter.client;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.LinkedHashSet;
import org.jspecify.annotations.Nullable;

/**
 * @author Freeman
 */
// https://spotbugs.readthedocs.io/en/stable/bugDescriptions.html#pa-primitive-field-is-public-pa-public-primitive-attribute
@SuppressFBWarnings("PA_PUBLIC_PRIMITIVE_ATTRIBUTE")
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
