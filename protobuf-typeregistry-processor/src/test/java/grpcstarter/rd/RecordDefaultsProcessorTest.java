package grpcstarter.rd;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import org.junit.jupiter.api.Test;

/**
 * Test for RecordDefaultsProcessor.
 *
 * @author Freeman
 */
class RecordDefaultsProcessorTest {

    @Test
    void shouldGenerateDefaultsClassForSimpleRecord() {
        var testRecord = JavaFileObjects.forSourceString(
                "test.SimpleRecord",
                """
                package test;

                public record SimpleRecord(String name, Integer age) {}
                """);

        Compilation compilation =
                javac().withProcessors(new RecordDefaultsProcessor()).compile(testRecord);

        assertThat(compilation).succeeded();

        // Verify that a source file was generated
        assertThat(compilation.generatedSourceFiles()).hasSize(1);

        // Verify the generated source contains the expected DEFAULT field
        var generatedFile = compilation.generatedSourceFile("test.SimpleRecordDefaults");
        assertThat(generatedFile).isPresent();

        try {
            String generatedSource = generatedFile.get().getCharContent(false).toString();
            assertThat(generatedSource)
                    .contains("public static final SimpleRecord DEFAULT = new SimpleRecord(null, null);");
        } catch (Exception e) {
            throw new RuntimeException("Failed to read generated source", e);
        }
    }

    @Test
    void shouldGenerateDefaultsClassForComplexRecord() {
        var testRecord = JavaFileObjects.forSourceString(
                "test.ComplexRecord",
                """
                package test;

                import java.util.List;
                import java.util.Map;

                public record ComplexRecord(
                    String id,
                    List<String> items,
                    Map<String, Object> metadata,
                    boolean active
                ) {}
                """);

        Compilation compilation =
                javac().withProcessors(new RecordDefaultsProcessor()).compile(testRecord);

        assertThat(compilation).succeeded();

        // Verify that a source file was generated
        assertThat(compilation.generatedSourceFiles()).hasSize(1);

        // Verify the generated source contains the expected DEFAULT field with proper default values
        var generatedFile = compilation.generatedSourceFile("test.ComplexRecordDefaults");
        assertThat(generatedFile).isPresent();

        try {
            String generatedSource = generatedFile.get().getCharContent(false).toString();
            assertThat(generatedSource)
                    .contains(
                            "public static final ComplexRecord DEFAULT = new ComplexRecord(null, null, null, false);");
        } catch (Exception e) {
            throw new RuntimeException("Failed to read generated source", e);
        }
    }

    @Test
    void shouldNotProcessNonRecordClasses() {
        var testClass = JavaFileObjects.forSourceString(
                "test.RegularClass",
                """
                package test;

                public class RegularClass {
                    private String name;

                    public RegularClass(String name) {
                        this.name = name;
                    }
                }
                """);

        Compilation compilation =
                javac().withProcessors(new RecordDefaultsProcessor()).compile(testClass);

        assertThat(compilation).succeeded();
        // Should not generate any files for non-record classes
        assertThat(compilation.generatedSourceFiles()).isEmpty();
    }
}
