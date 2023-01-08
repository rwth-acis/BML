package i5.bml.transpiler.generators;

import i5.bml.parser.types.AbstractBMLType;
import i5.bml.parser.types.TypeRegistry;
import i5.bml.parser.types.functions.FunctionRegistry;
import org.antlr.symtab.Type;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class GeneratorRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(GeneratorRegistry.class);

    private static final Map<String, Generator> registeredGenerators = new HashMap<>();

    private static final Map<String, Generator> registeredFunctionGenerators = new HashMap<>();

    static {
        Reflections reflections = new Reflections("i5.bml.transpiler.generators");
        var types = reflections.getTypesAnnotatedWith(CodeGenerator.class);

        TypeRegistry.getRegisteredTypes().forEach((encodedTypeName, typeGenerator) -> {
            var generatorClass = types.stream()
                    .filter(a -> a.getAnnotation(CodeGenerator.class).typeClass().isInstance(typeGenerator))
                    .findAny();

            if (generatorClass.isEmpty()) {
                LOGGER.debug("No type generator for {}", encodedTypeName);
                return;
            }

            try {
                var constructors = generatorClass.get().getDeclaredConstructors();
                if (Arrays.stream(constructors).noneMatch(c -> c.getParameters().length > 0 && c.getParameterTypes()[0].equals(Type.class))) {
                    LOGGER.error("Class {} does not have a constructor with a Type parameter", generatorClass.get().getName());
                    return;
                }

                var generatorInstance = (Generator) generatorClass.get().getDeclaredConstructor(Type.class).newInstance(typeGenerator);
                registeredGenerators.put(encodedTypeName, generatorInstance);
            } catch (Exception e) {
                LOGGER.error("Failed to create new instance of generator {}", encodedTypeName, e);
            }
        });

        var functions = new Reflections("i5.bml.transpiler.generators.functions").getTypesAnnotatedWith(CodeGenerator.class);

        FunctionRegistry.getRegisteredFunctions().forEach((functionName, functionGenerator) -> {
            var generatorClass = functions.stream()
                    .filter(a -> a.getAnnotation(CodeGenerator.class).typeClass().isInstance(functionGenerator))
                    .findAny();

            if (generatorClass.isEmpty()) {
                LOGGER.debug("No function generator for {}", functionName);
                return;
            }

            try {
                var generatorInstance = (Generator) generatorClass.get().getDeclaredConstructor().newInstance();
                registeredFunctionGenerators.put(functionName, generatorInstance);
            } catch (Exception e) {
                LOGGER.error("Failed to create new instance of generator {}", generatorClass.get().getSimpleName(), e);
            }
        });
    }

    public static Generator generatorForType(Type type) {
        return registeredGenerators.get(((AbstractBMLType) type).encodeToString().toLowerCase());
    }

    public static Generator generatorForFunctionName(String name) {
        return registeredFunctionGenerators.get(name);
    }

    public static Map<String, Generator> registeredGenerators() {
        return registeredGenerators;
    }
}
