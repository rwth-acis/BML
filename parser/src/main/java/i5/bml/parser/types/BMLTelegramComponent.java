package i5.bml.parser.types;

import i5.bml.parser.walker.DiagnosticsCollector;
import org.antlr.symtab.Type;
import org.antlr.v4.runtime.tree.ParseTree;

@BMLType(name = BuiltinType.TELEGRAM, isComplex = true)
public class BMLTelegramComponent extends AbstractBMLType {

    @Override
    public Type resolveAccess(DiagnosticsCollector diagnosticsCollector, ParseTree ctx) {
        return null;
    }
}
