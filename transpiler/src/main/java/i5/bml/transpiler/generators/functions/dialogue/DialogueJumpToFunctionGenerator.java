package i5.bml.transpiler.generators.functions.dialogue;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import generatedParser.BMLParser;
import i5.bml.parser.types.BMLFunctionType;
import i5.bml.parser.functions.dialogue.BMLDialogueJumpToFunction;
import i5.bml.transpiler.generators.CodeGenerator;
import i5.bml.transpiler.generators.Generator;
import i5.bml.transpiler.generators.JavaTreeGenerator;

@CodeGenerator(typeClass = BMLDialogueJumpToFunction.class)
public class DialogueJumpToFunctionGenerator extends Generator {

    @Override
    public Node generateFunctionCall(Expression object, BMLParser.FunctionCallContext ctx, JavaTreeGenerator visitor) {
        var expr = ((BMLFunctionType) ctx.type).getRequiredParameters().get(0).getExprCtx();
        return new MethodCallExpr(new NameExpr("dialogueAutomaton"), "jumpTo", new NodeList<>((Expression) visitor.visit(expr), new NameExpr("ctx")));
    }
}
