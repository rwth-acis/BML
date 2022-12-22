package i5.bml.parser.types.functions;

import org.reflections.Reflections;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class FunctionRegistry {

    private static final Map<BMLFunctionScope, Set<BMLFunction>> registeredFunctionsInScope = new HashMap<>();

    private static final Map<String, BMLFunction> registeredFunctions = new HashMap<>();

    static {
        var annotated = new Reflections("i5.bml.parser.types.functions").getTypesAnnotatedWith(BMLFunctionAnnotation.class);
        for (var clazz : annotated) {
            BMLFunctionAnnotation functionAnnotation = clazz.getAnnotation(BMLFunctionAnnotation.class);

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

    public static Set<BMLFunction> getFunctionsForScope(BMLFunctionScope bmlFunctionScope) {
        return registeredFunctionsInScope.get(bmlFunctionScope);
    }
}
