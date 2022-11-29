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

public class SyntaxErrorListener extends BaseErrorListener {

    private final List<Diagnostic> collectedSyntaxErrors = new ArrayList<>();

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
        Position start;
        Position end;
        start = new Position(line, charPositionInLine);
        end = new Position(line, charPositionInLine);
        var range = new Range(start, end);
        collectedSyntaxErrors.add(new Diagnostic(range, msg, DiagnosticSeverity.Error, "bml"));
    }

    public List<Diagnostic> getCollectedSyntaxErrors() {
        return collectedSyntaxErrors;
    }
}
