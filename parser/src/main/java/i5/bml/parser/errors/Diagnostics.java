package i5.bml.parser.errors;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;

import java.util.List;

/**
 * The Diagnostics class provides static methods for adding diagnostic information to a List of Diagnostics.
 * <p>
 * It includes methods for adding a {@link Diagnostic} object, a message, and a {@link ParserError},
 * with options for setting the severity and range of the diagnostic.
 * It also includes methods for adding diagnostics based on a {@link Token} object.
 * <p>
 * Note: The class has a private constructor, since it is a "static" class and should not be instantiated.
 */
public class Diagnostics {

    private Diagnostics() {
    }

    public static void addDiagnostic(List<Diagnostic> diagnostics, Diagnostic diagnostic, ParserRuleContext ctx) {
        Position start;
        Position end;
        start = new Position(ctx.start.getLine(), ctx.start.getCharPositionInLine());
        end = new Position(ctx.stop.getLine(), ctx.start.getCharPositionInLine() + ctx.getText().length());
        diagnostic.setRange(new Range(start, end));
        diagnostic.setSource("bml");
        diagnostics.add(diagnostic);
    }

    public static void addDiagnostic(List<Diagnostic> diagnostics, String message, ParserRuleContext ctx) {
        addDiagnostic(diagnostics, message, ctx, DiagnosticSeverity.Error);
    }

    public static void addDiagnostic(List<Diagnostic> diagnostics, ParserError parserError, ParserRuleContext ctx) {
        addDiagnostic(diagnostics, parserError.message, ctx, DiagnosticSeverity.Error);
    }

    public static void addDiagnostic(List<Diagnostic> diagnostics, String message, ParserRuleContext ctx, DiagnosticSeverity severity) {
        Position start;
        Position end;
        start = new Position(ctx.start.getLine(), ctx.start.getCharPositionInLine());
        end = new Position(ctx.stop.getLine(), ctx.start.getCharPositionInLine() + ctx.getText().length());
        var range = new Range(start, end);
        diagnostics.add(new Diagnostic(range, message, severity, "bml"));
    }

    public static void addDiagnostic(List<Diagnostic> diagnostics, String message, Token token) {
        addDiagnostic(diagnostics, message, token, DiagnosticSeverity.Error);
    }

    public static void addDiagnostic(List<Diagnostic> diagnostics, ParserError parserError, Token token) {
        addDiagnostic(diagnostics, parserError.message, token, DiagnosticSeverity.Error);
    }

    public static void addDiagnostic(List<Diagnostic> diagnostics, String message, Token token, DiagnosticSeverity severity) {
        var start = new Position(token.getLine(), token.getCharPositionInLine() + 1);
        var end = new Position(token.getLine(), token.getCharPositionInLine() + token.getText().length());
        var range = new Range(start, end);
        diagnostics.add(new Diagnostic(range, message, DiagnosticSeverity.Error, "bml"));
    }
}
