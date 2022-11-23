package types;

import generatedParser.BMLParser;
import org.antlr.symtab.ParameterSymbol;
import org.antlr.symtab.Type;

import java.util.ArrayList;
import java.util.List;

public class BMLFunction extends AbstractBMLType {

    private final Type returnType;

    private final List<ParameterSymbol> optionalParameters;

    private final List<ParameterSymbol> requiredParameters;

    public BMLFunction(Type returnType, List<ParameterSymbol> optionalParameters, List<ParameterSymbol> requiredParameters) {
        this.returnType = returnType;
        this.optionalParameters = optionalParameters;
        this.requiredParameters = requiredParameters;
    }

    public void checkParameters(BMLParser.FunctionCallContext ctx) {
        var parameterListMutable = new ArrayList<>(ctx.elementExpressionPairList().elementExpressionPair());

        for (var requiredParameter : requiredParameters) {
            // Name
            var name = requiredParameter.getName();

            var invocationParameter = parameterListMutable.stream()
                    .filter(p -> p.name.getText().equals(name))
                    .findAny();

            if (invocationParameter.isEmpty()) {
                throw new IllegalStateException("Parameter %s is required but not present for function call %s"
                        .formatted(name, ctx.getText()));
            }

            // Type
            var requiredParameterType = requiredParameter.getType();
            var invocationParameterType = invocationParameter.get().expression().type;
            if (!requiredParameterType.equals(invocationParameterType)) {
                throw new IllegalStateException("Parameter %s requires type %s but found type %s"
                        .formatted(name, requiredParameterType.getName(),
                                invocationParameterType.getName()));
            }

            parameterListMutable.remove(invocationParameter.get());
        }

        // TODO: This is not very nicely done
        // We remove the path parameter since it is not part of the OpenAPI spec
        var remainingParameters = parameterListMutable.stream()
                .filter(p -> !p.name.getText().equals("path"))
                .toList();

        checkOptionalParameters(remainingParameters);
    }

    private void checkOptionalParameters(List<BMLParser.ElementExpressionPairContext> remainingParameters) {
        for (var parameterPair : remainingParameters) {
            // Name
            var name = parameterPair.name.getText();
            var optionalParameter = optionalParameters.stream()
                    .filter(p -> p.getName().equals(name))
                    .findAny();

            if (optionalParameter.isEmpty()) {
                // TODO
                throw new IllegalStateException("`%s` is not defined".formatted(name));
            }

            // We can assume that parameter is present, so we expect the correct type
            var optionalParameterType = optionalParameter.get().getType();
            var invocationParameterType = parameterPair.expression().type;
            if (!optionalParameterType.equals(invocationParameterType)) {
                throw new IllegalStateException("`%s` requires type %s but found type %s"
                        .formatted(name, optionalParameterType.getName(),
                                invocationParameterType.getName()));
            }
        }
    }

    @Override
    public String getName() {
        return "Function"; // TODO
    }

    @Override
    public int getTypeIndex() {
        return -1; // TODO
    }

    public Type getReturnType() {
        return returnType;
    }
}
