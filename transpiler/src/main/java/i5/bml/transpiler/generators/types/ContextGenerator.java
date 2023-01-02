package i5.bml.transpiler.generators.types;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import i5.bml.parser.types.BMLContext;
import i5.bml.transpiler.generators.CodeGenerator;
import i5.bml.transpiler.generators.Generator;
import org.antlr.symtab.Type;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.commons.lang3.StringUtils;

@CodeGenerator(typeClass = BMLContext.class)
public class ContextGenerator implements Generator {

    public ContextGenerator(Type contextType) {}

    @Override
    public Node generateFieldAccess(Expression object, TerminalNode field) {
        return new MethodCallExpr(new NameExpr("ctx"), StringUtils.uncapitalize(field.getText()), new NodeList<>());
    }
}
