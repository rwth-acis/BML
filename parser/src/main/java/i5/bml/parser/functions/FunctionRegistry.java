package i5.bml.parser.functions;

import i5.bml.parser.functions.dialogue.*;
import i5.bml.parser.utils.Measurements;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * A utility class that manages a mapping from BML functions scopes to a set of BML functions.
 * This is useful to determine what functions are available for a given {@link BMLFunctionScope}.
 */
public class FunctionRegistry {

    /**
     * Stores the mapping of {@link BMLFunctionScope} to a set of {@link BMLFunction}.
     */
    private static final Map<BMLFunctionScope, Set<BMLFunction>> registeredFunctionsInScope = new EnumMap<>(BMLFunctionScope.class);

    /**
     * Private constructor to prevent instantiation.
     */
    private FunctionRegistry() {}

    static {
        Measurements.measure("Registering functions", FunctionRegistry::init);
    }

    /**
     * Returns the set of functions for the specified {@link BMLFunctionScope}.
     *
     * @param bmlFunctionScope {@link BMLFunctionScope} to get functions for.
     * @return set of functions for the specified {@link BMLFunctionScope}.
     */
    public static Set<BMLFunction> getFunctionsForScope(BMLFunctionScope bmlFunctionScope) {
        return registeredFunctionsInScope.get(bmlFunctionScope);
    }

    /**
     * Registers a {@link BMLFunction} with its scope and name.
     *
     * @param functionClass class of the BMLFunction to register.
     */
    private static void registerFunction(Class<?> functionClass) {
        BMLFunctionAnnotation functionAnnotation = functionClass.getAnnotation(BMLFunctionAnnotation.class);

        BMLFunction functionInstance;
        try {
            functionInstance = (BMLFunction) functionClass.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }

        if (registeredFunctionsInScope.containsKey(functionAnnotation.scope())) {
            registeredFunctionsInScope.get(functionAnnotation.scope()).add(functionInstance);
        } else {
            var set = new HashSet<BMLFunction>();
            set.add(functionInstance);
            registeredFunctionsInScope.put(functionAnnotation.scope(), set);
        }
    }

    /**
     * Initializes the registry by registering all BMLFunctions.
     */
    public static void init() {
        // - Global functions
        registerFunction(BMLNumberFunction.class);
        registerFunction(BMLSendFunction.class);
        registerFunction(BMLSendInlineKeyboardFunction.class);
        registerFunction(BMLStringFunction.class);
        registerFunction(BMLRangeFunction.class);
        registerFunction(BMLSqrtFunction.class);
        registerFunction(BMLDateFunction.class);

        // -- Dialogue functions
        registerFunction(BMLDialogueDefaultFunction.class);
        registerFunction(BMLDialogueInitialFunction.class);
        registerFunction(BMLDialogueJumpToFunction.class);
        registerFunction(BMLDialogueSinkFunction.class);
        registerFunction(BMLDialogueStateFunction.class);
    }

    public static void clear() {
        registeredFunctionsInScope.clear();
    }
}
