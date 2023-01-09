package i5.bml.parser.types.components;

import generatedParser.BMLParser;
import i5.bml.parser.errors.Diagnostics;
import i5.bml.parser.types.*;
import i5.bml.parser.walker.DiagnosticsCollector;
import org.antlr.symtab.Type;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayList;
import java.util.Map;

import static i5.bml.parser.errors.ParserError.CANT_RESOLVE_IN;

@BMLType(name = BuiltinType.MAP, isComplex = true)
public class BMLMap extends AbstractBMLType {

    Type keyType;

    Type valueType;

    public BMLMap() {}

    // TODO: ForEach not available if valueType == object || keyType == object

    public BMLMap(Type keyType, Type valueType, Map<String, Type> supportedAccesses) {
        this.keyType = keyType;
        this.valueType = valueType;
        super.supportedAccesses = supportedAccesses;
    }

    public Type getKeyType() {
        return keyType;
    }

    public Type getValueType() {
        return valueType;
    }

    @Override
    public void initializeType(ParserRuleContext ctx) {
        var addFunction = new BMLFunctionType(TypeRegistry.resolveType(BuiltinType.VOID), new ArrayList<>(), new ArrayList<>());
        supportedAccesses.put("add", addFunction);
        var removeFunction = new BMLFunctionType(TypeRegistry.resolveType(BuiltinType.VOID), new ArrayList<>(), new ArrayList<>());
        supportedAccesses.put("remove", removeFunction);
    }

    @Override
    public void checkParameters(DiagnosticsCollector diagnosticsCollector, BMLParser.ElementExpressionPairListContext ctx) {}

    @Override
    public Type resolveAccess(DiagnosticsCollector diagnosticsCollector, ParseTree ctx) {
        if (ctx instanceof TerminalNode) { // Identifier
            var valueType = supportedAccesses.get(ctx.getText());
            if (valueType == null) {
                Diagnostics.addDiagnostic(diagnosticsCollector.getCollectedDiagnostics(),
                        CANT_RESOLVE_IN.format(ctx.getText(), getName()), ((TerminalNode) ctx).getSymbol());
                return TypeRegistry.resolveType(BuiltinType.OBJECT);
            }

            return valueType;
        } else { // Function call
            var functionCallCtx = (BMLParser.FunctionCallContext) ctx;
            var functionName = functionCallCtx.functionName.getText();

            var functionType = (BMLFunctionType) supportedAccesses.get(functionName);
            if (functionType == null) {
                Diagnostics.addDiagnostic(diagnosticsCollector.getCollectedDiagnostics(),
                        CANT_RESOLVE_IN.format(functionName, getName()), functionCallCtx.functionName);
                return TypeRegistry.resolveType(BuiltinType.OBJECT);
            }

            if (functionName.equals("add")) {
                var invocationKeyParameter = functionCallCtx.params.elementExpressionPair().get(0);
                var invocationValueParameter = functionCallCtx.params.elementExpressionPair().get(1);
                if (!invocationKeyParameter.name.getText().equals("key")) {
                    var t = invocationValueParameter;
                    invocationValueParameter = invocationKeyParameter;
                    invocationKeyParameter = t;
                }

                if (keyType == null) {
                    keyType = invocationKeyParameter.expr.type;
                    functionType.getRequiredParameters().add(new BMLFunctionParameter("key", keyType));
                } else if (functionType.getRequiredParameters().stream().noneMatch(p -> p.getName().equals("key"))) {
                    functionType.getRequiredParameters().add(new BMLFunctionParameter("key", keyType));
                }

                if (valueType == null) {
                    valueType = invocationValueParameter.expr.type;
                    functionType.getRequiredParameters().add(new BMLFunctionParameter("value", valueType));
                } else if (functionType.getRequiredParameters().stream().noneMatch(p -> p.getName().equals("value"))) {
                    functionType.getRequiredParameters().add(new BMLFunctionParameter("value", valueType));
                }
            } else if (functionName.equals("remove")) {
                var invocationKeyParameter = functionCallCtx.params.elementExpressionPair().get(0);

                if (keyType == null) {
                    keyType = invocationKeyParameter.expr.type;
                    functionType.getRequiredParameters().add(new BMLFunctionParameter("key", keyType));
                } else if (functionType.getRequiredParameters().stream().noneMatch(p -> p.getName().equals("key"))) {
                    functionType.getRequiredParameters().add(new BMLFunctionParameter("key", keyType));
                }
            }

            return functionType;
        }
    }

    @Override
    public String toString() {
        return "BMLMap{keyType=%s, valueType=%s, supportedAccesses=%s}".formatted(keyType, valueType, supportedAccesses);
    }

    @Override
    public String encodeToString() {
        return super.toString();
    }
}
