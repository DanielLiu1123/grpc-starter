package grpcstarter.client.exception;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

/**
 * gRPC client failure analyzer.
 *
 * @author Freeman
 */
public class GrpcClientFailureAnalyzer extends AbstractFailureAnalyzer<MissingChannelConfigurationException> {

    @Override
    protected FailureAnalysis analyze(Throwable rootFailure, MissingChannelConfigurationException cause) {
        return new FailureAnalysis(
                cause.getMessage(),
                "Please configure the gRPC channel authority for the stub: "
                        + cause.getStubClass().getName() + "\n\n"
                        + "You can configure the channel authority in application.yml:\n\n"
                        + "Configure channel authority globally:\n"
                        + "grpc:\n"
                        + "  client:\n"
                        + "    authority: localhost:9090\n\n"
                        + "Or configure channel authority for the specific stub:\n"
                        + "grpc:\n"
                        + "  client:\n"
                        + "    channels:\n"
                        + "      - authority: localhost:9090\n"
                        + "        classes: " + cause.getStubClass().getCanonicalName(),
                cause);
    }
}
