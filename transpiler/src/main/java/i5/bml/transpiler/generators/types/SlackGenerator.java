package i5.bml.transpiler.generators.types;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.BlockStmt;
import generatedParser.BMLParser;
import i5.bml.parser.types.BMLSlackComponent;
import i5.bml.transpiler.JavaSynthesizer;
import i5.bml.transpiler.bot.threads.slack.SlackBotThread;
import i5.bml.transpiler.generators.CodeGenerator;
import i5.bml.transpiler.generators.Generator;
import i5.bml.transpiler.utils.Utils;
import org.antlr.symtab.Type;

import java.util.concurrent.ExecutorService;

@CodeGenerator(typeClass = BMLSlackComponent.class)
public class SlackGenerator implements Generator {

    public SlackGenerator(Type slackComponent) {}

    @Override
    public void generateComponent(BMLParser.ComponentContext ctx, JavaSynthesizer visitor) {
        var currentClass = visitor.getCurrentClass();

        // Add initializer method
        var method = currentClass.addMethod("initSlackComponent", Modifier.Keyword.PUBLIC, Modifier.Keyword.STATIC);
        method.addAnnotation(new MarkerAnnotationExpr(new Name("ComponentInitializer")));
        method.addParameter(ExecutorService.class, "threadPool");
        method.addParameter(StaticJavaParser.parseType("PriorityBlockingQueue<Event>"), "eventQueue");
        var threadInstance = new ObjectCreationExpr(null, StaticJavaParser.parseClassOrInterfaceType("SlackBotThread"),
                new NodeList<>(new NameExpr("eventQueue")));
        // TODO: Add botToken
        method.setBody(new BlockStmt().addStatement(new MethodCallExpr(new NameExpr("threadPool"), "execute", new NodeList<>(threadInstance))));

        // Add import for `SlackBotThread`
        var slackBotThreadImport = Utils.renameImport(SlackBotThread.class, visitor.getOutputPackage());
        //noinspection OptionalGetWithoutIsPresent -> We can assume that it is present
        currentClass.findCompilationUnit().get().addImport(slackBotThreadImport, false, false);
    }
}
