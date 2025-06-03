package grpcstarter.extensions.transcoding;

import com.google.protobuf.Descriptors;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility class for mapping between protobuf descriptors and Java types.
 *
 * @author Freeman
 */
public final class ProtobufJavaTypeUtil {

    private ProtobufJavaTypeUtil() {
        throw new UnsupportedOperationException("Cannot instantiate utility class");
    }

    /**
     * Cache for Java classes mapped by protobuf descriptor full names.
     */
    private static final Map<String, Class<?>> classCache = new ConcurrentHashMap<>();

    /**
     * Cache for Java field types mapped by class and field descriptor combinations.
     */
    private static final Map<Class<?>, Map<String, Type>> fieldTypeCache = new ConcurrentHashMap<>();

    /**
     * Finds the Java class corresponding to the given protobuf message descriptor.
     *
     * @param messageDescriptor the protobuf message descriptor
     * @return the corresponding Java class
     * @throws IllegalStateException if the Java class cannot be found
     */
    public static Class<?> findJavaClass(Descriptors.Descriptor messageDescriptor) {
        if (messageDescriptor == null) {
            throw new IllegalArgumentException("Message descriptor cannot be null");
        }

        String fullName = messageDescriptor.getFullName();
        return classCache.computeIfAbsent(fullName, k -> doFindJavaClass(messageDescriptor));
    }

    /**
     * Finds the Java field type corresponding to the given Java class and protobuf field descriptor.
     *
     * @param javaClass       the Java class containing the field
     * @param fieldDescriptor the protobuf field descriptor
     * @return the Java type of the field
     * @throws IllegalStateException if the field type cannot be determined
     */
    public static Type findJavaFieldType(Class<?> javaClass, Descriptors.FieldDescriptor fieldDescriptor) {
        if (javaClass == null) {
            throw new IllegalArgumentException("Java class cannot be null");
        }
        if (fieldDescriptor == null) {
            throw new IllegalArgumentException("Field descriptor cannot be null");
        }

        return fieldTypeCache
                .computeIfAbsent(javaClass, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(fieldDescriptor.getName(), k -> findGetterReturnType(javaClass, fieldDescriptor));
    }

    private static Class<?> doFindJavaClass(Descriptors.Descriptor descriptor) {
        var options = descriptor.getFile().getOptions();
        var javaPackage = options.hasJavaPackage()
                ? options.getJavaPackage()
                : descriptor.getFile().getPackage();

        List<String> classNames = new ArrayList<>(2);

        if (options.getJavaMultipleFiles()) {
            // When java_multiple_files = true, each message gets its own class
            classNames.add(javaPackage + "." + getSimpleClassName(descriptor));
        } else {
            // When java_multiple_files = false, messages are nested in outer class
            if (options.hasJavaOuterClassname()) {
                classNames.add(
                        javaPackage + "." + options.getJavaOuterClassname() + "$" + getSimpleClassName(descriptor));
            } else {
                // Generate outer class name from proto file name
                String name = descriptor.getFile().getName(); // "google/protobuf/empty.proto"
                String fileName = name.substring(name.lastIndexOf('/') + 1); // "empty.proto"
                String outerClassName = underlineToPascal(fileName.replace(".proto", "")); // "Empty"

                // Try with "OuterClass" suffix first (for potential naming conflicts)
                classNames.add(String.format(
                        "%s.%sOuterClass$%s", javaPackage, outerClassName, getSimpleClassName(descriptor)));
                // Then try without suffix
                classNames.add(String.format("%s.%s$%s", javaPackage, outerClassName, getSimpleClassName(descriptor)));
            }
        }

        Class<?> clazz = null;
        for (String className : classNames) {
            try {
                clazz = Class.forName(className);
                break;
            } catch (ClassNotFoundException ignored) {
            }
        }

        if (clazz == null) {
            throw new IllegalStateException("Unable to find Java class for protobuf message type: " + classNames
                    + " (descriptor: " + descriptor.getFullName() + ")");
        }

        return clazz;
    }

    private static Type findGetterReturnType(Class<?> javaClass, Descriptors.FieldDescriptor fieldDescriptor) {
        String fieldName = fieldDescriptor.getName();
        String getterMethodName;

        if (fieldDescriptor.isMapField()) {
            getterMethodName = "get" + underlineToPascal(fieldName) + "Map";
        } else if (fieldDescriptor.isRepeated()) {
            getterMethodName = "get" + underlineToPascal(fieldName) + "List";
        } else {
            getterMethodName = "get" + underlineToPascal(fieldName);
        }

        try {
            Method getterMethod = javaClass.getMethod(getterMethodName);
            return getterMethod.getGenericReturnType();
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private static String getSimpleClassName(Descriptors.Descriptor descriptor) {
        return Util.getClassName(descriptor);
    }

    private static String underlineToCamel(String name) {
        var sb = new StringBuilder();
        var len = name.length();
        var end = len - 1;
        for (var i = 0; i < len; i++) {
            var c = name.charAt(i);
            if (c == '_' && i < end) {
                sb.append(Character.toUpperCase(name.charAt(++i)));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String underlineToPascal(String name) {
        var n = underlineToCamel(name);
        if (n.isBlank()) {
            return n;
        }
        return Character.toUpperCase(n.charAt(0)) + n.substring(1);
    }
}
