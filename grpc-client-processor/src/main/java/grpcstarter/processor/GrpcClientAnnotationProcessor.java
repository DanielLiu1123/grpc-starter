package grpcstarter.processor;

import grpcstarter.client.GenerateGrpcClients;
import java.io.IOException;
import java.io.Writer;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import org.springframework.util.AntPathMatcher;

/**
 * Annotation processor for {@link GenerateGrpcClients}.
 *
 * <p>This processor scans for gRPC client stubs and generates a Spring Configuration
 * class that registers all found clients as beans.
 *
 * @author Freeman
 */
@SupportedAnnotationTypes("grpcstarter.client.GenerateGrpcClients")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public class GrpcClientAnnotationProcessor extends AbstractProcessor {

    private final AntPathMatcher matcher = new AntPathMatcher(".");

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (annotations.isEmpty()) {
            return false;
        }

        for (Element element : roundEnv.getElementsAnnotatedWith(GenerateGrpcClients.class)) {
            try {
                processAnnotation(element, roundEnv);
            } catch (Exception e) {
                processingEnv
                        .getMessager()
                        .printMessage(
                                Diagnostic.Kind.ERROR,
                                "Failed to process @GenerateGrpcClients: " + e.getMessage(),
                                element);
            }
        }

        return true;
    }

    private void processAnnotation(Element element, RoundEnvironment roundEnv) throws IOException {
        GenerateGrpcClients annotation = element.getAnnotation(GenerateGrpcClients.class);
        if (annotation == null) {
            return;
        }

        String[] basePackages = annotation.basePackages();
        String authority = annotation.authority();
        String configurationName = annotation.configurationName();

        // Get package name from the annotated element
        String packageName = processingEnv
                .getElementUtils()
                .getPackageOf(element)
                .getQualifiedName()
                .toString();

        processingEnv
                .getMessager()
                .printMessage(
                        Diagnostic.Kind.NOTE,
                        "Generating gRPC client configuration: " + packageName + "." + configurationName);

        // Scan for gRPC clients using annotation processor API
        Map<String, String> stubClasses = scanGrpcStubsUsingElements(basePackages, roundEnv);

        if (stubClasses.isEmpty()) {
            processingEnv
                    .getMessager()
                    .printMessage(
                            Diagnostic.Kind.WARNING,
                            "No gRPC client stubs found in packages: " + String.join(", ", basePackages));
            return;
        }

        // Generate configuration
        String configurationCode = generateConfiguration(packageName, configurationName, authority, stubClasses);

        // Write to file
        writeConfigurationFile(packageName, configurationName, configurationCode);

        processingEnv
                .getMessager()
                .printMessage(
                        Diagnostic.Kind.NOTE,
                        "Generated gRPC client configuration with " + stubClasses.size() + " clients");
    }

    /**
     * Scan for gRPC stub classes using annotation processor Elements API.
     *
     * @param basePackages packages to scan (will scan all sub-packages)
     * @param roundEnv round environment
     * @return map of stub class simple name to qualified name
     */
    private Map<String, String> scanGrpcStubsUsingElements(String[] basePackages, RoundEnvironment roundEnv) {
        Map<String, String> stubClasses = new LinkedHashMap<>();
        Elements elementUtils = processingEnv.getElementUtils();
        Types typeUtils = processingEnv.getTypeUtils();

        // Get AbstractStub type element
        TypeElement abstractStubElement = elementUtils.getTypeElement("io.grpc.stub.AbstractStub");
        if (abstractStubElement == null) {
            processingEnv
                    .getMessager()
                    .printMessage(Diagnostic.Kind.WARNING, "Cannot find io.grpc.stub.AbstractStub class");
            return stubClasses;
        }

        // 1. Scan root elements (classes being compiled in current compilation unit)
        for (Element element : roundEnv.getRootElements()) {
            if (element.getKind() == ElementKind.CLASS || element.getKind() == ElementKind.INTERFACE) {
                TypeElement typeElement = (TypeElement) element;
                String qualifiedName = typeElement.getQualifiedName().toString();

                // Check if this class is in one of the base packages or sub-packages
                for (String basePackage : basePackages) {
                    if (isInPackageOrSubPackage(qualifiedName, basePackage)) {
                        scanTypeForStubs(typeElement, abstractStubElement, typeUtils, stubClasses);
                        break;
                    }
                }
            }
        }

        // 2. Try to scan packages from dependencies (may not work for all packages)
        // This is a best-effort approach - annotation processor API doesn't provide
        // a way to enumerate all classes in a package from dependencies
        for (String basePackage : basePackages) {
            PackageElement packageElement = elementUtils.getPackageElement(basePackage);
            if (packageElement != null) {
                for (Element element : packageElement.getEnclosedElements()) {
                    if (element.getKind() == ElementKind.CLASS) {
                        TypeElement typeElement = (TypeElement) element;
                        scanTypeForStubs(typeElement, abstractStubElement, typeUtils, stubClasses);
                    }
                }
            }
        }

        processingEnv
                .getMessager()
                .printMessage(
                        Diagnostic.Kind.NOTE,
                        "Scanned packages: " + String.join(", ", basePackages) + ", found " + stubClasses.size()
                                + " stubs");

        return stubClasses;
    }

    /**
     * Check if a qualified class name is in the specified package or its sub-packages.
     *
     * @param qualifiedClassName fully qualified class name (e.g., "io.grpc.testing.protobuf.SimpleServiceGrpc")
     * @param basePackage base package (e.g., "io.grpc")
     * @return true if the class is in the package or sub-packages
     */
    private boolean isInPackageOrSubPackage(String qualifiedClassName, String basePackage) {
        // Extract package name from qualified class name
        int lastDot = qualifiedClassName.lastIndexOf('.');
        if (lastDot == -1) {
            return false; // No package
        }
        String packageName = qualifiedClassName.substring(0, lastDot);
        return matcher.match(basePackage, packageName);
    }

    /**
     * Scan a type element and its inner classes for gRPC stubs.
     */
    private void scanTypeForStubs(
            TypeElement typeElement,
            TypeElement abstractStubElement,
            Types typeUtils,
            Map<String, String> stubClasses) {
        // Check inner classes
        for (Element enclosed : typeElement.getEnclosedElements()) {
            if (enclosed.getKind() == ElementKind.CLASS) {
                TypeElement innerClass = (TypeElement) enclosed;
                String className = innerClass.getSimpleName().toString();

                // Check if it's a stub class (name ends with "Stub" and extends AbstractStub)
                if (className.endsWith("Stub") && extendsAbstractStub(innerClass, abstractStubElement, typeUtils)) {
                    String qualifiedName = innerClass.getQualifiedName().toString();
                    stubClasses.put(className, qualifiedName);
                }
            }
        }
    }

    /**
     * Check if a type extends AbstractStub.
     */
    private boolean extendsAbstractStub(TypeElement typeElement, TypeElement abstractStubElement, Types typeUtils) {
        TypeMirror superclass = typeElement.getSuperclass();
        if (superclass.getKind() != TypeKind.DECLARED) {
            return false;
        }

        DeclaredType declaredType = (DeclaredType) superclass;
        TypeElement superElement = (TypeElement) declaredType.asElement();

        // Check if superclass is AbstractStub or extends AbstractStub
        if (typeUtils.isSameType(
                typeUtils.erasure(superElement.asType()), typeUtils.erasure(abstractStubElement.asType()))) {
            return true;
        }

        // Check superclass recursively
        TypeMirror superSuperclass = superElement.getSuperclass();
        if (superSuperclass.getKind() == TypeKind.DECLARED) {
            return extendsAbstractStub(superElement, abstractStubElement, typeUtils);
        }

        return false;
    }

    /**
     * Generate Spring Configuration code.
     */
    private String generateConfiguration(
            String packageName, String className, String authority, Map<String, String> stubClasses) {
        StringBuilder code = new StringBuilder();

        // Package and imports
        code.append("package ").append(packageName).append(";\n\n");
        code.append("import org.springframework.beans.factory.DisposableBean;\n");
        code.append("import org.springframework.context.annotation.Bean;\n");
        code.append("import org.springframework.context.annotation.Configuration;\n");
        code.append("import org.springframework.context.annotation.Lazy;\n");
        code.append("import io.grpc.ManagedChannel;\n");
        code.append("import io.grpc.ManagedChannelBuilder;\n\n");

        // Import stub classes and their outer classes (deduplicated)
        Set<String> imports = new LinkedHashSet<>();
        for (String qualifiedName : stubClasses.values()) {
            imports.add(qualifiedName);
            // Also import the outer class (e.g., HelloServiceGrpc)
            String outerClassName = qualifiedName.substring(0, qualifiedName.lastIndexOf('.'));
            imports.add(outerClassName);
        }
        for (String importClass : imports) {
            code.append("import ").append(importClass).append(";\n");
        }
        code.append("\n");

        // Class declaration
        code.append("/**\n");
        code.append(" * Auto-generated gRPC client configuration.\n");
        code.append(" * Generated by GrpcClientAnnotationProcessor.\n");
        code.append(" */\n");
        code.append("@Configuration(proxyBeanMethods = false)\n");
        code.append("public class ").append(className).append(" implements DisposableBean {\n\n");

        // Channel bean
        code.append("    private ManagedChannel channel;\n\n");
        code.append("    @Bean\n");
        code.append("    public ManagedChannel grpcChannel() {\n");
        code.append("        if (channel == null) {\n");
        code.append("            channel = ManagedChannelBuilder.forTarget(\"%s\")\n".formatted(authority));
        code.append("                    .usePlaintext()\n");
        code.append("                    .build();\n");
        code.append("        }\n");
        code.append("        return channel;\n");
        code.append("    }\n\n");

        // Stub beans
        for (Map.Entry<String, String> entry : stubClasses.entrySet()) {
            String simpleName = entry.getKey();
            String qualifiedName = entry.getValue();
            String beanName = Character.toLowerCase(simpleName.charAt(0)) + simpleName.substring(1);

            // Extract the outer class name (e.g., HelloServiceGrpc from HelloServiceGrpc.HelloServiceBlockingStub)
            String outerClassName = qualifiedName.substring(0, qualifiedName.lastIndexOf('.'));
            String outerSimpleName = outerClassName.substring(outerClassName.lastIndexOf('.') + 1);

            // Determine the factory method name
            // HelloServiceStub -> newStub
            // HelloServiceBlockingStub -> newBlockingStub
            // HelloServiceBlockingV2Stub -> newBlockingV2Stub
            // HelloServiceFutureStub -> newFutureStub
            String factoryMethodName = determineFactoryMethodName(simpleName);

            code.append("    @Bean\n");
            code.append("    @Lazy\n");
            code.append("    public %s %s() {\n".formatted(simpleName, beanName));
            code.append("        return %s.%s(grpcChannel());\n".formatted(outerSimpleName, factoryMethodName));
            code.append("    }\n\n");
        }

        // DisposableBean destroy method
        code.append("    @Override\n");
        code.append("    public void destroy() {\n");
        code.append("        if (channel != null && !channel.isShutdown()) {\n");
        code.append("            channel.shutdown();\n");
        code.append("        }\n");
        code.append("    }\n");

        code.append("}\n");

        return code.toString();
    }

    /**
     * Determine the factory method name for a stub class.
     * Examples:
     * - HelloServiceStub -> newStub
     * - HelloServiceBlockingStub -> newBlockingStub
     * - HelloServiceBlockingV2Stub -> newBlockingV2Stub
     * - HelloServiceFutureStub -> newFutureStub
     * - HelloServiceAsyncStub -> newStub (async is the default)
     */
    private String determineFactoryMethodName(String stubClassName) {
        // Remove the service name prefix to get the stub type
        // e.g., HelloServiceBlockingStub -> BlockingStub
        if (stubClassName.endsWith("BlockingV2Stub")) {
            return "newBlockingV2Stub";
        } else if (stubClassName.endsWith("BlockingStub")) {
            return "newBlockingStub";
        } else if (stubClassName.endsWith("FutureStub")) {
            return "newFutureStub";
        } else if (stubClassName.endsWith("AsyncStub") || stubClassName.endsWith("Stub")) {
            // AsyncStub or just Stub (which is async by default)
            return "newStub";
        }
        // Fallback
        return "newStub";
    }

    private void writeConfigurationFile(String packageName, String className, String content) throws IOException {
        String qualifiedName = packageName + "." + className;
        JavaFileObject fileObject = processingEnv.getFiler().createSourceFile(qualifiedName);

        try (Writer writer = fileObject.openWriter()) {
            writer.write(content);
        }
    }
}
