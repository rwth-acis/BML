package i5.bml.parser.types;

import generatedParser.BMLParser;
import i5.bml.parser.errors.Diagnostics;
import i5.bml.parser.walker.DiagnosticsCollector;
import org.antlr.symtab.Type;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.Map;

import static i5.bml.parser.errors.ParserError.NOT_DEFINED;

@BMLType(name = "Map", isComplex = true)
public class BMLMap extends AbstractBMLType {

    Type keyType;

    /**
     * Either type Object when there are values with heterogeneous types.
     * Or a specific type because all values have this type.
     */
    Type valueType;

    public BMLMap() {
    }

    public BMLMap(Type keyType, Type valueType) {
        this.keyType = keyType;
        this.valueType = valueType;
    }

    public BMLMap(Type keyType, Type valueType, Map<String, Type> supportedAccesses) {
        this.keyType = keyType;
        this.valueType = valueType;
        super.supportedAccesses = supportedAccesses;
    }

    public Type getKeyType() {
        return null;
    }

    public Type getValueType() {
        return null;
    }

    @Override
    public Type resolveAccess(DiagnosticsCollector diagnosticsCollector, ParseTree ctx) {
        if (ctx instanceof TerminalNode) { // Identifier
            var valueType = supportedAccesses.get(ctx.getText());
            if (valueType == null) {
                Diagnostics.addDiagnostic(diagnosticsCollector.getCollectedDiagnostics(),
                        NOT_DEFINED.format(ctx.getText()), ((TerminalNode) ctx).getSymbol());
                return TypeRegistry.resolveType("Object");
            }

            return valueType;
        } else { // Function call
            var functionCallCtx = (BMLParser.FunctionCallContext) ctx;
            var valueType = supportedAccesses.get(functionCallCtx.functionName.getText());
            if (valueType == null) {
                Diagnostics.addDiagnostic(diagnosticsCollector.getCollectedDiagnostics(),
                        NOT_DEFINED.format(ctx.getText()), functionCallCtx.functionName);
                return TypeRegistry.resolveType("Object");
            }

            return valueType;
        }
    }

    @Override
    public String toString() {
        return "BMLMap{keyType=%s, valueType=%s, supportedAccesses=%s}".formatted(keyType, valueType, supportedAccesses);
    }
}
