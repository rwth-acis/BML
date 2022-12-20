package i5.bml.transpiler.generators;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.BlockStmt;
import generatedParser.BMLBaseVisitor;
import generatedParser.BMLParser;
import i5.bml.parser.types.annotations.BMLMessengerAnnotation;
import i5.bml.parser.types.annotations.BMLRoutineAnnotation;
import i5.bml.transpiler.bot.events.messenger.MessageEventContext;
import i5.bml.transpiler.utils.Utils;
import org.antlr.symtab.Type;

@CodeGenerator(typeClass = BMLMessengerAnnotation.class)
public class MessengerAnnotationGenerator implements Generator {

    private static final String PATH = "events/messenger";

    private static final String CLASS_NAME = "MessageEventHandler";

    private static ClassOrInterfaceDeclaration clazz;

    public MessengerAnnotationGenerator(Type bmlMessengerAnnotation) {}

    @Override
    public void populateClassWithFunction(String botOutputPath, BMLParser.FunctionDefinitionContext functionContext,
                                          BMLParser.AnnotationContext annotationContext, BMLBaseVisitor<Node> visitor) {
        if (clazz == null) {
            clazz = Utils.readClass("%s/%s".formatted(botOutputPath, PATH), CLASS_NAME);
        }

        var functionName = functionContext.head.functionName.getText();
        var methods = clazz.getMethodsByName(functionName);
        var eventName = Utils.pascalCaseToSnakeCase(annotationContext.name.getText());
        if (methods.isEmpty()) {
            var handlerMethod = clazz.addMethod(functionName, Modifier.Keyword.PUBLIC, Modifier.Keyword.STATIC);
            handlerMethod.addAnnotation(new NormalAnnotationExpr(new Name("EventHandler"),
                    new NodeList<>(new MemberValuePair("messageEventType", new FieldAccessExpr(new NameExpr("MessageEventType"), eventName)))));
            handlerMethod.addParameter(MessageEventContext.class, "context");
            handlerMethod.setBody((BlockStmt) visitor.visit(functionContext.body));
        } else {
            methods.get(0).addAnnotation(new NormalAnnotationExpr(new Name("EventHandler"),
                    new NodeList<>(new MemberValuePair("messageEventType", new FieldAccessExpr(new NameExpr("MessageEventType"), eventName)))));
        }
    }
}
