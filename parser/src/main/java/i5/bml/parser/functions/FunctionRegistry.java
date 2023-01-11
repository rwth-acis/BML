package i5.bml.parser.functions;

import i5.bml.parser.utils.IOUtil;
import i5.bml.parser.utils.Measurements;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class FunctionRegistry {

    private static final ClassLoader CLASS_LOADER = FunctionRegistry.class.getClassLoader();

    private static final String USER_DIR = System.getProperty("user.dir");

    private static final Map<BMLFunctionScope, Set<BMLFunction>> registeredFunctionsInScope = new EnumMap<>(BMLFunctionScope.class);

    private static final Map<String, BMLFunction> registeredFunctions = new HashMap<>();

    private FunctionRegistry() {}

    static {
        init(USER_DIR);
    }

    public static Set<BMLFunction> getFunctionsForScope(BMLFunctionScope bmlFunctionScope) {
        return registeredFunctionsInScope.get(bmlFunctionScope);
    }

    public static Map<String, BMLFunction> getRegisteredFunctions() {
        return registeredFunctions;
    }

    public static void init(String userDir) {
        var classes = Measurements.measure("Collecting function classes", () -> {
            return IOUtil.collectClassesFromPackage(CLASS_LOADER, userDir,  "parser", "parser/src/main/java/i5/bml/parser/functions");
        });

        for (var clazz : classes) {
            BMLFunctionAnnotation functionAnnotation = clazz.getAnnotation(BMLFunctionAnnotation.class);
            // Ignore interfaces or helper classes present in types package
            if (functionAnnotation == null) {
                continue;
            }

            BMLFunction functionInstance;
            try {
                functionInstance = (BMLFunction) clazz.getDeclaredConstructor().newInstance();
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
    }

    public static void clear() {
        registeredFunctionsInScope.clear();
        registeredFunctions.clear();
    }
}
