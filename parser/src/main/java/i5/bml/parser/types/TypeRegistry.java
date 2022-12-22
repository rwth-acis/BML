package i5.bml.parser.types;

import org.antlr.symtab.Type;
import org.reflections.Reflections;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class TypeRegistry {

    private TypeRegistry() {
    }

    private static final Map<String, Type> registeredTypes = new HashMap<>();

    private static final Set<String> builtinTypes = new HashSet<>();

    private static final Map<String, BMLAnnotationType> builtinAnnotations = new HashMap<>();

    private static final Map<String, Class<?>> complexTypeBlueprints = new HashMap<>();

    private static int typeIndex = 0;

    static {
        init();
    }

    public static Type resolveType(String typeName) {
        return registeredTypes.get(typeName.toLowerCase());
    }

    public static Type resolveType(Type type) {
        return registeredTypes.get(((AbstractBMLType) type).encodeToString().toLowerCase());
    }

    public static Type resolveType(BuiltinType typeName) {
        return registeredTypes.get(typeName.toString().toLowerCase());
    }

    public static Type resolveComplexType(BuiltinType typeName) {
        return resolveComplexType(typeName.name());
    }

    public static Type resolveComplexType(String typeName) {
        var clazz = complexTypeBlueprints.get(typeName.toLowerCase());
        if (clazz == null) {
            return null;
        }

        Type complexTypeInstance;
        try {
            complexTypeInstance = (Type) clazz.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
        return complexTypeInstance;
    }

    public static void registerType(Type type) {
        ((AbstractBMLType) type).setTypeIndex(typeIndex++);
        registeredTypes.put(((AbstractBMLType) type).encodeToString().toLowerCase(), type);
    }

    public static boolean isTypeBuiltin(String typeName) {
        return builtinTypes.contains(typeName.toLowerCase());
    }

    public static BMLAnnotationType getBuiltinAnnotation(String annotationName) {
        return builtinAnnotations.get(annotationName.toLowerCase());
    }

    public static boolean isTypeComplex(String typeName) {
        return complexTypeBlueprints.containsKey(typeName.toLowerCase());
    }

    public static void init() {
        for (var value : BuiltinType.values()) {
            if (!value.isInternal()) {
                builtinTypes.add(value.name().toLowerCase());
            }
        }

        for (var value : BuiltinAnnotation.values()) {
            builtinAnnotations.put(value.name().replaceAll("_", "").toLowerCase(), value.annotationType);
        }

        var annotated = new Reflections("i5.bml.parser.types").getTypesAnnotatedWith(BMLType.class);
        for (Class<?> clazz : annotated) {
            BMLType type = clazz.getAnnotation(BMLType.class);

            // Check: class extends AbstractBMLType
            if (!AbstractBMLType.class.isAssignableFrom(clazz)) {
                throw new IllegalStateException("Class %s does not does not extend %s".formatted(clazz.getName(), AbstractBMLType.class.getName()));
            }

            // Check: class has default constructor with no parameters
            boolean hasDefaultConstructor = Arrays.stream(clazz.getDeclaredConstructors())
                    .anyMatch(constructor -> constructor.getParameterCount() == 0);
            if (!hasDefaultConstructor) {
                throw new IllegalStateException("Class %s does not have an empty default constructor".formatted(clazz.getName()));
            }

            if (type.isComplex()) {
                complexTypeBlueprints.put(type.name().toString().toLowerCase(), clazz);
            } else {
                Type primitiveTypeInstance;
                try {
                    primitiveTypeInstance = (Type) clazz.getDeclaredConstructor().newInstance();
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                         NoSuchMethodException e) {
                    throw new IllegalStateException(e);
                }

                ((AbstractBMLType) primitiveTypeInstance).setTypeIndex(typeIndex++);
                registeredTypes.put(type.name().toString().toLowerCase(), primitiveTypeInstance);
            }
        }

        // Explicitly add BuiltinTypes.FLOAT_NUMBER.toString() as Type
        BMLNumber type = new BMLNumber(true);
        type.setTypeIndex(typeIndex++);
        registeredTypes.put(BuiltinType.FLOAT_NUMBER.toString().toLowerCase(), type);
    }

    public static void clear() {
        registeredTypes.clear();
        builtinTypes.clear();
        complexTypeBlueprints.clear();
    }

    public static Map<String, Type> getRegisteredTypes() {
        return registeredTypes;
    }
}
