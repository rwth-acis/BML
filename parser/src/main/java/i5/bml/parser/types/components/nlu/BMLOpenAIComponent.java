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

import java.sql.Time;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static i5.bml.parser.errors.ParserError.CANT_RESOLVE_IN;

@BMLType(name = BuiltinType.OPENAI, isComplex = true)
public class BMLOpenAIComponent extends AbstractBMLType implements CanPopulateParameters {

    @BMLComponentParameter(name = "key", expectedBMLType = BuiltinType.STRING, isRequired = true)
    private String key;

    @BMLComponentParameter(name = "model", expectedBMLType = BuiltinType.STRING, isRequired = true)
    private String model;

    @BMLComponentParameter(name = "tokens", expectedBMLType = BuiltinType.NUMBER, isRequired = false)
    private String tokens;

    @BMLComponentParameter(name = "timeout", expectedBMLType = BuiltinType.STRING, isRequired = false)
    private String timeout;

    private String duration;

    private ChronoUnit timeUnit;

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
    public void populateParameters(DiagnosticsCollector diagnosticsCollector, BMLParser.ElementExpressionPairListContext ctx) {
        super.populateParameters(diagnosticsCollector, ctx);
        if (!timeout.isEmpty()) {
            duration = timeout.split("[a-zA-Z]+")[0];
            timeUnit = switch (timeout.split("\\d+")[1]) {
                case "ns" -> ChronoUnit.NANOS;
                case "µs" -> ChronoUnit.MICROS;
                case "ms" -> ChronoUnit.MILLIS;
                case "s" -> ChronoUnit.SECONDS;
                case "m" -> ChronoUnit.MINUTES;
                case "h" -> ChronoUnit.HOURS;
                case "d" -> ChronoUnit.DAYS;
                default -> {
                    Diagnostics.addDiagnostic(diagnosticsCollector.getCollectedDiagnostics(),
                            "Can't recognize time unit, allowed time units are: ns, µs, ms, s, m, h, d", ctx);
                    yield ChronoUnit.HOURS;
                }
            };
        }
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

    public String duration() {
        return duration;
    }

    public ChronoUnit timeUnit() {
        return timeUnit;
    }

    public String prompt() {
        return prompt;
    }
}
