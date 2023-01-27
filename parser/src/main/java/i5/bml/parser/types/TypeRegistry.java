package i5.bml.parser.types;

import i5.bml.parser.types.annotations.BMLActionAnnotation;
import i5.bml.parser.types.annotations.BMLAnnotationType;
import i5.bml.parser.types.annotations.BMLMessengerAnnotation;
import i5.bml.parser.types.annotations.BMLRoutineAnnotation;
import i5.bml.parser.types.components.BMLContext;
import i5.bml.parser.types.components.messenger.BMLSlackComponent;
import i5.bml.parser.types.components.messenger.BMLTelegramComponent;
import i5.bml.parser.types.components.messenger.BMLUser;
import i5.bml.parser.types.components.nlu.BMLOpenAIComponent;
import i5.bml.parser.types.components.nlu.BMLRasaComponent;
import i5.bml.parser.types.components.openapi.BMLOpenAPIComponent;
import i5.bml.parser.types.components.openapi.BMLOpenAPISchema;
import i5.bml.parser.types.components.primitives.*;
import i5.bml.parser.types.dialogue.BMLDialogue;
import i5.bml.parser.types.dialogue.BMLState;
import i5.bml.parser.types.functions.BMLFunctionType;
import i5.bml.parser.types.functions.BMLVoid;
import i5.bml.parser.utils.Measurements;
import org.antlr.symtab.Type;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * TypeRegistry is a utility class that provides methods for registering, resolving, and querying types.
 * Types can be registered by calling {@link TypeRegistry#registerType(Type)}, and resolved by calling
 * {@link TypeRegistry#resolveType(String)}, {@link TypeRegistry#resolveType(Type)},
 * {@link TypeRegistry#resolveType(BuiltinType)}, or {@link TypeRegistry#resolveComplexType(String)}.
 * Additionally, it provides methods for querying whether a type is builtin or complex and for getting
 * built-in annotations.
 * <p>
 * The class plays a central role in the re-usability of types. The underlying idea is that types are singletons, i.e.,
 * there can only exist one type. This is quite obvious in the case of Strings or Booleans. However, for a component
 * of type {@link BMLOpenAPIComponent} it is different, since the type is determined by the specific OpenAPI specification.
 * The class {@link AbstractBMLType} provides means for types to define what makes them equal. In the case of Strings and
 * Booleans this could simply be class equality, i.e., the `instanceof` relation. In the case of an OpenAPI component
 * it should be class equality and specification equality (e.g., same url).
 */

public class TypeRegistry {

    private static final Map<String, Type> registeredTypes = new HashMap<>();

    private static final Set<String> builtinTypes = new HashSet<>();

    private static final Map<String, BMLAnnotationType> builtinAnnotations = new HashMap<>();

    private static final Map<String, Class<?>> complexTypeBlueprints = new HashMap<>();

    private static int typeIndex = 0;

    private TypeRegistry() {
    }

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

    /**
     * Resolves a complex type (see {@link BMLType#isComplex()}) by its name.
     *
     * @param typeName The name of the complex type to resolve.
     * @return An instance of the complex type, or null if the type is not registered.
     * @throws IllegalStateException if the class does not have a default constructor or if the class does not extend AbstractBMLType.
     */
    public static Type resolveComplexType(String typeName) {
        var clazz = complexTypeBlueprints.get(typeName.toLowerCase());
        if (clazz == null) {
            return null;
        }

        Type complexTypeInstance;
        try {
            complexTypeInstance = (Type) clazz.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException e) {
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

    /**
     * Registers a type class as a complex or primitive type.
     *
     * @param typeClass the class of the type to register.
     * @throws IllegalStateException if the class does not have an empty default constructor, or
     *                               if the class does not have the annotation BMLType, or if the class does not extend AbstractBMLType.
     */
    private static void registerType(Class<?> typeClass) {
        BMLType type = typeClass.getAnnotation(BMLType.class);

        if (type.isComplex()) {
            complexTypeBlueprints.put(type.name().toString().toLowerCase(), typeClass);
        } else {
            Type primitiveTypeInstance;
            try {
                primitiveTypeInstance = (Type) typeClass.getDeclaredConstructor().newInstance();
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                     NoSuchMethodException e) {
                throw new IllegalStateException(e);
            }

            ((AbstractBMLType) primitiveTypeInstance).setTypeIndex(typeIndex++);
            registeredTypes.put(type.name().toString().toLowerCase(), primitiveTypeInstance);
        }
    }

    /**
     * Initializes the {@link TypeRegistry} by registering built-in types, annotations and types that are defined
     * in the classpath. The classpath here refers to the subpackages of {@link i5.bml.parser.types}.
     */
    public static void init() {
        for (var value : BuiltinType.values()) {
            if (!value.isInternal()) {
                builtinTypes.add(value.name().toLowerCase());
            }
        }

        for (var value : BuiltinAnnotation.values()) {
            builtinAnnotations.put(value.name().replace("_", "").toLowerCase(), value.annotationType);
        }

        Measurements.measure("Registering types", () -> {
            // - Types
            registerType(BMLBot.class);
            registerType(BMLObject.class);

            // -- Annotation types
            registerType(BMLActionAnnotation.class);
            registerType(BMLMessengerAnnotation.class);
            registerType(BMLRoutineAnnotation.class);

            // -- Component types
            registerType(BMLContext.class);

            // --- Messenger component types
            registerType(BMLUser.class);
            registerType(BMLTelegramComponent.class);
            registerType(BMLSlackComponent.class);

            // --- NLU component types
            registerType(BMLRasaComponent.class);
            registerType(BMLOpenAIComponent.class);

            // --- OpenAPI component types
            registerType(BMLOpenAPIComponent.class);
            registerType(BMLOpenAPISchema.class);

            // --- Primitive component types
            registerType(BMLBoolean.class);
            registerType(BMLNumber.class);
            registerType(BMLString.class);
            registerType(BMLList.class);
            registerType(BMLMap.class);

            // -- Dialogue types
            registerType(BMLDialogue.class);
            registerType(BMLState.class);

            // -- Function types
            registerType(BMLVoid.class);
            registerType(BMLFunctionType.class);
        });

        // Explicitly add BuiltinTypes.FLOAT_NUMBER.toString() as Type
        BMLNumber type = new BMLNumber(true);
        type.setTypeIndex(typeIndex++);
        registeredTypes.put(BuiltinType.FLOAT_NUMBER.toString().toLowerCase(), type);

        // Explicitly add BuiltinTypes.FLOAT_NUMBER.toString() as Type
        type = new BMLNumber(false, true);
        type.setTypeIndex(typeIndex++);
        registeredTypes.put(BuiltinType.LONG_NUMBER.toString().toLowerCase(), type);

        // FIXME: Explicitly add BuiltinTypes.STATE.toString() as Type
        var stateType = new BMLState();
        stateType.setTypeIndex(typeIndex++);
        registeredTypes.put(BuiltinType.STATE.toString().toLowerCase(), stateType);
    }

    public static void clear() {
        registeredTypes.clear();
        builtinTypes.clear();
        builtinAnnotations.clear();
        complexTypeBlueprints.clear();
    }

    public static Map<String, Type> getRegisteredTypes() {
        return registeredTypes;
    }
}
