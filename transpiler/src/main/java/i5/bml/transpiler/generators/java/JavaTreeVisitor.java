package i5.bml.transpiler.generators.java;

import com.github.javaparser.ast.comments.LineComment;
import com.github.javaparser.printer.DefaultPrettyPrinterVisitor;
import com.github.javaparser.printer.configuration.PrinterConfiguration;
import com.github.javaparser.utils.Utils;

public class JavaTreeVisitor extends DefaultPrettyPrinterVisitor {

    public JavaTreeVisitor(PrinterConfiguration configuration) {
        super(configuration);
    }

    @Override
    public void visit(LineComment n, Void arg) {
        printer.println().print("// ").println(Utils.normalizeEolInTextBlock(n.getContent(), "").trim());
    }
}
