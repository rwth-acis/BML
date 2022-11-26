package i5.bml.parser.types;

import i5.bml.parser.errors.ParserException;
import generatedParser.BMLParser;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.symtab.ParameterSymbol;
import org.antlr.symtab.Type;

import java.util.ArrayList;
import java.util.List;

import static i5.bml.parser.errors.ParserError.*;

public class BMLFunction extends AbstractBMLType {

    private final Type returnType;

    private final List<ParameterSymbol> requiredParameters;

    private final List<ParameterSymbol> optionalParameters;

    public BMLFunction(Type returnType, List<ParameterSymbol> requiredParameters, List<ParameterSymbol> optionalParameters) {
        this.returnType = returnType;
        this.requiredParameters = requiredParameters;
        this.optionalParameters = optionalParameters;
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
                throw new ParserException("Parameter %s is required but not present for function call %s"
                        .formatted(name, ctx.getText()), ctx);
            }

            // Type
            var requiredParameterType = requiredParameter.getType();
            var invocationParameterType = invocationParameter.get().expression().type;
            if (!requiredParameterType.equals(invocationParameterType)) {
                throw new ParserException(EXPECTED_BUT_FOUND.format(requiredParameterType, invocationParameterType),
                        invocationParameter.get());
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
                throw new ParserException(NOT_DEFINED.format(name), parameterPair.name);
            }

            // We can assume that parameter is present, so we expect the correct type
            var optionalParameterType = optionalParameter.get().getType();
            var invocationParameterType = parameterPair.expression().type;
            if (!optionalParameterType.equals(invocationParameterType)) {
                throw new ParserException(EXPECTED_BUT_FOUND.format(optionalParameterType, invocationParameterType),
                        parameterPair.expression());
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

    @Override
    public Type resolveAccess(ParseTree ctx) {
        return ((AbstractBMLType) returnType).resolveAccess(ctx);
    }

    public Type getReturnType() {
        return returnType;
    }
}
