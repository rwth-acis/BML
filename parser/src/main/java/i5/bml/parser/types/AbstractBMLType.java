package i5.bml.parser.types;

import generatedParser.BMLParser;
import i5.bml.parser.errors.Diagnostics;
import i5.bml.parser.walker.DiagnosticsCollector;
import org.antlr.symtab.ParameterSymbol;
import org.antlr.symtab.Type;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;

import java.util.*;

import static i5.bml.parser.errors.ParserError.*;

public abstract class AbstractBMLType implements Type {

    protected Map<String, Type> supportedAccesses = new HashMap<>();

    protected List<ParameterSymbol> requiredParameters = new ArrayList<>();

    protected List<ParameterSymbol> optionalParameters = new ArrayList<>();

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
                    var parameterSymbol = new ParameterSymbol(p.name());
                    parameterSymbol.setType(TypeRegistry.resolveType(p.expectedBMLType()));
                    if (p.isRequired()) {
                        requiredParameters.add(parameterSymbol);
                    } else {
                        optionalParameters.add(parameterSymbol);
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
                var requiredParameterType = requiredParameter.getType();
                var invocationParameterType = invocationParameter.get().expression().type;
                if (!requiredParameterType.equals(invocationParameterType)) {
                    Diagnostics.addDiagnostic(diagnosticsCollector.getCollectedDiagnostics(),
                            EXPECTED_BUT_FOUND.format(requiredParameterType, invocationParameterType),
                            invocationParameter.get());
                }

                parameterListMutable.remove(invocationParameter.get());
            }
        }

        checkOptionalParameters(diagnosticsCollector, parameterListMutable);
    }

    private void checkOptionalParameters(DiagnosticsCollector diagnosticsCollector,
                                         Set<BMLParser.ElementExpressionPairContext> remainingParameters) {
        for (var parameterPair : remainingParameters) {
            // Name
            var name = parameterPair.name.getText();
            var optionalParameter = optionalParameters.stream()
                    .filter(p -> p.getName().equals(name))
                    .findAny();

            if (optionalParameter.isEmpty()) {
                Diagnostics.addDiagnostic(diagnosticsCollector.getCollectedDiagnostics(),
                        PARAM_NOT_DEFINED.format(name), parameterPair.name);
            } else {
                // We can assume that parameter is present, so we expect the correct type
                var optionalParameterType = optionalParameter.get().getType();
                var invocationParameterType = parameterPair.expression().type;
                if (!optionalParameterType.equals(invocationParameterType)) {
                    Diagnostics.addDiagnostic(diagnosticsCollector.getCollectedDiagnostics(),
                            EXPECTED_BUT_FOUND.format(optionalParameterType, invocationParameterType),
                            parameterPair.expression());
                }
            }
        }
    }

    public void populateParameters(DiagnosticsCollector diagnosticsCollector, BMLParser.ElementExpressionPairListContext ctx) {
    }

    public void initializeType(ParserRuleContext ctx) {
    }

    public Type resolveAccess(DiagnosticsCollector diagnosticsCollector, ParseTree ctx) {
        return null;
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
