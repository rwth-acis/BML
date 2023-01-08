package i5.bml.transpiler.generators.types.components;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import generatedParser.BMLParser;
import i5.bml.parser.types.components.BMLSlackComponent;
import i5.bml.transpiler.bot.threads.slack.SlackBotThread;
import i5.bml.transpiler.generators.CodeGenerator;
import i5.bml.transpiler.generators.Generator;
import i5.bml.transpiler.generators.JavaTreeGenerator;
import org.antlr.symtab.Type;

@CodeGenerator(typeClass = BMLSlackComponent.class)
public class SlackGenerator implements Generator, InitializableComponent {

    private final BMLSlackComponent slackComponent;

    public SlackGenerator(Type slackComponent) {
        this.slackComponent = (BMLSlackComponent) slackComponent;
    }

    @Override
    public void generateComponent(BMLParser.ComponentContext ctx, JavaTreeGenerator visitor) {
        var currentClass = visitor.currentClass();

        var threadInstance = new ObjectCreationExpr(null, StaticJavaParser.parseClassOrInterfaceType(SlackBotThread.class.getSimpleName()),
                new NodeList<>(
                        new NameExpr("eventQueue"),
                        new StringLiteralExpr(slackComponent.getBotToken()),
                        new StringLiteralExpr(slackComponent.getAppToken())
                ));

        // Add component initializer method to registry
        addComponentInitializerMethod(currentClass, "Slack", SlackBotThread.class, threadInstance, visitor.outputPackage());
    }
}
