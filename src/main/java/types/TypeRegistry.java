package types;

import org.antlr.symtab.Type;
import org.reflections.Reflections;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class TypeRegistry {

    private static final Map<String, Type> typeRegistry = new HashMap<>();

    static {
        Reflections reflections = new Reflections("types");
        Set<Class<?>> annotated = reflections.getTypesAnnotatedWith(BMLType.class);

        for (Class<?> clazz : annotated) {
            BMLType type = clazz.getAnnotation(BMLType.class);

            Type typeObject;
            try {
                typeObject = (Type) clazz.getConstructor().newInstance();
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                     NoSuchMethodException e) {
                throw new RuntimeException(e);
            }

            typeRegistry.put(type.typeString(), Objects.requireNonNull(typeObject));
        }
    }

    public static Type resolveType(String typeString) {
        Type type = typeRegistry.get(typeString);
        if (type == null) {
            // TODO: Type Error
            throw new TypeNotPresentException("Unknown type%s".formatted(typeString), null);
        }

        return type;
    }
}
