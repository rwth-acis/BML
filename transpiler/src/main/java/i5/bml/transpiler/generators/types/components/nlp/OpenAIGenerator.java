package i5.bml.transpiler.generators.types.components.nlp;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.expr.*;
import generatedParser.BMLParser;
import i5.bml.parser.types.components.nlp.BMLOpenAIComponent;
import i5.bml.parser.types.functions.BMLFunctionType;
import i5.bml.transpiler.bot.components.ComponentRegistry;
import i5.bml.transpiler.bot.threads.openai.OpenAIComponent;
import i5.bml.transpiler.generators.CodeGenerator;
import i5.bml.transpiler.generators.Generator;
import i5.bml.transpiler.generators.java.JavaTreeGenerator;
import i5.bml.transpiler.generators.types.components.UsesEnvVariable;
import i5.bml.transpiler.utils.IOUtil;
import i5.bml.transpiler.utils.Utils;
import org.antlr.symtab.Type;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.stream.Collectors;

@CodeGenerator(typeClass = BMLOpenAIComponent.class)
public class OpenAIGenerator extends Generator implements UsesEnvVariable {

    private final BMLOpenAIComponent openAIComponent;

    public OpenAIGenerator(Type openAIComponent) {
        this.openAIComponent = (BMLOpenAIComponent) openAIComponent;
    }

    @Override
    public void generateComponent(BMLParser.ComponentContext ctx, JavaTreeGenerator visitor) {
        var currentClass = visitor.currentClass();

        // Make sure that dependencies and included in gradle build file
        visitor.gradleFile().add("hasOpenAIComponent", true);

        // Copy required implementation for OpenAI
        IOUtil.copyDirAndRenameImports("threads/openai", visitor);

        // Add field
        var type = StaticJavaParser.parseClassOrInterfaceType(OpenAIComponent.class.getSimpleName());
        var fieldName = "openAI";
        var initializerArgs = new NodeList<Expression>();
        initializerArgs.add(getFromEnv(openAIComponent.key()));
        initializerArgs.add(new StringLiteralExpr(openAIComponent.model()));
        initializerArgs.add(new IntegerLiteralExpr(openAIComponent.tokens().isEmpty() ? "-1" : openAIComponent.tokens()));
        if (openAIComponent.duration() != null) {
            initializerArgs.add(new MethodCallExpr(new NameExpr(Duration.class.getSimpleName()), "of", new NodeList<>(
                    new LongLiteralExpr(openAIComponent.duration()),
                    new FieldAccessExpr(new NameExpr(ChronoUnit.class.getSimpleName()), openAIComponent.timeUnit().name())
            )));
        } else {
            initializerArgs.add(new MethodCallExpr(new NameExpr(Duration.class.getSimpleName()), "of", new NodeList<>(
                    new LongLiteralExpr("10"),
                    new FieldAccessExpr(new NameExpr(ChronoUnit.class.getSimpleName()), "SECONDS")
            )));
        }
        initializerArgs.add(new StringLiteralExpr(openAIComponent.prompt()));
        var initializer = new ObjectCreationExpr(null, type, initializerArgs);
        FieldDeclaration field = currentClass.addFieldWithInitializer(type, fieldName, initializer,
                Modifier.Keyword.PRIVATE, Modifier.Keyword.STATIC, Modifier.Keyword.FINAL);

        // Add getter
        Utils.generateRecordStyleGetter(field, true);

        // Add import for `OpenAIComponent`
        //noinspection OptionalGetWithoutIsPresent -> We can assume presence
        var compilationUnit = currentClass.findCompilationUnit().get();
        compilationUnit.addImport(Utils.renameImport(OpenAIComponent.class, visitor.outputPackage()), false, false);

        // Add imports for `Duration` and `ChronoUnit`
        compilationUnit.addImport(Duration.class);
        compilationUnit.addImport(ChronoUnit.class);
    }

    @Override
    public Node generateFunctionCall(Expression object, BMLParser.FunctionCallContext ctx, JavaTreeGenerator visitor) {
        var currentClass = visitor.currentClass();

        // Add import for `ComponentRegistry`
        //noinspection OptionalGetWithoutIsPresent -> We can assume presence
        var compilationUnit = currentClass.findCompilationUnit().get();
        compilationUnit.addImport(Utils.renameImport(ComponentRegistry.class, visitor.outputPackage()), false, false);

        var funcName = ctx.functionName.getText();
        return switch (funcName) {
            case "process" -> {
                var call = "ComponentRegistry.openAI().invokeModel(ctx.event())";
                yield StaticJavaParser.parseExpression(call);
            }
            case "annotate" -> {
                var call = "ComponentRegistry.openAI().annotateMessage()";
                var params = new ArrayList<>(((BMLFunctionType) ctx.type).getRequiredParameters());
                var args = params.stream().map(p -> (Expression) visitor.visit(p.exprCtx())).collect(Collectors.toCollection(NodeList::new));
                yield StaticJavaParser.parseExpression(call).asMethodCallExpr().setArguments(args);
            }
            default -> throw new IllegalStateException("Unknown function call %s".formatted(funcName));
        };
    }
}
