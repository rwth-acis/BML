package types;

import org.antlr.symtab.Type;
import org.reflections.Reflections;
import org.yaml.snakeyaml.constructor.ConstructorException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.util.*;

public class TypeRegistry {

    private static final Map<String, Class<?>> typeRegistry = new HashMap<>();

    static {
        Reflections reflections = new Reflections("types");
        Set<Class<?>> annotated = reflections.getTypesAnnotatedWith(BMLType.class);

        for (Class<?> clazz : annotated) {
            BMLType type = clazz.getAnnotation(BMLType.class);

            // Check: class has default constructor with no parameters
            boolean hasDefaultConstructor = Arrays.stream(clazz.getDeclaredConstructors())
                    .anyMatch(constructor -> constructor.getParameterCount() == 0);
            if (!hasDefaultConstructor) {
                // TODO: Proper error handling
                throw new IllegalStateException("Class %s does not have an empty default constructor".formatted(clazz.getName()));
            }

            // Check: class extends AbstractBMLType
            if (!AbstractBMLType.class.isAssignableFrom(clazz)) {
                // TODO: Proper error handling
                throw new IllegalStateException("Class %s does not does not extend %s".formatted(clazz.getName(), AbstractBMLType.class.getName()));
            }

            // Check: Functions with @BMLFunction annotation for required parameter types use org.antlr.symtab.Type
            Arrays.stream(clazz.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(BMLFunction.class))
                    .forEach(m -> {
                        for (var parameter : m.getParameters()) {
                            if (!parameter.isAnnotationPresent(BMLFunctionParameter.class)) {
                                throw new IllegalStateException("Parameter %s from method %s does not have a %s annotation"
                                        .formatted(parameter.getName(), m.getName(), BMLFunctionParameter.class));
                            }

                            if (!Type.class.isAssignableFrom(parameter.getType())) {
                                throw new IllegalStateException("Parameter %s from method %s does not have a type that extends from %s"
                                        .formatted(parameter.getName(), m.getName(), Type.class));
                            }
                        }
                    });

            typeRegistry.put(type.typeString(), clazz);
        }
    }

    public static Object resolveType(String typeString) {
        Class<?> type = typeRegistry.get(typeString);
        if (type == null) {
            // TODO: Type Error
            throw new TypeNotPresentException("Unknown type%s".formatted(typeString), null);
        }

        Object typeInstance;
        try {
            typeInstance = type.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }

        return typeInstance;
    }
}
