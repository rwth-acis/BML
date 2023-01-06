package i5.bml.transpiler.generators.types;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import generatedParser.BMLParser;
import i5.bml.parser.types.dialogue.BMLState;
import i5.bml.transpiler.generators.CodeGenerator;
import i5.bml.transpiler.generators.Generator;
import org.antlr.symtab.Type;

@CodeGenerator(typeClass = BMLState.class)
public class StateGenerator implements Generator {

    public StateGenerator(Type stateType) {}

    @Override
    public Node generateNameExpr(BMLParser.AtomContext ctx) {
        return new MethodCallExpr(new NameExpr("dialogueAutomaton"), "getStateByName", new NodeList<>(new StringLiteralExpr(ctx.getText())));
    }
}
