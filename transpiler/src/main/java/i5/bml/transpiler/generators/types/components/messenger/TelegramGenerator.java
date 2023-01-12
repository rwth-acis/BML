package i5.bml.transpiler.generators.types.components.messenger;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import generatedParser.BMLParser;
import i5.bml.parser.types.components.messenger.BMLTelegramComponent;
import i5.bml.transpiler.bot.threads.telegram.TelegramBotThread;
import i5.bml.transpiler.bot.threads.telegram.TelegramComponent;
import i5.bml.transpiler.bot.threads.telegram.TelegramUser;
import i5.bml.transpiler.generators.CodeGenerator;
import i5.bml.transpiler.generators.Generator;
import i5.bml.transpiler.generators.java.JavaTreeGenerator;
import i5.bml.transpiler.generators.types.components.InitializableComponent;
import i5.bml.transpiler.utils.IOUtil;
import org.antlr.symtab.Type;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@CodeGenerator(typeClass = BMLTelegramComponent.class)
public class TelegramGenerator extends Generator implements InitializableComponent, IsMessengerComponent {

    private final BMLTelegramComponent telegramComponent;

    private static final String SEND_TELEGRAM_MESSAGE = """
            private static void sendTelegramMessage(TelegramComponent telegramComponent, Long chatId, String msg) {
                try {
                    var send = new SendMessage();
                    send.setChatId(chatId);
                    send.setText(msg);
                    telegramComponent.execute(send);
                } catch (TelegramApiException e) {
                    LOGGER.error("An error occurred while sending the msg '{}' to the chat with id {} using the telegram bot {}:\\n{}", msg, chatId, telegramComponent.getBotUsername(), e.getMessage());
                }
            }""";

    public TelegramGenerator(Type telegramComponent) {
        this.telegramComponent = (BMLTelegramComponent) telegramComponent;
    }

    @Override
    public void generateComponent(BMLParser.ComponentContext ctx, JavaTreeGenerator visitor) {
        var currentClass = visitor.currentClass();

        // Make sure that dependencies and included in gradle build file
        visitor.gradleFile().add("hasTelegramComponent", true);

        // Copy required implementation for Telegram
        IOUtil.copyDirAndRenameImports("threads/telegram", visitor);

        // Add methods to `MessageHelper`
        var expr = StaticJavaParser.parseExpression("sendTelegramMessage(telegramUser.telegramComponent(), telegramUser.chatId(), msg)");
        addBranchToMessageHelper(visitor, TelegramUser.class, expr, SEND_TELEGRAM_MESSAGE, TelegramComponent.class,
                TelegramApiException.class, SendMessage.class);

        // Add component initializer method to registry
        var threadInstance = new ObjectCreationExpr(null, StaticJavaParser.parseClassOrInterfaceType(TelegramBotThread.class.getSimpleName()),
                new NodeList<>(new NameExpr("eventQueue"), new StringLiteralExpr(telegramComponent.getBotName()), new StringLiteralExpr(telegramComponent.getBotToken())));
        addComponentInitializerMethod(currentClass, "Telegram", TelegramBotThread.class, threadInstance, visitor.outputPackage());
    }
}
