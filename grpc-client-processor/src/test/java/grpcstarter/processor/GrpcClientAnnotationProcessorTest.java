package grpcstarter.processor;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.tools.JavaFileObject;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link GrpcClientAnnotationProcessor} using Google's compile-testing library.
 *
 * <p>Note: These tests verify that the annotation processor runs correctly during compilation.
 * The processor may not find stub classes in the test environment because Spring's
 * ClassPathScanningCandidateComponentProvider requires classes to be on the classpath,
 * which doesn't happen during compile-testing's in-memory compilation.
 *
 * @author Freeman
 */
class GrpcClientAnnotationProcessorTest {

    /**
     * Get the classpath for the compilation, including the test runtime classpath.
     */
    private List<File> getClasspath() {
        List<File> classpath = new ArrayList<>();
        String classpathProperty = System.getProperty("java.class.path");
        if (classpathProperty != null) {
            for (String path : classpathProperty.split(File.pathSeparator)) {
                classpath.add(new File(path));
            }
        }
        return classpath;
    }

    @Test
    void testAnnotationProcessorRuns() {
        // Create a test source file with @GenerateGrpcClients annotation
        JavaFileObject markerClass = JavaFileObjects.forSourceString(
                "com.example.GrpcClientMarker",
                """
                package com.example;

                import grpcstarter.processor.GenerateGrpcClients;

                @GenerateGrpcClients(
                    basePackages = {"io.grpc"}
                )
                public class GrpcClientMarker {
                }
                """);

        // Compile with the annotation processor and test classpath
        Compilation compilation = javac().withProcessors(new GrpcClientAnnotationProcessor())
                .withClasspath(getClasspath())
                .compile(markerClass);

        // Verify compilation succeeded (may have warnings about no stubs found)
        assertThat(compilation).succeeded();

        // Verify the annotation processor ran and printed messages
        assertThat(compilation).hadNoteContaining("Generating gRPC client configuration");
    }

    @Test
    void testAnnotationProcessorWithCustomAuthority() {
        JavaFileObject markerClass = JavaFileObjects.forSourceString(
                "com.example.GrpcClientMarker",
                """
                package com.example;

                import grpcstarter.processor.GenerateGrpcClients;

                @GenerateGrpcClients(
                    basePackages = {"com.example"},
                    authority = "production.example.com:443",
                    configurationName = "GrpcClientConfiguration"
                )
                public class GrpcClientMarker {
                }
                """);

        Compilation compilation = javac().withProcessors(new GrpcClientAnnotationProcessor())
                .withClasspath(getClasspath())
                .compile(markerClass);

        assertThat(compilation).succeeded();
        assertThat(compilation).hadNoteContaining("Generating gRPC client configuration");
    }

    @Test
    void testAnnotationProcessorWithNoStubs() {
        JavaFileObject markerClass = JavaFileObjects.forSourceString(
                "com.example.GrpcClientMarker",
                """
                package com.example;

                import grpcstarter.processor.GenerateGrpcClients;

                @GenerateGrpcClients(
                    basePackages = {"com.nonexistent"},
                    authority = "localhost:9090",
                    configurationName = "GrpcClientConfiguration"
                )
                public class GrpcClientMarker {
                }
                """);

        Compilation compilation = javac().withProcessors(new GrpcClientAnnotationProcessor())
                .withClasspath(getClasspath())
                .compile(markerClass);

        // Should succeed but with a warning about no stubs found
        assertThat(compilation).succeeded();
        assertThat(compilation).hadWarningContaining("No gRPC client stubs found");
    }

    @Test
    void testAnnotationProcessorWithCustomConfigurationName() {
        JavaFileObject markerClass = JavaFileObjects.forSourceString(
                "com.example.GrpcClientMarker",
                """
                package com.example;

                import grpcstarter.processor.GenerateGrpcClients;

                @GenerateGrpcClients(
                    basePackages = {"com.example"},
                    authority = "localhost:9090",
                    configurationName = "CustomGrpcConfig"
                )
                public class GrpcClientMarker {
                }
                """);

        Compilation compilation = javac().withProcessors(new GrpcClientAnnotationProcessor())
                .withClasspath(getClasspath())
                .compile(markerClass);

        assertThat(compilation).succeeded();
        assertThat(compilation).hadNoteContaining("Generating gRPC client configuration");
    }

    @Test
    void testAnnotationProcessorWithMultiplePackages() {
        JavaFileObject markerClass = JavaFileObjects.forSourceString(
                "com.example.GrpcClientMarker",
                """
                package com.example;

                import grpcstarter.processor.GenerateGrpcClients;

                @GenerateGrpcClients(
                    basePackages = {"com.example.user", "com.example.order", "com.example.product"},
                    authority = "localhost:9090",
                    configurationName = "GrpcClientConfiguration"
                )
                public class GrpcClientMarker {
                }
                """);

        Compilation compilation = javac().withProcessors(new GrpcClientAnnotationProcessor())
                .withClasspath(getClasspath())
                .compile(markerClass);

        assertThat(compilation).succeeded();
        assertThat(compilation).hadNoteContaining("Generating gRPC client configuration");
    }

    @Test
    void testAnnotationProcessorWithEmptyPackages() {
        JavaFileObject markerClass = JavaFileObjects.forSourceString(
                "com.example.GrpcClientMarker",
                """
                package com.example;

                import grpcstarter.processor.GenerateGrpcClients;

                @GenerateGrpcClients(
                    basePackages = {},
                    authority = "localhost:9090",
                    configurationName = "GrpcClientConfiguration"
                )
                public class GrpcClientMarker {
                }
                """);

        Compilation compilation = javac().withProcessors(new GrpcClientAnnotationProcessor())
                .withClasspath(getClasspath())
                .compile(markerClass);

        assertThat(compilation).succeeded();
        assertThat(compilation).hadNoteContaining("Generating gRPC client configuration");
    }
}
