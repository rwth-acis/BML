package i5.bml.transpiler.generators.types.annotations;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.stmt.BlockStmt;
import generatedParser.BMLParser;
import i5.bml.parser.types.annotations.BMLBotAnnotation;
import i5.bml.transpiler.bot.Bot;
import i5.bml.transpiler.generators.CodeGenerator;
import i5.bml.transpiler.generators.Generator;
import i5.bml.transpiler.generators.java.JavaTreeGenerator;
import i5.bml.transpiler.utils.PrinterUtil;
import org.antlr.symtab.Type;

@CodeGenerator(typeClass = BMLBotAnnotation.class)
public class BotAnnotationGenerator extends Generator {

    public BotAnnotationGenerator(Type bmlBotAnnotation) {}

    @Override
    public void populateClassWithFunction(BMLParser.FunctionDefinitionContext functionContext, BMLParser.AnnotationContext annotationContext, JavaTreeGenerator visitor) {
        PrinterUtil.readAndWriteClass(visitor.botOutputPath(), Bot.class, clazz -> {
            var functionName = functionContext.head.functionName.getText();
            var initMethodDecl = clazz.addMethod(functionName, Modifier.Keyword.PRIVATE);
            visitor.classStack().push(clazz);
            var body = (BlockStmt) visitor.visitFunctionDefinition(functionContext);
            initMethodDecl.setBody(body);
            visitor.classStack().pop();

            var initCall = StaticJavaParser.parseExpression("%s()".formatted(functionName));
            initCall.setLineComment("Bot initialization (@BotStarted annotation)");
            //noinspection OptionalGetWithoutIsPresent -> We can assume presence
            clazz.getDefaultConstructor().get().getBody().addStatement(initCall);
        });
    }
}
