package i5.bml.parser.types.components.primitives;

import generatedParser.BMLParser;
import i5.bml.parser.errors.Diagnostics;
import i5.bml.parser.functions.BMLFunctionParameter;
import i5.bml.parser.types.*;
import i5.bml.parser.types.functions.BMLFunctionType;
import i5.bml.parser.walker.DiagnosticsCollector;
import org.antlr.symtab.Type;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.ArrayList;
import java.util.List;

import static i5.bml.parser.errors.ParserError.EXPECTED_BUT_FOUND;

@BMLType(name = BuiltinType.LIST, isComplex = true)
public class BMLList extends AbstractBMLType implements Summable {

    private final Type itemType;

    public BMLList(Type itemType) {
        this.itemType = itemType;
        var delimiterParameter = new BMLFunctionParameter("delimiter", TypeRegistry.resolveType(BuiltinType.STRING));
        var joinFunction = new BMLFunctionType(TypeRegistry.resolveType(BuiltinType.STRING), List.of(delimiterParameter), new ArrayList<>());
        supportedAccesses.put("join", joinFunction);

        var itemParameter = new BMLFunctionParameter("", itemType);
        var lambdaParameter = new BMLFunctionParameter("f",
                new BMLFunctionType(TypeRegistry.resolveType(BuiltinType.OBJECT), List.of(itemParameter), new ArrayList<>()));
        var mapFunction = new BMLFunctionType(TypeRegistry.resolveType(BuiltinType.OBJECT), List.of(lambdaParameter), new ArrayList<>());
        supportedAccesses.put("map", mapFunction);
    }

    @Override
    public String getName() {
        return "%s<%s>".formatted(super.getName(), itemType.getName());
    }

    @Override
    public Type resolveAccess(DiagnosticsCollector diagnosticsCollector, ParseTree ctx) {
        if (ctx instanceof BMLParser.FunctionCallContext functionCallContext) {
            var functionName = functionCallContext.functionName.getText();
            switch (functionName) {
                case "join" -> {
                    if (!itemType.equals(TypeRegistry.resolveType(BuiltinType.STRING))) {
                        Diagnostics.addDiagnostic(diagnosticsCollector.getCollectedDiagnostics(),
                                EXPECTED_BUT_FOUND.format(BuiltinType.STRING, itemType), functionCallContext);
                        return TypeRegistry.resolveType(BuiltinType.OBJECT);
                    } else {
                        return supportedAccesses.get("join");
                    }
                }
                case "map" -> {

                }
                default -> {
                    return TypeRegistry.resolveType(BuiltinType.OBJECT);
                }
            }
        }

        return ((AbstractBMLType) itemType).resolveAccess(diagnosticsCollector, ctx);
    }

    public Type getItemType() {
        return itemType;
    }

    @Override
    public String encodeToString() {
        return "%s{itemType=%s}".formatted(super.getName(), itemType);
    }
}
