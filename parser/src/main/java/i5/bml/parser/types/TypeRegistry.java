package i5.bml.parser.types;

import org.antlr.symtab.Type;
import org.reflections.Reflections;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class TypeRegistry {

    private static final Map<String, Type> typeRegistry = new HashMap<>();

    private static final Set<String> builtinTypes = new HashSet<>();

    private static final Map<String, Class<?>> complexTypeBlueprints = new HashMap<>();

    static {
        for (BuiltinTypes value : BuiltinTypes.values()) {
            builtinTypes.add(value.name().toLowerCase());
        }

        Reflections reflections = new Reflections("i5.bml.parser.types");
        Set<Class<?>> annotated = reflections.getTypesAnnotatedWith(BMLType.class);

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
                complexTypeBlueprints.put(type.name().toLowerCase(), clazz);
            } else {
                Type primitiveTypeInstance;
                try {
                    primitiveTypeInstance = (Type) clazz.getDeclaredConstructor().newInstance();
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                         NoSuchMethodException e) {
                    throw new RuntimeException(e);
                }

                typeRegistry.put(type.name().toLowerCase(), primitiveTypeInstance);
            }
        }

        // Explicitly add "Float Number" as Type
        typeRegistry.put("float number", new BMLNumber(true));
    }

    public static Type resolveBuiltinType(String typeName) {
        typeName = typeName.toLowerCase();

        if (!isTypeBuiltin(typeName)) {
            throw new IllegalStateException("Unknown type `%s`".formatted(typeName));
        }

        if (isTypeComplex(typeName)) {
            return resolveComplexType(typeName);
        } else {
            return resolveType(typeName);
        }
    }

    public static Type resolveType(String typeName) {
        return typeRegistry.get(typeName.toLowerCase());
    }

    public static Type resolveComplexType(String typeName) {
        typeName = typeName.toLowerCase();
        var clazz = complexTypeBlueprints.get(typeName);
        if (clazz == null) {
            return null;
        }

        Type complexTypeInstance;
        try {
            complexTypeInstance = (Type) clazz.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        return complexTypeInstance;
    }

    public static void registerType(String typeName, Type type) {
        typeRegistry.put(typeName.toLowerCase(), type);
    }

    public static boolean isTypeBuiltin(String typeName) {
        return builtinTypes.contains(typeName.toLowerCase());
    }

    public static boolean isTypeComplex(String typeName) {
        return complexTypeBlueprints.containsKey(typeName.toLowerCase());
    }
}
