package i5.bml.transpiler.generators;

import i5.bml.parser.functions.BMLFunctionAnnotation;
import i5.bml.parser.types.AbstractBMLType;
import i5.bml.parser.types.TypeRegistry;
import i5.bml.parser.utils.Measurements;
import i5.bml.transpiler.generators.dialogue.DialogueGenerator;
import i5.bml.transpiler.generators.dialogue.StateGenerator;
import i5.bml.transpiler.generators.functions.*;
import i5.bml.transpiler.generators.functions.dialogue.DialogueJumpToFunctionGenerator;
import i5.bml.transpiler.generators.types.annotations.MessageAnnotationGenerator;
import i5.bml.transpiler.generators.types.annotations.RoutineAnnotationGenerator;
import i5.bml.transpiler.generators.types.components.ContextGenerator;
import i5.bml.transpiler.generators.types.components.messenger.SlackGenerator;
import i5.bml.transpiler.generators.types.components.messenger.TelegramGenerator;
import i5.bml.transpiler.generators.types.components.nlu.OpenAIGenerator;
import i5.bml.transpiler.generators.types.components.nlu.RasaGenerator;
import i5.bml.transpiler.generators.types.components.openapi.OpenAPIGenerator;
import i5.bml.transpiler.generators.types.components.openapi.OpenAPISchemaGenerator;
import i5.bml.transpiler.generators.types.components.primitives.*;
import i5.bml.transpiler.generators.types.messenger.UserGenerator;
import org.antlr.symtab.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class GeneratorRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(GeneratorRegistry.class);

    private static final Map<String, Generator> registeredGenerators = new HashMap<>();

    private static final Map<String, Generator> registeredFunctionGenerators = new HashMap<>();

    private GeneratorRegistry() {}

    static {
        Measurements.measure("Registering generators", GeneratorRegistry::init);
    }

    private static void init() {
        // - Generators

        // -- Annotation generators
        registerTypeGenerator(MessageAnnotationGenerator.class);
        registerTypeGenerator(RoutineAnnotationGenerator.class);

        // -- Component generators
        registerTypeGenerator(ContextGenerator.class);

        // --- Messenger component generators
        registerTypeGenerator(UserGenerator.class);
        registerTypeGenerator(SlackGenerator.class);
        registerTypeGenerator(TelegramGenerator.class);

        // --- NLU component generators
        registerTypeGenerator(OpenAIGenerator.class);
        registerTypeGenerator(RasaGenerator.class);

        // --- OpenAPI component generators
        registerTypeGenerator(OpenAPIGenerator.class);
        registerTypeGenerator(OpenAPISchemaGenerator.class);

        // --- Primitive component generators
        registerTypeGenerator(NumberGenerator.class);
        registerTypeGenerator(BooleanGenerator.class);
        registerTypeGenerator(StringGenerator.class);
        registerTypeGenerator(ListGenerator.class);
        registerTypeGenerator(MapGenerator.class);

        // -- Dialogue generators
        registerTypeGenerator(DialogueGenerator.class);
        registerTypeGenerator(StateGenerator.class);

        // -- Function generators
        registerFunctionGenerator(NumberFunctionGenerator.class);
        registerFunctionGenerator(SendFunctionGenerator.class);
        registerFunctionGenerator(StringFunctionGenerator.class);
        registerFunctionGenerator(RangeFunctionGenerator.class);
        registerFunctionGenerator(SqrtFunctionGenerator.class);
        registerFunctionGenerator(DateFunctionGenerator.class);

        // --- Dialogue function generators
        registerFunctionGenerator(DialogueJumpToFunctionGenerator.class);
    }

    private static void registerTypeGenerator(Class<?> generatorClass) {
        var annotation = generatorClass.getAnnotation(CodeGenerator.class);
        TypeRegistry.getRegisteredTypes().entrySet().stream()
                .filter(e -> annotation.typeClass().isInstance(e.getValue()))
                .forEach(typeRegistryEntry -> {
                    try {
//                        var constructors = generatorClass.getDeclaredConstructors();
//                        if (Arrays.stream(constructors).noneMatch(c -> c.getParameters().length > 0 && c.getParameterTypes()[0].equals(Type.class))) {
//                            LOGGER.error("Class {} does not have a constructor with a Type parameter", generatorClass.get().getName());
//                            return;
//                        }

                        var generatorInstance = (Generator) generatorClass.getDeclaredConstructor(Type.class).newInstance(typeRegistryEntry.getValue());
                        registeredGenerators.put(typeRegistryEntry.getKey(), generatorInstance);
                    } catch (Exception e) {
                        LOGGER.error("Failed to create new instance of generator {}", typeRegistryEntry.getKey(), e);
                    }
                });
    }

    private static void registerFunctionGenerator(Class<?> generatorClass) {
        var annotation = generatorClass.getAnnotation(CodeGenerator.class);
        var functionName = annotation.typeClass().getAnnotation(BMLFunctionAnnotation.class).name();

        try {
            var generatorInstance = (Generator) generatorClass.getDeclaredConstructor().newInstance();
            registeredFunctionGenerators.put(functionName, generatorInstance);
        } catch (Exception e) {
            LOGGER.error("Failed to create new instance of generator {}", generatorClass.getSimpleName(), e);
        }
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
