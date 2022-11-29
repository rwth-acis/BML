package i5.bml.parser.types;

import generatedParser.BMLParser;
import i5.bml.parser.walker.DiagnosticsCollector;
import org.antlr.symtab.ParameterSymbol;
import org.antlr.symtab.Type;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.*;

import static i5.bml.parser.errors.ParserError.*;
import static i5.bml.parser.errors.ParserError.EXPECTED_BUT_FOUND;

public abstract class AbstractBMLType implements Type {

    protected Map<String, Type> supportedAccesses = new HashMap<>();

    protected List<ParameterSymbol> requiredParameters = new ArrayList<>();

    protected List<ParameterSymbol> optionalParameters = new ArrayList<>();

    public void collectParameters() {
        var annotatedFields = Arrays.stream(this.getClass().getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(BMLComponentParameter.class))
                .toList();

        annotatedFields.stream()
                .map(f -> f.getAnnotation(BMLComponentParameter.class))
                .filter(BMLComponentParameter::isRequired)
                .forEach(p -> {
                    var parameterSymbol = new ParameterSymbol(p.name());
                    parameterSymbol.setType(TypeRegistry.resolveBuiltinType(p.expectedBMLType()));
                    requiredParameters.add(parameterSymbol);
                });

        annotatedFields.stream()
                .map(f -> f.getAnnotation(BMLComponentParameter.class))
                .filter(p -> !p.isRequired())
                .forEach(p -> {
                    var parameterSymbol = new ParameterSymbol(p.name());
                    parameterSymbol.setType(TypeRegistry.resolveBuiltinType(p.expectedBMLType()));
                    optionalParameters.add(parameterSymbol);
                });
    }

    public void checkParameters(DiagnosticsCollector diagnosticsCollector, BMLParser.ElementExpressionPairListContext ctx) {
        var parameterListMutable = new HashSet<>(ctx.elementExpressionPair());

        for (var requiredParameter : requiredParameters) {
            var name = requiredParameter.getName();

            var invocationParameter = parameterListMutable.stream()
                    .filter(p -> p.name.getText().equals(name))
                    .findAny();

            if (invocationParameter.isEmpty()) {
                diagnosticsCollector.addDiagnostic(MISSING_PARAM.format(name), ctx);
            } else {
                var requiredParameterType = requiredParameter.getType();
                var invocationParameterType = invocationParameter.get().expression().type;
                if (!requiredParameterType.equals(invocationParameterType)) {
                    diagnosticsCollector.addDiagnostic(EXPECTED_BUT_FOUND.format(requiredParameterType, invocationParameterType),
                            invocationParameter.get());
                }

                parameterListMutable.remove(invocationParameter.get());
            }
        }

        checkOptionalParameters(diagnosticsCollector, parameterListMutable);
    }

    private void checkOptionalParameters(DiagnosticsCollector diagnosticsCollector, Set<BMLParser.ElementExpressionPairContext> remainingParameters) {
        for (var parameterPair : remainingParameters) {
            // Name
            var name = parameterPair.name.getText();
            var optionalParameter = optionalParameters.stream()
                    .filter(p -> p.getName().equals(name))
                    .findAny();

            if (optionalParameter.isEmpty()) {
                diagnosticsCollector.addDiagnostic(NOT_DEFINED.format(name), parameterPair.name);
            } else {
                // We can assume that parameter is present, so we expect the correct type
                var optionalParameterType = optionalParameter.get().getType();
                var invocationParameterType = parameterPair.expression().type;
                if (!optionalParameterType.equals(invocationParameterType)) {
                    diagnosticsCollector.addDiagnostic(EXPECTED_BUT_FOUND.format(optionalParameterType, invocationParameterType),
                            parameterPair.expression());
                }
            }
        }
    }

    public void populateParameters(DiagnosticsCollector diagnosticsCollector, BMLParser.ElementExpressionPairListContext ctx) {
    }

    public void initializeType(DiagnosticsCollector diagnosticsCollector, ParserRuleContext ctx) {
    }

    public Type resolveAccess(DiagnosticsCollector diagnosticsCollector, ParseTree ctx) {
        return null;
    }

    @Override
    public String getName() {
        return this.getClass().getAnnotation(BMLType.class).name();
    }

    @Override
    public int getTypeIndex() {
        return -1;
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AbstractBMLType that = (AbstractBMLType) o;

        return this.getName().equals(that.getName());
    }
}
