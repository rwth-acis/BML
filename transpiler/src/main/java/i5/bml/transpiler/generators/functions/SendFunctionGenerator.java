package i5.bml.transpiler.generators.functions;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import generatedParser.BMLParser;
import i5.bml.parser.types.BMLFunctionType;
import i5.bml.parser.types.functions.BMLSendFunction;
import i5.bml.transpiler.bot.events.messenger.MessageHelper;
import i5.bml.transpiler.generators.CodeGenerator;
import i5.bml.transpiler.generators.Generator;
import i5.bml.transpiler.generators.JavaTreeGenerator;
import i5.bml.transpiler.utils.Utils;

@CodeGenerator(typeClass = BMLSendFunction.class)
public class SendFunctionGenerator extends Generator {

    @Override
    public Node generateFunctionCall(Expression object, BMLParser.FunctionCallContext ctx, JavaTreeGenerator visitor) {
        var functionType = (BMLFunctionType) ctx.type;
        var receiver = functionType.getOptionalParameters().stream()
                .filter(p -> p.getName().equals("receiver"))
                .findAny();
        var methodCallExpr = new MethodCallExpr(new NameExpr("MessageHelper"), "replyToMessenger");
        if (receiver.isPresent() && receiver.get().getExprCtx() != null) {
            methodCallExpr.addArgument((Expression) visitor.visit(receiver.get().getExprCtx()));
        } else {
            methodCallExpr.addArgument(new NameExpr("ctx"));
        }

        var text = functionType.getRequiredParameters().stream()
                .filter(p -> p.getName().equals("text"))
                .findAny();
        //noinspection OptionalGetWithoutIsPresent -> We can assume that it is present
        methodCallExpr.addArgument((Expression) visitor.visit(text.get().getExprCtx()));

        // Add import for `MessageHelper`
        //noinspection OptionalGetWithoutIsPresent -> We can assume that it is present
        var compilationUnit = visitor.currentClass().findCompilationUnit().get();
        compilationUnit.addImport(Utils.renameImport(MessageHelper.class, visitor.outputPackage()), false, false);

        return methodCallExpr;
    }
}
