package i5.bml.transpiler.generators;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.SimpleName;
import generatedParser.BMLBaseVisitor;
import generatedParser.BMLParser;
import i5.bml.parser.types.openapi.BMLOpenAPISchema;
import org.antlr.symtab.Type;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.commons.lang3.StringUtils;

@CodeGenerator(typeClass = BMLOpenAPISchema.class)
public class OpenAPISchemaGenerator implements Generator {

    BMLOpenAPISchema openAPISchema;

    public OpenAPISchemaGenerator(Type openAPISchema) {
        this.openAPISchema = (BMLOpenAPISchema) openAPISchema;
    }

    @Override
    public Node generateFieldAccess(Expression object, TerminalNode field) {
        return new MethodCallExpr(object, new SimpleName("get" + StringUtils.capitalize(field.getText())));
    }

    @Override
    public Node generateFunctionCall(BMLParser.FunctionCallContext function, BMLBaseVisitor<Node> visitor) {
        throw new IllegalStateException(openAPISchema.getName() + " does not support function calls");
    }
}
