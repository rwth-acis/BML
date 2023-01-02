package i5.bml.transpiler.generators.types.components;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.BlockStmt;
import generatedParser.BMLParser;
import i5.bml.parser.types.BMLTelegramComponent;
import i5.bml.transpiler.JavaSynthesizer;
import i5.bml.transpiler.bot.threads.telegram.TelegramBotThread;
import i5.bml.transpiler.generators.CodeGenerator;
import i5.bml.transpiler.generators.Generator;
import i5.bml.transpiler.utils.Utils;
import org.antlr.symtab.Type;

import java.util.concurrent.ExecutorService;

@CodeGenerator(typeClass = BMLTelegramComponent.class)
public class TelegramGenerator implements Generator {

    private final BMLTelegramComponent telegramComponent;

    public TelegramGenerator(Type telegramComponent) {
        this.telegramComponent = (BMLTelegramComponent) telegramComponent;
    }

    @Override
    public void generateComponent(BMLParser.ComponentContext ctx, JavaSynthesizer visitor) {
        var currentClass = visitor.currentClass();

        // Add initializer method
        var method = currentClass.addMethod("initTelegramComponent", Modifier.Keyword.PUBLIC, Modifier.Keyword.STATIC);
        method.addAnnotation(new MarkerAnnotationExpr(new Name("ComponentInitializer")));
        method.addParameter(ExecutorService.class, "threadPool");
        method.addParameter(StaticJavaParser.parseType("PriorityBlockingQueue<Event>"), "eventQueue");
        var threadInstance = new ObjectCreationExpr(null, StaticJavaParser.parseClassOrInterfaceType("TelegramBotThread"),
                new NodeList<>(new NameExpr("eventQueue"), new StringLiteralExpr(telegramComponent.getBotName()), new StringLiteralExpr(telegramComponent.getBotToken())));
        method.setBody(new BlockStmt().addStatement(new MethodCallExpr(new NameExpr("threadPool"), "execute", new NodeList<>(threadInstance))));

        // Add import for `TelegramBotThread`
        var telegramBotThreadImport = Utils.renameImport(TelegramBotThread.class, visitor.outputPackage());
        //noinspection OptionalGetWithoutIsPresent -> We can assume that it is present
        currentClass.findCompilationUnit().get().addImport(telegramBotThreadImport, false, false);
    }
}
