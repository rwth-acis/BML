package i5.bml.transpiler.generators.types.components;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.slack.api.methods.SlackApiException;
import com.slack.api.socket_mode.SocketModeClient;
import generatedParser.BMLParser;
import i5.bml.parser.types.components.BMLSlackComponent;
import i5.bml.transpiler.bot.threads.slack.SlackBotThread;
import i5.bml.transpiler.bot.threads.slack.SlackUser;
import i5.bml.transpiler.generators.CodeGenerator;
import i5.bml.transpiler.generators.Generator;
import i5.bml.transpiler.generators.JavaTreeGenerator;
import i5.bml.transpiler.utils.IOUtil;
import org.antlr.symtab.Type;

import java.io.IOException;

@CodeGenerator(typeClass = BMLSlackComponent.class)
public class SlackGenerator extends Generator implements InitializableComponent, IsMessengerComponent {

    private final BMLSlackComponent slackComponent;

    private static final String SEND_SLACK_MESSAGE = """
            private static void sendSlackMessage(SocketModeClient slackClient, String botToken, String channelId, String msg) {
                try {
                    slackClient.getSlack().methods().chatPostMessage(r -> r.token(botToken).channel(channelId).text(msg));
                } catch (IOException | SlackApiException e) {
                  LOGGER.error("An error occurred while sending the msg '{}' to the chat with id {} using the slack bot:
                                {}", msg, channelId, e.getMessage());
                }
            }""";

    public SlackGenerator(Type slackComponent) {
        this.slackComponent = (BMLSlackComponent) slackComponent;
    }

    @Override
    public void generateComponent(BMLParser.ComponentContext ctx, JavaTreeGenerator visitor) {
        var currentClass = visitor.currentClass();

        // Make sure that dependencies and included in gradle build file
        visitor.gradleFile().add("hasSlackComponent", true);

        // Copy required implementation for Slack
        IOUtil.copyDirAndRenameImports("threads/slack", visitor);

        // Add methods to `MessageHelper`
        var expr = StaticJavaParser.parseExpression("sendSlackMessage(slackUser.slackClient(), slackUser.botToken(), slackUser.channelId(), msg)");
        addBranchToMessageHelper(visitor, SlackUser.class, expr, SEND_SLACK_MESSAGE, SocketModeClient.class,
                SlackApiException.class, IOException.class);

        // Add component initializer method to registry
        var threadInstance = new ObjectCreationExpr(null, StaticJavaParser.parseClassOrInterfaceType(SlackBotThread.class.getSimpleName()),
                new NodeList<>(
                        new NameExpr("eventQueue"),
                        new StringLiteralExpr(slackComponent.getBotToken()),
                        new StringLiteralExpr(slackComponent.getAppToken())
                ));
        addComponentInitializerMethod(currentClass, "Slack", SlackBotThread.class, threadInstance, visitor.outputPackage());
    }
}
