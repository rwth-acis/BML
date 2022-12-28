package i5.bml.transpiler.generators.functions;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import generatedParser.BMLParser;
import i5.bml.parser.types.BMLFunctionType;
import i5.bml.parser.types.functions.BMLNumberFunction;
import i5.bml.transpiler.JavaSynthesizer;
import i5.bml.transpiler.generators.CodeGenerator;
import i5.bml.transpiler.generators.Generator;

@CodeGenerator(typeClass = BMLNumberFunction.class)
public class NumberFunctionGenerator implements Generator {

    @Override
    public Node generateFunctionCall(Expression object, BMLParser.FunctionCallContext ctx, JavaSynthesizer visitor) {
        var expr = ((BMLFunctionType) ctx.type).getRequiredParameters().get(0).getExprCtx();
        return new MethodCallExpr(new NameExpr("Integer"), "parseInt", new NodeList<>((Expression) visitor.visit(expr)));
    }
}
