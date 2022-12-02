package i5.bml.parser.types;

import generatedParser.BMLParser;
import i5.bml.parser.errors.Diagnostics;
import i5.bml.parser.walker.DiagnosticsCollector;
import org.antlr.symtab.Type;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.Map;

import static i5.bml.parser.errors.ParserError.CANT_RESOLVE_IN;
import static i5.bml.parser.errors.ParserError.NOT_DEFINED;

@BMLType(name = BuiltinType.MAP, isComplex = true)
public class BMLMap extends AbstractBMLType {

    Type keyType;

    public BMLMap() {
    }

    public BMLMap(Type keyType) {
        this.keyType = keyType;
    }

    public BMLMap(Type keyType, Map<String, Type> supportedAccesses) {
        this.keyType = keyType;
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
                        CANT_RESOLVE_IN.format(ctx.getText(), getName()), ((TerminalNode) ctx).getSymbol());
                return TypeRegistry.resolveType(BuiltinType.OBJECT);
            }

            return valueType;
        } else { // Function call
            var functionCallCtx = (BMLParser.FunctionCallContext) ctx;
            var returnType = supportedAccesses.get(functionCallCtx.functionName.getText());
            if (returnType == null) {
                Diagnostics.addDiagnostic(diagnosticsCollector.getCollectedDiagnostics(),
                        CANT_RESOLVE_IN.format(((BMLParser.FunctionCallContext) ctx).functionName.getText(), getName()),
                        functionCallCtx.functionName);
                return TypeRegistry.resolveType(BuiltinType.OBJECT);
            }

            return returnType;
        }
    }

    @Override
    public String encodeToString() {
        return "BMLMap{keyType=%s, supportedAccesses=%s}".formatted(keyType, supportedAccesses);
    }
}
