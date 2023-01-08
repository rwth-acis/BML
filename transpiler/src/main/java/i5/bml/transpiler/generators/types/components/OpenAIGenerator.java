package i5.bml.transpiler.generators.types.components;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.type.VarType;
import generatedParser.BMLParser;
import i5.bml.parser.types.components.BMLOpenAIComponent;
import i5.bml.transpiler.bot.components.ComponentRegistry;
import i5.bml.transpiler.bot.dialogue.DialogueHandler;
import i5.bml.transpiler.bot.events.messenger.MessageHelper;
import i5.bml.transpiler.bot.threads.openai.OpenAIComponent;
import i5.bml.transpiler.bot.threads.rasa.RasaComponent;
import i5.bml.transpiler.generators.CodeGenerator;
import i5.bml.transpiler.generators.Generator;
import i5.bml.transpiler.generators.JavaTreeGenerator;
import i5.bml.transpiler.utils.PrinterUtil;
import i5.bml.transpiler.utils.Utils;
import org.antlr.symtab.Type;

@CodeGenerator(typeClass = BMLOpenAIComponent.class)
public class OpenAIGenerator implements Generator {

    private final BMLOpenAIComponent openAIComponent;

    private String fieldName;

    public OpenAIGenerator(Type openAIComponent) {
        this.openAIComponent = (BMLOpenAIComponent) openAIComponent;
    }

    @Override
    public void generateComponent(BMLParser.ComponentContext ctx, JavaTreeGenerator visitor) {
        var currentClass = visitor.currentClass();

        // Add field
        var type = StaticJavaParser.parseClassOrInterfaceType(OpenAIComponent.class.getSimpleName());
        fieldName = ctx.name.getText();
        var initializer = new ObjectCreationExpr(null, type, new NodeList<>(new StringLiteralExpr(openAIComponent.key()), new StringLiteralExpr(openAIComponent.model())));
        FieldDeclaration field = currentClass.addFieldWithInitializer(type, fieldName, initializer,
                Modifier.Keyword.PRIVATE, Modifier.Keyword.STATIC, Modifier.Keyword.FINAL);

        // Add getter
        Utils.generateRecordStyleGetter(field);
    }

    @Override
    public Node generateFunctionCall(Expression object, BMLParser.FunctionCallContext ctx, JavaTreeGenerator visitor) {
        var currentClass = visitor.currentClass();

        var block = new BlockStmt();
        var invokeModelExpr = new MethodCallExpr("ComponentRegistry.%s().invokeModel".formatted(fieldName), new NameExpr("messageEvent"));
        block.addStatement(new VariableDeclarationExpr(new VariableDeclarator(new VarType(), "response", invokeModelExpr)));
        block.addStatement(new MethodCallExpr(new NameExpr("MessageHelper"), "replyToMessenger",
                new NodeList<>(new NameExpr("ctx"), new NameExpr("response"))));

        // Add import for `ComponentRegistry` and `MessageHelper`
        //noinspection OptionalGetWithoutIsPresent -> We can assume presence
        var compilationUnit = currentClass.findCompilationUnit().get();
        compilationUnit.addImport(Utils.renameImport(ComponentRegistry.class, visitor.outputPackage()), false, false);
        compilationUnit.addImport(Utils.renameImport(MessageHelper.class, visitor.outputPackage()), false, false);

        return block;
    }
}
