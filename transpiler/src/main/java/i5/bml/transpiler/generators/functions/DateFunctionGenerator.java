package i5.bml.transpiler.generators.functions;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import generatedParser.BMLParser;
import i5.bml.parser.functions.BMLDateFunction;
import i5.bml.parser.types.functions.BMLFunctionType;
import i5.bml.transpiler.generators.CodeGenerator;
import i5.bml.transpiler.generators.Generator;
import i5.bml.transpiler.generators.java.JavaTreeGenerator;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@CodeGenerator(typeClass = BMLDateFunction.class)
public class DateFunctionGenerator extends Generator {

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @Override
    public Node generateFunctionCall(Expression object, BMLParser.FunctionCallContext ctx, JavaTreeGenerator visitor) {
        var cu = visitor.currentClass().findCompilationUnit().get();
        cu.addImport(LocalDate.class);
        cu.addImport(DateTimeFormatter.class);

        var formatExpr = ((BMLFunctionType) ctx.type).getRequiredParameters().get(0).exprCtx().getText();
        var call = "LocalDate.parse(LocalDate.now().format(DateTimeFormatter.ofPattern(%s)), DateTimeFormatter.ofPattern(%s)).toString()".formatted(formatExpr, formatExpr);
        return StaticJavaParser.parseExpression(call);
    }
}
