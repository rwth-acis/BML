package i5.bml.transpiler.generators.functions;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import generatedParser.BMLParser;
import i5.bml.parser.functions.BMLStringFunction;
import i5.bml.parser.types.functions.BMLFunctionType;
import i5.bml.transpiler.generators.CodeGenerator;
import i5.bml.transpiler.generators.Generator;
import i5.bml.transpiler.generators.java.JavaTreeGenerator;

@CodeGenerator(typeClass = BMLStringFunction.class)
public class StringFunctionGenerator extends Generator {

    @Override
    public Node generateFunctionCall(Expression object, BMLParser.FunctionCallContext ctx, JavaTreeGenerator visitor) {
        var functionType = (BMLFunctionType) ctx.type;
        var expr = functionType.getRequiredParameters().get(0).getExprCtx();
        return new MethodCallExpr(new NameExpr("String"), "valueOf", new NodeList<>((Expression) visitor.visit(expr)));
    }
}
