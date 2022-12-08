package i5.bml.transpiler.generators;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import generatedParser.BMLBaseVisitor;
import generatedParser.BMLParser;
import i5.bml.parser.types.BMLFunction;
import org.antlr.v4.runtime.tree.TerminalNode;

public interface Generator {

    Node generateFieldAccess(Expression object, TerminalNode field);

    Node generateFunctionCall(BMLParser.FunctionCallContext functionCallContext, BMLBaseVisitor<Node> visitor);
}
