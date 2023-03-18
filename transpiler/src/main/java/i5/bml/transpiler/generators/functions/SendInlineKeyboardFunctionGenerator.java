package i5.bml.transpiler.generators.functions;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import generatedParser.BMLParser;
import i5.bml.parser.functions.BMLSendInlineKeyboardFunction;
import i5.bml.parser.types.functions.BMLFunctionType;
import i5.bml.transpiler.bot.events.messenger.MessageHelper;
import i5.bml.transpiler.generators.CodeGenerator;
import i5.bml.transpiler.generators.Generator;
import i5.bml.transpiler.generators.java.JavaTreeGenerator;
import i5.bml.transpiler.utils.Utils;

@CodeGenerator(typeClass = BMLSendInlineKeyboardFunction.class)
public class SendInlineKeyboardFunctionGenerator extends Generator {

    @Override
    public Node generateFunctionCall(Expression object, BMLParser.FunctionCallContext ctx, JavaTreeGenerator visitor) {
        var functionType = (BMLFunctionType) ctx.type;
        var receiver = functionType.getOptionalParameters().stream()
                .filter(p -> p.getName().equals("receiver"))
                .findAny();
        var methodCallExpr = new MethodCallExpr(new NameExpr("MessageHelper"), "replyToMessenger");
        if (receiver.isPresent() && receiver.get().exprCtx() != null) {
            methodCallExpr.addArgument((Expression) visitor.visit(receiver.get().exprCtx()));
        } else {
            methodCallExpr.addArgument(new NameExpr("ctx"));
        }

        var text = functionType.getRequiredParameters().stream()
                .filter(p -> p.getName().equals("text"))
                .findAny();
        //noinspection OptionalGetWithoutIsPresent -> We can assume that it is present
        methodCallExpr.addArgument((Expression) visitor.visit(text.get().exprCtx()));

        var buttonRows = functionType.getRequiredParameters().stream()
                .filter(p -> p.getName().equals("buttonRows"))
                .findAny();
        //noinspection OptionalGetWithoutIsPresent -> We can assume that it is present
        methodCallExpr.addArgument((Expression) visitor.visit(buttonRows.get().exprCtx()));

        // Add import for `MessageHelper`
        //noinspection OptionalGetWithoutIsPresent -> We can assume that it is present
        var compilationUnit = visitor.currentClass().findCompilationUnit().get();
        compilationUnit.addImport(Utils.renameImport(MessageHelper.class, visitor.outputPackage()), false, false);

        return methodCallExpr;
    }
}
