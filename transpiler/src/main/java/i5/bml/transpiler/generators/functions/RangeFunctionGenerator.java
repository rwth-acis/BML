package i5.bml.transpiler.generators.functions;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.type.UnknownType;
import com.github.javaparser.ast.type.VarType;
import generatedParser.BMLParser;
import i5.bml.parser.functions.BMLRangeFunction;
import i5.bml.parser.types.functions.BMLFunctionType;
import i5.bml.transpiler.generators.CodeGenerator;
import i5.bml.transpiler.generators.Generator;
import i5.bml.transpiler.generators.java.JavaTreeGenerator;

import java.util.stream.IntStream;

@CodeGenerator(typeClass = BMLRangeFunction.class)
public class RangeFunctionGenerator extends Generator {

    @Override
    public Node generateFunctionCall(Expression object, BMLParser.FunctionCallContext ctx, JavaTreeGenerator visitor) {
        var functionType = ((BMLFunctionType) ctx.type);

        var startExpr = functionType.getRequiredParameters().get(0).getExprCtx();
        var endExpr = functionType.getRequiredParameters().get(1).getExprCtx();
        var step = functionType.getOptionalParameters().stream()
                .filter(p -> p.getName().equals("step"))
                .findAny();

        Expression stepExpr;
        if (step.isEmpty()) {
            stepExpr = new IntegerLiteralExpr("1");
        } else {
            stepExpr = (Expression) visitor.visit(functionType.getOptionalParameters().get(0).getExprCtx());
        }

        var initialization = new NodeList<Expression>(new VariableDeclarationExpr(new VariableDeclarator(new VarType(), "i", (Expression) visitor.visit(startExpr))));
        var compare = new BinaryExpr(new NameExpr("i"), (Expression) visitor.visit(endExpr), BinaryExpr.Operator.LESS);
        var update = new NodeList<Expression>(new AssignExpr(new NameExpr("i"), stepExpr, AssignExpr.Operator.PLUS));
        return new ForStmt(initialization, compare, update, new BlockStmt());
    }
}
