package i5.bml.transpiler.generators.types.components;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.Statement;
import i5.bml.transpiler.bot.events.messenger.MessageEventContext;
import i5.bml.transpiler.bot.events.messenger.MessageHelper;
import i5.bml.transpiler.generators.JavaTreeGenerator;
import i5.bml.transpiler.utils.PrinterUtil;
import i5.bml.transpiler.utils.Utils;
import org.apache.commons.lang3.StringUtils;

public interface IsMessengerComponent {

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    default void addBranchToMessageHelper(JavaTreeGenerator visitor, Class<?> messengerUserClass, Expression thenExpr, String helperMethod,
                                          Class<?>... classesToImport) {
        PrinterUtil.readAndWriteClass(visitor.botOutputPath(), MessageHelper.class, clazz -> {
            var methodDeclaration = StaticJavaParser.parseMethodDeclaration(helperMethod);
            clazz.addMember(methodDeclaration);

            var replyToMessengerBody = clazz.getMethodsBySignature("replyToMessenger", "User", "String").get(0).getBody().get();
            var ifStmt = replyToMessengerBody.stream()
                    .filter(node -> node instanceof Statement stmt
                            && stmt.isIfStmt()
                            && stmt.asIfStmt().hasElseBranch()
                            && !stmt.asIfStmt().getElseStmt().get().isIfStmt())
                    .findAny()
                    .map(n -> (IfStmt) n);

            // Add import for `messengerUserClass`
            var cu = clazz.findCompilationUnit().get();
            cu.addImport(Utils.renameImport(messengerUserClass, visitor.outputPackage()), false, false);
            for (Class<?> classToImport : classesToImport) {
                cu.addImport(Utils.renameImport(classToImport, visitor.outputPackage()), false, false);
            }

            var messengerUserTypeName = messengerUserClass.getSimpleName();
            var patternName = new SimpleName(StringUtils.uncapitalize(messengerUserTypeName));
            var type = StaticJavaParser.parseClassOrInterfaceType(messengerUserTypeName);
            var patternExpr = new PatternExpr(new NodeList<>(), type, patternName);
            var instanceofExpr = new InstanceOfExpr(new NameExpr("user"), type, patternExpr);

            if (ifStmt.isPresent()) {
                var elseStmt = ifStmt.get().getElseStmt().get();
                ifStmt.get().setElseStmt(new IfStmt(instanceofExpr, new ExpressionStmt(thenExpr), elseStmt));
            } else {
                var elseExpr = StaticJavaParser.parseExpression("LOGGER.error(\"Unknown user type {}\", user.getClass())");
                var newIfStmt = new IfStmt(instanceofExpr, new ExpressionStmt(thenExpr), new ExpressionStmt(elseExpr));
                replyToMessengerBody.addStatement(newIfStmt);
            }
        });
    }
}
