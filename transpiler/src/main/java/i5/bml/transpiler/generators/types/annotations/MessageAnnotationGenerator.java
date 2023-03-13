package i5.bml.transpiler.generators.types.annotations;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.BlockStmt;
import generatedParser.BMLParser;
import i5.bml.parser.types.annotations.BMLMessengerAnnotation;
import i5.bml.transpiler.bot.events.MessageEventHandlerMethod;
import i5.bml.transpiler.bot.events.messenger.MessageEventContext;
import i5.bml.transpiler.bot.events.messenger.MessageEventHandler;
import i5.bml.transpiler.bot.events.messenger.MessageEventType;
import i5.bml.transpiler.generators.CodeGenerator;
import i5.bml.transpiler.generators.Generator;
import i5.bml.transpiler.generators.java.JavaTreeGenerator;
import i5.bml.transpiler.utils.PrinterUtil;
import i5.bml.transpiler.utils.Utils;
import org.antlr.symtab.Type;

@CodeGenerator(typeClass = BMLMessengerAnnotation.class)
public class MessageAnnotationGenerator extends Generator {

    public MessageAnnotationGenerator(Type bmlMessengerAnnotation) {}

    @Override
    public void populateClassWithFunction(BMLParser.FunctionDefinitionContext functionContext,
                                          BMLParser.AnnotationContext annotationContext, JavaTreeGenerator visitor) {
        PrinterUtil.readAndWriteClass(visitor.botOutputPath(), MessageEventHandler.class, clazz -> {
            var functionName = functionContext.head.functionName.getText();
            var methods = clazz.getMethodsByName(functionName);
            var eventName = Utils.pascalCaseToSnakeCase(annotationContext.name.getText());
            if (methods.isEmpty()) {
                var handlerMethod = clazz.addMethod(functionName, Modifier.Keyword.PUBLIC, Modifier.Keyword.STATIC);
                handlerMethod.addAnnotation(new NormalAnnotationExpr(new Name(MessageEventHandlerMethod.class.getSimpleName()),
                        new NodeList<>(new MemberValuePair("messageEventType", new FieldAccessExpr(new NameExpr(MessageEventType.class.getSimpleName()), eventName)))));
                handlerMethod.addParameter(MessageEventContext.class.getSimpleName(), "ctx");

                visitor.classStack().push(clazz);
                var body = (BlockStmt) visitor.visitFunctionDefinition(functionContext);
                var usesIntentOrEntity = body.findFirst(MethodCallExpr.class, m -> m.getNameAsString().equals("intent") || m.getNameAsString().equals("entity"));
                if (usesIntentOrEntity.isPresent()) {
                    // TODO: Throw error if NLU not present
                    var invokeModel = "ComponentRegistry.rasa().invokeModel(ctx.event())";
                    body.addStatement(0, StaticJavaParser.parseExpression(invokeModel));
                }
                handlerMethod.setBody(body);
                visitor.classStack().pop();
            } else {
                methods.get(0).addAnnotation(new NormalAnnotationExpr(new Name(MessageEventHandlerMethod.class.getSimpleName()),
                        new NodeList<>(new MemberValuePair("messageEventType", new FieldAccessExpr(new NameExpr(MessageEventType.class.getSimpleName()), eventName)))));
            }

            // Add import for `MessageEventHandlerMethod`
            //noinspection OptionalGetWithoutIsPresent -> We can assume that it is present
            var compilationUnit = clazz.findCompilationUnit().get();
            compilationUnit.addImport(Utils.renameImport(MessageEventHandlerMethod.class, visitor.outputPackage()), false, false);
        });
    }
}
