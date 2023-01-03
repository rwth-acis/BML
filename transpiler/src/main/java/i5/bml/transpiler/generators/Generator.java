package i5.bml.transpiler.generators;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.NameExpr;
import generatedParser.BMLParser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.TerminalNode;

public interface Generator {

    default void generateComponent(BMLParser.ComponentContext ctx, JavaSynthesizer visitor) {
        throw new UnsupportedOperationException(this.getClass().getSimpleName() + " does not support the generation of components");
    }

    default Node generateFieldAccess(Expression object, TerminalNode field) {
        throw new UnsupportedOperationException(this.getClass().getSimpleName() + " does not support the generation of field accesses");
    }

    default Node generateFunctionCall(Expression object, BMLParser.FunctionCallContext ctx, JavaSynthesizer visitor) {
        throw new UnsupportedOperationException(this.getClass().getSimpleName() + " does not support the generation of function calls");
    }

    default Node generateInitializer(ParserRuleContext ctx, JavaSynthesizer visitor) {
        throw new UnsupportedOperationException(this.getClass().getSimpleName() + " does not support the generation of initializers");
    }

    default void populateClassWithFunction(BMLParser.FunctionDefinitionContext functionContext,
                                           BMLParser.AnnotationContext annotationContext, JavaSynthesizer visitor) {
        throw new UnsupportedOperationException(this.getClass().getSimpleName() + " does not support the population of classes");
    }

    default Node generateArithmeticAssignmentToGlobal(BMLParser.AssignmentContext ctx, BinaryExpr.Operator op, JavaSynthesizer visitor) {
        throw new UnsupportedOperationException(this.getClass().getSimpleName() + " does not support the generation of arithmetic assignments");
    }

    default Node generateAddAssignment(BMLParser.AssignmentContext ctx, JavaSynthesizer visitor) {
        throw new UnsupportedOperationException(this.getClass().getSimpleName() + " does not support the generation of `+=`");
    }

    default Node generateNameExpr(BMLParser.AtomContext ctx) {
        return new NameExpr(ctx.token.getText());
    }
}
