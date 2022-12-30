package i5.bml.parser.types.annotations;

import generatedParser.BMLParser;
import i5.bml.parser.errors.Diagnostics;
import i5.bml.parser.types.AbstractBMLType;
import i5.bml.parser.types.BMLComponentParameter;
import i5.bml.parser.types.BMLType;
import i5.bml.parser.types.BuiltinType;
import i5.bml.parser.utils.Utils;
import i5.bml.parser.walker.DiagnosticsCollector;

import java.util.concurrent.TimeUnit;

import static i5.bml.parser.errors.ParserError.PARAM_REQUIRES_CONSTANT;

@BMLType(name = BuiltinType.ROUTINE_ANNOTATION, isComplex = true)
public class BMLRoutineAnnotation extends AbstractBMLType {

    @BMLComponentParameter(name = "rate", expectedBMLType = BuiltinType.STRING, isRequired = true)
    private String rate;

    private String period;

    private TimeUnit timeUnit;

    @Override
    public void populateParameters(DiagnosticsCollector diagnosticsCollector, BMLParser.ElementExpressionPairListContext ctx) {
        // Missing parameters, but it has been reported by `checkParameters`
        if (ctx == null) {
            return;
        }

        var expr = ctx.elementExpressionPair(0).expr;
        rate = Utils.extractConstStringFromParameter(diagnosticsCollector, ctx, "rate").replaceAll(" ", "");
        if (!rate.matches("[0-9]+[a-zA-Z]+")) {
            Diagnostics.addDiagnostic(diagnosticsCollector.getCollectedDiagnostics(),
                    "Can't recognize format, required format is <number><timeUnit>", expr);
            return;
        }

        period = rate.split("[a-zA-Z]+")[0];
        timeUnit = switch (rate.split("[0-9]+")[1]) {
            case "ns" -> TimeUnit.NANOSECONDS;
            case "µs" -> TimeUnit.MICROSECONDS;
            case "ms" -> TimeUnit.MILLISECONDS;
            case "s" -> TimeUnit.SECONDS;
            case "m" -> TimeUnit.MINUTES;
            case "h" -> TimeUnit.HOURS;
            case "d" -> TimeUnit.DAYS;
            default -> {
                Diagnostics.addDiagnostic(diagnosticsCollector.getCollectedDiagnostics(),
                        "Can't recognize time unit, allowed time units are: ns, µs, ms, s, m, h, d", expr);
                yield TimeUnit.HOURS;
            }
        };
    }

    public String getPeriod() {
        return period;
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }
}
