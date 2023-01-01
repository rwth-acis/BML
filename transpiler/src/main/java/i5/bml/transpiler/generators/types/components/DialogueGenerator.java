package i5.bml.transpiler.generators.types.components;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import generatedParser.BMLBaseVisitor;
import generatedParser.BMLParser;
import i5.bml.parser.types.dialogue.BMLDialogue;
import i5.bml.transpiler.JavaSynthesizer;
import i5.bml.transpiler.generators.CodeGenerator;
import i5.bml.transpiler.generators.Generator;
import org.antlr.symtab.Type;

@CodeGenerator(typeClass = BMLDialogue.class)
public class DialogueGenerator implements Generator {

    public DialogueGenerator(Type dialogueComponent) {}

    @Override
    public Node generateFunctionCall(Expression object, BMLParser.FunctionCallContext ctx, JavaSynthesizer visitor) {
        var getEvent = new MethodCallExpr(new NameExpr("context"), "event");
        var getSession = new MethodCallExpr(getEvent, "session");
        var getDialogue = new MethodCallExpr(getSession, "dialogue");
        return new MethodCallExpr(getDialogue, "step", new NodeList<>(new NameExpr("context")));
    }
}
