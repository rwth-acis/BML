package i5.bml.parser.functions;

import i5.bml.parser.functions.dialogue.*;
import i5.bml.parser.utils.Measurements;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class FunctionRegistry {

    private static final Map<BMLFunctionScope, Set<BMLFunction>> registeredFunctionsInScope = new EnumMap<>(BMLFunctionScope.class);

    private static final Map<String, BMLFunction> registeredFunctions = new HashMap<>();

    private FunctionRegistry() {}

    static {
        Measurements.measure("Registering functions", FunctionRegistry::init);
    }

    public static Set<BMLFunction> getFunctionsForScope(BMLFunctionScope bmlFunctionScope) {
        return registeredFunctionsInScope.get(bmlFunctionScope);
    }

    public static Map<String, BMLFunction> getRegisteredFunctions() {
        return registeredFunctions;
    }

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

        registeredFunctions.put(functionAnnotation.name(), functionInstance);
    }

    public static void init() {
        // - Global functions
        registerFunction(BMLNumberFunction.class);
        registerFunction(BMLSendFunction.class);
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
        registeredFunctions.clear();
    }
}
