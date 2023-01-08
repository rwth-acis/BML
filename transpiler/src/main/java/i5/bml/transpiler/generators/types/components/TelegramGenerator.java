package i5.bml.transpiler.generators.types.components;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.*;
import generatedParser.BMLParser;
import i5.bml.parser.types.components.BMLTelegramComponent;
import i5.bml.transpiler.generators.JavaTreeGenerator;
import i5.bml.transpiler.bot.threads.telegram.TelegramBotThread;
import i5.bml.transpiler.generators.CodeGenerator;
import i5.bml.transpiler.generators.Generator;
import org.antlr.symtab.Type;

@CodeGenerator(typeClass = BMLTelegramComponent.class)
public class TelegramGenerator implements Generator, InitializableComponent {

    private final BMLTelegramComponent telegramComponent;

    public TelegramGenerator(Type telegramComponent) {
        this.telegramComponent = (BMLTelegramComponent) telegramComponent;
    }

    @Override
    public void generateComponent(BMLParser.ComponentContext ctx, JavaTreeGenerator visitor) {
        var currentClass = visitor.currentClass();

        // Add component initializer method to registry
        var threadInstance = new ObjectCreationExpr(null, StaticJavaParser.parseClassOrInterfaceType(TelegramBotThread.class.getSimpleName()),
                new NodeList<>(new NameExpr("eventQueue"), new StringLiteralExpr(telegramComponent.getBotName()), new StringLiteralExpr(telegramComponent.getBotToken())));
        addComponentInitializerMethod(currentClass, "Telegram", TelegramBotThread.class, threadInstance, visitor.outputPackage());
    }
}
