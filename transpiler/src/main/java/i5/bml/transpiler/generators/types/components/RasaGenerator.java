package i5.bml.transpiler.generators.types.components;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.BlockStmt;
import generatedParser.BMLParser;
import i5.bml.parser.types.BMLRasaComponent;
import i5.bml.transpiler.generators.JavaTreeGenerator;
import i5.bml.transpiler.bot.components.ComponentRegistry;
import i5.bml.transpiler.bot.dialogue.DialogueHandler;
import i5.bml.transpiler.bot.threads.rasa.RasaComponent;
import i5.bml.transpiler.generators.CodeGenerator;
import i5.bml.transpiler.generators.Generator;
import i5.bml.transpiler.utils.PrinterUtil;
import i5.bml.transpiler.utils.Utils;
import org.antlr.symtab.Type;

import java.util.concurrent.ExecutorService;

@CodeGenerator(typeClass = BMLRasaComponent.class)
public class RasaGenerator implements Generator {

    private final BMLRasaComponent rasaComponent;

    public RasaGenerator(Type rasaComponent) {
        this.rasaComponent = (BMLRasaComponent) rasaComponent;
    }

    @Override
    public void generateComponent(BMLParser.ComponentContext ctx, JavaTreeGenerator visitor) {
        var currentClass = visitor.currentClass();

        // Add field
        var type = StaticJavaParser.parseClassOrInterfaceType("RasaComponent");
        var fieldName = ctx.name.getText();
        var initializer = new ObjectCreationExpr(null, type, new NodeList<>(new StringLiteralExpr(rasaComponent.getUrl())));
        FieldDeclaration field = currentClass.addFieldWithInitializer(type, fieldName,
                initializer, Modifier.Keyword.PRIVATE, Modifier.Keyword.STATIC, Modifier.Keyword.FINAL);

        // Add getter
        var getter = field.createGetter();
        getter.addModifier(Modifier.Keyword.STATIC);

        // Add initializer method
        var method = currentClass.addMethod("initRasaComponent", Modifier.Keyword.PUBLIC, Modifier.Keyword.STATIC);
        method.addAnnotation(new MarkerAnnotationExpr(new Name("ComponentInitializer")));
        method.addParameter(ExecutorService.class, "threadPool");
        method.addParameter(StaticJavaParser.parseType("PriorityBlockingQueue<Event>"), "eventQueue");
        method.setBody(new BlockStmt().addStatement(new MethodCallExpr(new NameExpr(fieldName), "init")));

        // Add import for `RasaHandler`
        var rasaHandlerImport = Utils.renameImport(RasaComponent.class, visitor.outputPackage());
        //noinspection OptionalGetWithoutIsPresent -> We can assume that it is present
        currentClass.findCompilationUnit().get().addImport(rasaHandlerImport, false, false);

        // Register to `DialogueHandler`
        PrinterUtil.readAndWriteClass(visitor.botOutputPath(), DialogueHandler.class, clazz -> {
            var m = clazz.getMethodsByName("handleMessageEvent").get(0);
            var block = new BlockStmt();
            block.addStatement(new MethodCallExpr("ComponentRegistry.getRasa().invokeModel", new NameExpr("messageEvent")));
            m.setBody(block);

            // Add import for `ComponentRegistry`
            //noinspection OptionalGetWithoutIsPresent -> We can assume that it is present
            var compilationUnit = clazz.findCompilationUnit().get();
            compilationUnit.addImport(Utils.renameImport(ComponentRegistry.class, visitor.outputPackage()), false, false);
        });
    }
}
