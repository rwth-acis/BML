package i5.bml.transpiler.generators.types.components.nlu;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.expr.*;
import generatedParser.BMLParser;
import i5.bml.parser.types.components.nlu.BMLOpenAIComponent;
import i5.bml.transpiler.bot.components.ComponentRegistry;
import i5.bml.transpiler.bot.events.messenger.MessageHelper;
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
import java.util.concurrent.TimeUnit;

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
        var initializer = new ObjectCreationExpr(null, type, new NodeList<>(
                getFromEnv(openAIComponent.key()),
                new StringLiteralExpr(openAIComponent.model()),
                new IntegerLiteralExpr(openAIComponent.tokens()),
                new MethodCallExpr(new NameExpr(Duration.class.getSimpleName()), "of",
                        new NodeList<>(
                                new LongLiteralExpr(openAIComponent.duration()),
                                new FieldAccessExpr(new NameExpr(ChronoUnit.class.getSimpleName()), openAIComponent.timeUnit().name())
                        )),
                new StringLiteralExpr(openAIComponent.prompt())
        ));
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

        // Add import for `ComponentRegistry` and `MessageHelper`
        //noinspection OptionalGetWithoutIsPresent -> We can assume presence
        var compilationUnit = currentClass.findCompilationUnit().get();
        compilationUnit.addImport(Utils.renameImport(ComponentRegistry.class, visitor.outputPackage()), false, false);

        var call = "ComponentRegistry.openAI().invokeModel(ctx.event())";
        return StaticJavaParser.parseExpression(call);
    }
}
