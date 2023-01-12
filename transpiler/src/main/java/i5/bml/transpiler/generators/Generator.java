package i5.bml.transpiler.generators;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.NameExpr;
import generatedParser.BMLParser;
import i5.bml.transpiler.generators.java.JavaTreeGenerator;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.TerminalNode;

public abstract class Generator {

    public void generateComponent(BMLParser.ComponentContext ctx, JavaTreeGenerator visitor) {
        throw new UnsupportedOperationException(this.getClass().getSimpleName() + " does not support the generation of components");
    }

    public Node generateFieldAccess(Expression object, TerminalNode field) {
        throw new UnsupportedOperationException(this.getClass().getSimpleName() + " does not support the generation of field accesses");
    }

    public Node generateFunctionCall(Expression object, BMLParser.FunctionCallContext ctx, JavaTreeGenerator visitor) {
        throw new UnsupportedOperationException(this.getClass().getSimpleName() + " does not support the generation of function calls");
    }

    public Node generateInitializer(ParserRuleContext ctx, JavaTreeGenerator visitor) {
        throw new UnsupportedOperationException(this.getClass().getSimpleName() + " does not support the generation of initializers");
    }

    public void populateClassWithFunction(BMLParser.FunctionDefinitionContext functionContext,
                                           BMLParser.AnnotationContext annotationContext, JavaTreeGenerator visitor) {
        throw new UnsupportedOperationException(this.getClass().getSimpleName() + " does not support the population of classes");
    }

    public Node generateArithmeticAssignmentToGlobal(BMLParser.AssignmentContext ctx, BinaryExpr.Operator op, JavaTreeGenerator visitor) {
        throw new UnsupportedOperationException(this.getClass().getSimpleName() + " does not support the generation of arithmetic assignments");
    }

    public Node generateAddAssignment(BMLParser.AssignmentContext ctx, JavaTreeGenerator visitor) {
        throw new UnsupportedOperationException(this.getClass().getSimpleName() + " does not support the generation of `+=`");
    }

    public Node generateGlobalNameExpr(BMLParser.AtomContext ctx) {
        return new NameExpr(ctx.token.getText());
    }

    public Node generateNameExpr(BMLParser.AtomContext ctx) {
        return new NameExpr(ctx.token.getText());
    }
}
