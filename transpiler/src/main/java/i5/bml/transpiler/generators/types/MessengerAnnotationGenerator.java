package i5.bml.transpiler.generators.types;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.BlockStmt;
import generatedParser.BMLParser;
import i5.bml.parser.types.annotations.BMLMessengerAnnotation;
import i5.bml.transpiler.JavaSynthesizer;
import i5.bml.transpiler.bot.events.MessageEventHandlerMethod;
import i5.bml.transpiler.generators.CodeGenerator;
import i5.bml.transpiler.generators.Generator;
import i5.bml.transpiler.utils.Utils;
import org.antlr.symtab.Type;

@CodeGenerator(typeClass = BMLMessengerAnnotation.class)
public class MessengerAnnotationGenerator implements Generator {

    private static final String PATH = "events/messenger";

    private static final String CLASS_NAME = "MessageEventHandler";

    public MessengerAnnotationGenerator(Type bmlMessengerAnnotation) {}

    @Override
    public void populateClassWithFunction(BMLParser.FunctionDefinitionContext functionContext,
                                          BMLParser.AnnotationContext annotationContext, JavaSynthesizer visitor) {
        Utils.readAndWriteClass("%s%s".formatted(visitor.getBotOutputPath(), PATH), CLASS_NAME, clazz -> {
            var functionName = functionContext.head.functionName.getText();
            var methods = clazz.getMethodsByName(functionName);
            var eventName = Utils.pascalCaseToSnakeCase(annotationContext.name.getText());
            if (methods.isEmpty()) {
                var handlerMethod = clazz.addMethod(functionName, Modifier.Keyword.PUBLIC, Modifier.Keyword.STATIC);
                handlerMethod.addAnnotation(new NormalAnnotationExpr(new Name("MessageEventHandlerMethod"),
                        new NodeList<>(new MemberValuePair("messageEventType", new FieldAccessExpr(new NameExpr("MessageEventType"), eventName)))));
                handlerMethod.addParameter("MessageEventContext", "context");
                visitor.getClassStack().push(clazz);
                handlerMethod.setBody((BlockStmt) visitor.visitFunctionDefinition(functionContext));
                visitor.getClassStack().pop();
            } else {
                methods.get(0).addAnnotation(new NormalAnnotationExpr(new Name("MessageEventHandlerMethod"),
                        new NodeList<>(new MemberValuePair("messageEventType", new FieldAccessExpr(new NameExpr("MessageEventType"), eventName)))));
            }

            // Add import for `MessageEventHandlerMethod`
            //noinspection OptionalGetWithoutIsPresent -> We can assume that it is present
            var compilationUnit = clazz.findCompilationUnit().get();
            compilationUnit.addImport(Utils.renameImport(MessageEventHandlerMethod.class, visitor.getOutputPackage()), false, false);
        });
    }
}
