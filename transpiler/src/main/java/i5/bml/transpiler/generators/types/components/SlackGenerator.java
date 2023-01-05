package i5.bml.transpiler.generators.types.components;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.BlockStmt;
import generatedParser.BMLParser;
import i5.bml.parser.types.BMLSlackComponent;
import i5.bml.transpiler.generators.JavaTreeGenerator;
import i5.bml.transpiler.bot.threads.slack.SlackBotThread;
import i5.bml.transpiler.generators.CodeGenerator;
import i5.bml.transpiler.generators.Generator;
import i5.bml.transpiler.utils.Utils;
import org.antlr.symtab.Type;

import java.util.concurrent.ExecutorService;

@CodeGenerator(typeClass = BMLSlackComponent.class)
public class SlackGenerator implements Generator {

    private final BMLSlackComponent slackComponent;

    public SlackGenerator(Type slackComponent) {
        this.slackComponent = (BMLSlackComponent) slackComponent;
    }

    @Override
    public void generateComponent(BMLParser.ComponentContext ctx, JavaTreeGenerator visitor) {
        var currentClass = visitor.currentClass();

        // Add initializer method
        var method = currentClass.addMethod("initSlackComponent", Modifier.Keyword.PUBLIC, Modifier.Keyword.STATIC);
        method.addAnnotation(new MarkerAnnotationExpr(new Name("ComponentInitializer")));
        method.addParameter(ExecutorService.class, "threadPool");
        method.addParameter(StaticJavaParser.parseType("PriorityBlockingQueue<Event>"), "eventQueue");
        var threadInstance = new ObjectCreationExpr(null, StaticJavaParser.parseClassOrInterfaceType("SlackBotThread"),
                new NodeList<>(
                        new NameExpr("eventQueue"),
                        new StringLiteralExpr(slackComponent.getBotToken()),
                        new StringLiteralExpr(slackComponent.getAppToken())
                ));
        method.setBody(new BlockStmt().addStatement(new MethodCallExpr(new NameExpr("threadPool"), "execute", new NodeList<>(threadInstance))));

        // Add import for `SlackBotThread`
        var slackBotThreadImport = Utils.renameImport(SlackBotThread.class, visitor.outputPackage());
        //noinspection OptionalGetWithoutIsPresent -> We can assume that it is present
        currentClass.findCompilationUnit().get().addImport(slackBotThreadImport, false, false);
    }
}
