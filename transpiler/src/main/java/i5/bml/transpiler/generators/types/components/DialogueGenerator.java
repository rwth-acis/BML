package i5.bml.transpiler.generators.types.components;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.type.UnknownType;
import generatedParser.BMLParser;
import i5.bml.parser.types.dialogue.BMLDialogue;
import i5.bml.transpiler.bot.components.ComponentRegistry;
import i5.bml.transpiler.bot.dialogue.DialogueAutomaton;
import i5.bml.transpiler.bot.dialogue.DialogueFactory;
import i5.bml.transpiler.generators.JavaTreeGenerator;
import i5.bml.transpiler.generators.CodeGenerator;
import i5.bml.transpiler.generators.Generator;
import i5.bml.transpiler.utils.PrinterUtil;
import i5.bml.transpiler.utils.Utils;
import org.antlr.symtab.Type;

import java.util.HashSet;
import java.util.Set;

@CodeGenerator(typeClass = BMLDialogue.class)
public class DialogueGenerator implements Generator {

    private final Set<String> alreadyGeneratedDialogues = new HashSet<>();

    public DialogueGenerator(Type dialogueComponent) {}

    @Override
    public Node generateFunctionCall(Expression object, BMLParser.FunctionCallContext ctx, JavaTreeGenerator visitor) {
        if (!alreadyGeneratedDialogues.contains(object.toString())) {
            alreadyGeneratedDialogues.add(object.toString());

            PrinterUtil.readAndWriteClass(visitor.botOutputPath(), DialogueFactory.class, clazz -> {
                System.out.println("here");
            });
        }

        

        var event = new MethodCallExpr(new NameExpr("ctx"), "event");
        var session = new MethodCallExpr(event, "session");
        var dialogues = new MethodCallExpr(session, "dialogues");
        var stepCall = new MethodCallExpr(new NameExpr("d"), "step", new NodeList<>(new NameExpr("ctx")));
        var lambda = new LambdaExpr(new Parameter(new UnknownType(), "d"), stepCall);
        return new MethodCallExpr(dialogues, "forEach", new NodeList<>(lambda));
    }
}
