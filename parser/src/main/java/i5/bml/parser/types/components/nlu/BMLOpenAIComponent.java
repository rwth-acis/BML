package i5.bml.parser.types.components.nlu;

import generatedParser.BMLParser;
import i5.bml.parser.errors.Diagnostics;
import i5.bml.parser.functions.BMLFunctionParameter;
import i5.bml.parser.types.*;
import i5.bml.parser.types.functions.BMLFunctionType;
import i5.bml.parser.walker.DiagnosticsCollector;
import org.antlr.symtab.Type;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.ArrayList;
import java.util.List;

import static i5.bml.parser.errors.ParserError.CANT_RESOLVE_IN;

@BMLType(name = BuiltinType.OPENAI, isComplex = true)
public class BMLOpenAIComponent extends AbstractBMLType implements CanPopulateParameters {

    @BMLComponentParameter(name = "key", expectedBMLType = BuiltinType.STRING, isRequired = true)
    private String key;

    @BMLComponentParameter(name = "model", expectedBMLType = BuiltinType.STRING, isRequired = true)
    private String model;

    @BMLComponentParameter(name = "tokens", expectedBMLType = BuiltinType.NUMBER, isRequired = false)
    private String tokens;

    @BMLComponentParameter(name = "prompt", expectedBMLType = BuiltinType.STRING, isRequired = false)
    private String prompt;

    @Override
    public void initializeType(ParserRuleContext ctx) {
        var contextType = TypeRegistry.resolveComplexType(BuiltinType.CONTEXT);
        var contextParameter = new BMLFunctionParameter("context", contextType);
        var processFunction = new BMLFunctionType(TypeRegistry.resolveType(BuiltinType.STRING), List.of(contextParameter), new ArrayList<>());
        supportedAccesses.put("process", processFunction);
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

    public String tokens() {
        return tokens;
    }

    public String prompt() {
        return prompt;
    }
}
