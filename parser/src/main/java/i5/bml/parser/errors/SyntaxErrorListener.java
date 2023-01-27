package i5.bml.parser.errors;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;

import java.util.ArrayList;
import java.util.List;

/**
 * A syntax error listener class that extends {@link BaseErrorListener}. It collects syntax errors
 * <p>
 * and stores them in a {@link List} of {@link Diagnostic} objects.
 */
public class SyntaxErrorListener extends BaseErrorListener {

    /**
     * The list of collected syntax errors.
     */
    private final List<Diagnostic> collectedSyntaxErrors = new ArrayList<>();

    /**
     * Overrides the {@link BaseErrorListener#syntaxError} method to collect syntax errors and store them
     * in the {@link #collectedSyntaxErrors} list.
     *
     * @param recognizer         the recognizer that is being used.
     * @param offendingSymbol    the offending symbol.
     * @param line               the line number where the error occurred.
     * @param charPositionInLine the character position in the line where the error occurred.
     * @param msg                the error message.
     * @param e                  the exception that was thrown.
     */
    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
        Position start;
        Position end;
        start = new Position(line, charPositionInLine);
        end = new Position(line, charPositionInLine);
        var range = new Range(start, end);
        collectedSyntaxErrors.add(new Diagnostic(range, msg, DiagnosticSeverity.Error, "bml"));
    }

    /**
     * Returns the list of collected syntax errors.
     *
     * @return the {@link #collectedSyntaxErrors} list of collected syntax errors.
     */
    public List<Diagnostic> getCollectedSyntaxErrors() {
        return collectedSyntaxErrors;
    }
}
