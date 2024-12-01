package grpcstarter.client;

import java.util.LinkedHashSet;

/**
 * @author Freeman
 */
final class ScanInfo {
    public final LinkedHashSet<String> basePackages = new LinkedHashSet<>();
    public final LinkedHashSet<Class<?>> clients = new LinkedHashSet<>();

    public void clear() {
        basePackages.clear();
        clients.clear();
    }
}
