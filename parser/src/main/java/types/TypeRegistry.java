package types;

import errors.ParserException;
import org.antlr.symtab.Type;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.reflections.Reflections;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

import static errors.ParserError.UNKNOWN_TYPE;

public class TypeRegistry {

    private static final Map<String, Class<?>> typeRegistry = new HashMap<>();

    static {
        Reflections reflections = new Reflections("types");
        Set<Class<?>> annotated = reflections.getTypesAnnotatedWith(BMLType.class);

        Map<Integer, String> uniqueBMLTypeIndices = new HashMap<>();
        Map<Integer, String> uniqueBMLCheckIndices = new HashMap<>();

        for (Class<?> clazz : annotated) {
            BMLType type = clazz.getAnnotation(BMLType.class);

            // Check: class has unique index
            var name = uniqueBMLTypeIndices.get(type.index());
            if (name != null) {
                // TODO
                throw new IllegalStateException("Class %s has the same index (%d) as class %s"
                        .formatted(clazz.getName(), type.index(), name));
            }
            uniqueBMLTypeIndices.put(type.index(), clazz.getName());

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

            // Check: functions with @BMLCheck need exactly one parameter with a type that extends ParserRuleContext
            // Check: functions with @BMLCheck have unique indices
            Arrays.stream(clazz.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(BMLCheck.class))
                    .forEach(m -> {
                        var index = m.getAnnotation(BMLCheck.class).index();
                        var methodName = uniqueBMLCheckIndices.get(index);
                        if (methodName != null) {
                            // TODO
                            throw new IllegalStateException("Method %s has the same index (%d) as %s"
                                    .formatted(m.getName(), index, methodName));
                        }
                        uniqueBMLCheckIndices.put(index, m.getName());

                        if (m.getParameters().length != 1) {
                            throw new IllegalStateException("Method %s from class %s does not have exactly one parameter"
                                    .formatted(m.getName(), clazz.getName()));
                        } else if (m.getParameters()[0].getType().isAssignableFrom(ParserRuleContext.class)) {
                            throw new IllegalStateException("Parameter %s from method %s from class %s does not have a type that extends from %s"
                                    .formatted(m.getParameters()[0].getName(), m.getName(), clazz.getName(), ParserRuleContext.class));
                        }
                    });

            // Check: at most one function with @BMLSynthesizer & return type is org.antlr.symtab.Type
            var synthesizerCount = Arrays.stream(clazz.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(BMLAccessResolver.class))
                    .count();

            if (synthesizerCount > 1) {
                throw new IllegalStateException("Class %s has more than one @BMLSynthesizer annotated function"
                        .formatted(clazz.getName()));
            } else if (synthesizerCount == 1) {
                var synthesizerMethod = Arrays.stream(clazz.getDeclaredMethods())
                        .filter(m -> m.isAnnotationPresent(BMLAccessResolver.class))
                        .findAny();

                //noinspection OptionalGetWithoutIsPresent
                if (!Type.class.equals(synthesizerMethod.get().getReturnType())) {
                    throw new IllegalStateException("Method %s in class %s needs return type org.antlr.symtab.Type"
                            .formatted(synthesizerMethod.get().getName(), clazz.getName()));
                }
            }

            typeRegistry.put(type.typeString(), clazz);
        }
    }

    public static Object resolveType(Token token) {
        var typeString = token.getText();
        Class<?> type = typeRegistry.get(typeString);
        if (type == null) {
            throw new ParserException(UNKNOWN_TYPE.format(typeString), token);
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
