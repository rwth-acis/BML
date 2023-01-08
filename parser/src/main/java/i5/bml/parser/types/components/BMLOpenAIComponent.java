package i5.bml.parser.types.components;

import generatedParser.BMLParser;
import i5.bml.parser.errors.Diagnostics;
import i5.bml.parser.types.*;
import i5.bml.parser.utils.Utils;
import i5.bml.parser.walker.DiagnosticsCollector;
import org.antlr.symtab.Type;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.ArrayList;
import java.util.List;

import static i5.bml.parser.errors.ParserError.CANT_RESOLVE_IN;

@BMLType(name = BuiltinType.OPENAI, isComplex = true)
public class BMLOpenAIComponent extends AbstractBMLType {

    @BMLComponentParameter(name = "key", expectedBMLType = BuiltinType.STRING, isRequired = true)
    private String key;

    @BMLComponentParameter(name = "model", expectedBMLType = BuiltinType.STRING, isRequired = true)
    private String model;

    @Override
    public void initializeType(ParserRuleContext ctx) {
        var contextType = TypeRegistry.resolveComplexType(BuiltinType.CONTEXT);
        var contextParameter = new BMLFunctionParameter("context", contextType);
        var processFunction = new BMLFunctionType(TypeRegistry.resolveType(BuiltinType.VOID), List.of(contextParameter), new ArrayList<>());
        supportedAccesses.put("process", processFunction);
    }

    @Override
    public void populateParameters(DiagnosticsCollector diagnosticsCollector, BMLParser.ElementExpressionPairListContext ctx) {
        // Missing parameters, but it has been reported by `checkParameters`
        if (ctx == null) {
            return;
        }

        key = Utils.extractConstStringFromParameter(diagnosticsCollector, ctx, "key");
        model = Utils.extractConstStringFromParameter(diagnosticsCollector, ctx, "model");
    }

    @Override
    public Type resolveAccess(DiagnosticsCollector diagnosticsCollector, ParseTree ctx) {
        var functionCallCtx = (BMLParser.FunctionCallContext) ctx;
        var functionName = functionCallCtx.functionName.getText();

        var functionType = (BMLFunctionType) supportedAccesses.get(functionName);
        if (functionType == null) {
            Diagnostics.addDiagnostic(diagnosticsCollector.getCollectedDiagnostics(),
                    CANT_RESOLVE_IN.format(functionName, getName()), functionCallCtx.functionName);
            return TypeRegistry.resolveType(BuiltinType.OBJECT);
        }

        return functionType;
    }

    public String key() {
        return key;
    }

    public String model() {
        return model;
    }
}
