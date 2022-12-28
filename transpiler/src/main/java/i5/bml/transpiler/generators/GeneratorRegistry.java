package i5.bml.transpiler.generators;


import i5.bml.parser.types.AbstractBMLType;
import i5.bml.parser.types.TypeRegistry;
import i5.bml.parser.types.functions.BMLFunction;
import i5.bml.parser.types.functions.BMLFunctionAnnotation;
import i5.bml.parser.types.functions.FunctionRegistry;
import org.antlr.symtab.Type;
import org.reflections.Reflections;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class GeneratorRegistry {

    private static final Map<String, Generator> registeredGenerators = new HashMap<>();
    private static final Map<String, Generator> registeredFunctionGenerators = new HashMap<>();

    static {
        Reflections reflections = new Reflections("i5.bml.transpiler.generators");
        var types = reflections.getTypesAnnotatedWith(CodeGenerator.class);

        TypeRegistry.getRegisteredTypes().forEach((encodedTypeName, type) -> {
            var generatorClass = types.stream()
                    .filter(a -> a.getAnnotation(CodeGenerator.class).typeClass().isInstance(type))
                    .findAny();

            if (generatorClass.isEmpty()) {
                System.out.println(type);
                //throw new IllegalStateException("No generator for type %s".formatted(type.getName()));
            } else {
                Generator generatorInstance = null;
                try {
                    var constructors = generatorClass.get().getDeclaredConstructors();
                    if (Arrays.stream(constructors).noneMatch(c -> c.getParameters().length > 0 && c.getParameterTypes()[0].equals(Type.class))) {
                        throw new IllegalStateException("Class %s does not have a constructor with a Type parameter"
                                .formatted(generatorClass.get().getName()));
                    }

                    generatorInstance = (Generator) generatorClass.get().getDeclaredConstructor(Type.class).newInstance(type);
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                         NoSuchMethodException e) {
                    // TODO: Handle error
                    e.printStackTrace();
                }

                registeredGenerators.put(encodedTypeName, generatorInstance);
            }
        });

        var functions = new Reflections("i5.bml.transpiler.generators.functions").getTypesAnnotatedWith(CodeGenerator.class);

        FunctionRegistry.getRegisteredFunctions().forEach((name, function) -> {
            var generatorClazz = functions.stream()
                    .filter(a -> a.getAnnotation(CodeGenerator.class).typeClass().isInstance(function))
                    .findAny();

            if (generatorClazz.isEmpty()) {
                //throw new IllegalStateException("No generator for type %s".formatted(type.getName()));
            } else {
                Generator generatorInstance = null;
                try {
                    generatorInstance = (Generator) generatorClazz.get().getDeclaredConstructor().newInstance();
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                         NoSuchMethodException e) {
                    // TODO: Handle error
                    e.printStackTrace();
                }

                registeredFunctionGenerators.put(name, generatorInstance);
            }
        });
    }

    public static Generator getGeneratorForType(Type type) {
        return registeredGenerators.get(((AbstractBMLType) type).encodeToString().toLowerCase());
    }

    public static Generator getGeneratorForFunctionName(String name) {
        return registeredFunctionGenerators.get(name);
    }

    public static Map<String, Generator> getRegisteredGenerators() {
        return registeredGenerators;
    }
}
