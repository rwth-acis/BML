package i5.bml.parser.types;

import generatedParser.BMLParser;
import i5.bml.parser.errors.Diagnostics;
import i5.bml.parser.walker.DiagnosticsCollector;
import org.antlr.symtab.Type;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;

import java.util.*;

import static i5.bml.parser.errors.ParserError.MISSING_PARAM;
import static i5.bml.parser.errors.ParserError.PARAM_NOT_DEFINED;

public abstract class AbstractBMLType implements Type {

    protected Map<String, Type> supportedAccesses = new HashMap<>();

    protected List<BMLFunctionParameter> requiredParameters = new ArrayList<>();

    protected List<BMLFunctionParameter> optionalParameters = new ArrayList<>();

    protected List<Diagnostic> cachedDiagnostics = new ArrayList<>();

    protected int typeIndex;

    protected void cacheDiagnostic(String msg, DiagnosticSeverity severity) {
        var d = new Diagnostic();
        d.setMessage(msg);
        d.setSeverity(severity);
        cachedDiagnostics.add(d);
    }

    public void collectParameters() {
        Arrays.stream(this.getClass().getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(BMLComponentParameter.class))
                .map(f -> f.getAnnotation(BMLComponentParameter.class))
                .forEach(p -> {
                    var parameter = new BMLFunctionParameter(p.name());
                    parameter.setType(TypeRegistry.resolveType(p.expectedBMLType()));
                    if (p.isRequired()) {
                        requiredParameters.add(parameter);
                    } else {
                        optionalParameters.add(parameter);
                    }
                });
    }

    public void checkParameters(DiagnosticsCollector diagnosticsCollector, BMLParser.ElementExpressionPairListContext ctx) {
        if (ctx == null) {
            return;
        }

        var parameterListMutable = new HashSet<>(ctx.elementExpressionPair());

        for (var requiredParameter : requiredParameters) {
            var name = requiredParameter.getName();

            var invocationParameter = parameterListMutable.stream()
                    .filter(p -> p.name.getText().equals(name))
                    .findAny();

            if (invocationParameter.isEmpty()) {
                Diagnostics.addDiagnostic(diagnosticsCollector.getCollectedDiagnostics(), MISSING_PARAM.format(name), ctx);
            } else {
                requiredParameter.setExprCtx(invocationParameter.get().expr);
                var invocationParameterType = invocationParameter.get().expr.type;
                if (requiredParameter.getAllowedTypes().stream().noneMatch(t -> t.equals(invocationParameterType))) {
                    addTypeErrorMessage(diagnosticsCollector, invocationParameter.get(), requiredParameter);
                }

                parameterListMutable.remove(invocationParameter.get());
            }
        }

        checkOptionalParameters(diagnosticsCollector, parameterListMutable);
    }

    protected void checkOptionalParameters(DiagnosticsCollector diagnosticsCollector,
                                         Set<BMLParser.ElementExpressionPairContext> remainingParameters) {
        for (var invocationParameter : remainingParameters) {
            // Name
            var name = invocationParameter.name.getText();
            var optionalParameter = optionalParameters.stream()
                    .filter(p -> p.getName().equals(name))
                    .findAny();

            if (optionalParameter.isEmpty()) {
                Diagnostics.addDiagnostic(diagnosticsCollector.getCollectedDiagnostics(),
                        PARAM_NOT_DEFINED.format(name), invocationParameter.name);
            } else {
                // We can assume that parameter is present, so we expect the correct type
                optionalParameter.get().setExprCtx(invocationParameter.expr);
                var invocationParameterType = invocationParameter.expr.type;
                if (optionalParameter.get().getAllowedTypes().stream().noneMatch(t -> t.equals(invocationParameterType))) {
                    addTypeErrorMessage(diagnosticsCollector, invocationParameter, optionalParameter.get());
                }
            }
        }
    }

    private void addTypeErrorMessage(DiagnosticsCollector diagnosticsCollector, BMLParser.ElementExpressionPairContext invocationParameter,
                                     BMLFunctionParameter parameter) {
        var errorMessage = new StringBuilder();
        errorMessage.append("Expected any of ");
        for (Type allowedType : parameter.getAllowedTypes()) {
            errorMessage.append("`").append(allowedType).append("`, ");
        }
        var i = errorMessage.lastIndexOf(", ");
        errorMessage.delete(i, i + 2);
        errorMessage.append(" but found ").append("`").append(invocationParameter.expr.type).append("`");
        Diagnostics.addDiagnostic(diagnosticsCollector.getCollectedDiagnostics(), errorMessage.toString(),
                invocationParameter.expr);
    }

    public void populateParameters(DiagnosticsCollector diagnosticsCollector, BMLParser.ElementExpressionPairListContext ctx) {
    }

    public void initializeType(ParserRuleContext ctx) {
    }

    public Type resolveAccess(DiagnosticsCollector diagnosticsCollector, ParseTree ctx) {
        if (ctx instanceof BMLParser.FunctionCallContext functionCallContext) {
            return supportedAccesses.get(functionCallContext.functionName.getText());
        } else {
            return supportedAccesses.get(ctx.getText());
        }
    }

    public Type deepCopy() {
        return this;
    }

    public List<Diagnostic> getCachedDiagnostics() {
        return cachedDiagnostics;
    }

    @Override
    public String getName() {
        return this.getClass().getAnnotation(BMLType.class).name().toString();
    }

    public void setTypeIndex(int typeIndex) {
        this.typeIndex = typeIndex;
    }

    @Override
    public int getTypeIndex() {
        return typeIndex;
    }

    public Map<String, Type> getSupportedAccesses() {
        return supportedAccesses;
    }

    @Override
    public String toString() {
        return getName();
    }

    public String encodeToString() {
        return getName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AbstractBMLType that = (AbstractBMLType) o;

        return this.getTypeIndex() == that.getTypeIndex();
    }
}
