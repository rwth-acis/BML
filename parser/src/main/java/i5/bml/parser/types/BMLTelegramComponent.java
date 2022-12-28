package i5.bml.parser.types;

import generatedParser.BMLParser;
import i5.bml.parser.errors.Diagnostics;
import i5.bml.parser.walker.DiagnosticsCollector;
import org.antlr.symtab.Type;
import org.antlr.v4.runtime.tree.ParseTree;

import java.awt.*;

import static i5.bml.parser.errors.ParserError.PARAM_REQUIRES_CONSTANT;

@BMLType(name = BuiltinType.TELEGRAM, isComplex = true)
public class BMLTelegramComponent extends AbstractBMLType {

    @BMLComponentParameter(name = "botName", expectedBMLType = BuiltinType.STRING, isRequired = true)
    private String botName;

    @BMLComponentParameter(name = "token", expectedBMLType = BuiltinType.STRING, isRequired = true)
    private String token;

    @Override
    public Type resolveAccess(DiagnosticsCollector diagnosticsCollector, ParseTree ctx) {
        return null;
    }

    @Override
    public void populateParameters(DiagnosticsCollector diagnosticsCollector, BMLParser.ElementExpressionPairListContext ctx) {
        // Missing parameters, but it has been reported by `checkParameters`
        if (ctx == null) {
            return;
        }

        //noinspection OptionalGetWithoutIsPresent
        var expr = ctx.elementExpressionPair().stream().filter(p -> p.name.getText().equals("botName")).findAny().get().expr;
        var atom = expr.atom();
        if (atom == null || atom.StringLiteral() == null) {
            Diagnostics.addDiagnostic(diagnosticsCollector.getCollectedDiagnostics(),
                    PARAM_REQUIRES_CONSTANT.format("botName", BuiltinType.STRING), expr);
        } else {
            botName = atom.getText().substring(1, atom.getText().length() - 1);
        }

        //noinspection OptionalGetWithoutIsPresent
        expr = ctx.elementExpressionPair().stream().filter(p -> p.name.getText().equals("token")).findAny().get().expr;
        atom = expr.atom();
        if (atom == null || atom.StringLiteral() == null) {
            Diagnostics.addDiagnostic(diagnosticsCollector.getCollectedDiagnostics(),
                    PARAM_REQUIRES_CONSTANT.format("token", BuiltinType.STRING), expr);
        } else {
            token = atom.getText().substring(1, atom.getText().length() - 1);
        }
    }

    public String getBotName() {
        return botName;
    }

    public String getToken() {
        return token;
    }
}
