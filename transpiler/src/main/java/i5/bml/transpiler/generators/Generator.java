package i5.bml.transpiler.generators;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.Expression;
import generatedParser.BMLBaseVisitor;
import generatedParser.BMLParser;
import org.antlr.v4.runtime.tree.TerminalNode;

public interface Generator {

    default Node generateComponent(BMLParser.ComponentContext ctx, BMLBaseVisitor<Node> visitor) {
        throw new UnsupportedOperationException(this.getClass().getSimpleName() + " does not support the generation of components");
    }

    default Node generateFieldAccess(Expression object, TerminalNode field) {
        throw new UnsupportedOperationException(this.getClass().getSimpleName() + " does not support the generation of field accesses");
    }

    default Node generateFunctionCall(BMLParser.FunctionCallContext ctx, BMLBaseVisitor<Node> visitor) {
        throw new UnsupportedOperationException(this.getClass().getSimpleName() + " does not support the generation of function calls");
    }

    default void populateClassWithFunction(String botOutputPath, BMLParser.FunctionDefinitionContext functionContext,
                                           BMLParser.AnnotationContext annotationContext, BMLBaseVisitor<Node> visitor) {
        throw new UnsupportedOperationException(this.getClass().getSimpleName() + " does not support the population of classes");
    }
}
