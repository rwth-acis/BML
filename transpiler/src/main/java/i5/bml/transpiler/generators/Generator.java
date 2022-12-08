package i5.bml.transpiler.generators;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import generatedParser.BMLBaseVisitor;
import generatedParser.BMLParser;
import org.antlr.v4.runtime.tree.TerminalNode;

public interface Generator {

    Node generateComponent(BMLParser.ComponentContext componentContext, BMLBaseVisitor<Node> visitor);

    Node generateFieldAccess(Expression object, TerminalNode field);

    Node generateFunctionCall(BMLParser.FunctionCallContext functionCallContext, BMLBaseVisitor<Node> visitor);
}
