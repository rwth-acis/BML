package i5.bml.transpiler.generators;


import i5.bml.parser.types.AbstractBMLType;
import i5.bml.parser.types.TypeRegistry;
import org.antlr.symtab.Type;
import org.reflections.Reflections;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

public class GeneratorRegistry {

    private static final Map<String, Generator> registeredGenerators = new HashMap<>();

    static {
        Reflections reflections = new Reflections("i5.bml.transpiler.generators");
        var annotated = reflections.getTypesAnnotatedWith(CodeGenerator.class);

        TypeRegistry.getRegisteredTypes().forEach((encodedTypeName, type) -> {
            var codeGeneratorClazz = annotated.stream()
                    .filter(a -> a.getAnnotation(CodeGenerator.class).typeClass().isInstance(type))
                    .findAny();

            if (codeGeneratorClazz.isEmpty()) {
                //throw new IllegalStateException("No generator for type %s".formatted(type.getName()));
            } else {
                Generator generatorInstance = null;
                try {
                    generatorInstance = (Generator) codeGeneratorClazz.get().getDeclaredConstructor(Type.class).newInstance(type);
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                         NoSuchMethodException e) {
                    // TODO: Handle error
                    e.printStackTrace();
                }

                registeredGenerators.put(encodedTypeName, generatorInstance);
            }
        });
    }

    public static Generator getGeneratorForType(Type type) {
        return registeredGenerators.get(((AbstractBMLType) type).encodeToString().toLowerCase());
    }
}
