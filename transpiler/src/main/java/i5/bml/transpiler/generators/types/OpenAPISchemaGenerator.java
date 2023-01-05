package i5.bml.transpiler.generators.types;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.SimpleName;
import i5.bml.parser.types.openapi.BMLOpenAPISchema;
import i5.bml.transpiler.generators.CodeGenerator;
import i5.bml.transpiler.generators.Generator;
import org.antlr.symtab.Type;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.commons.lang3.StringUtils;

@CodeGenerator(typeClass = BMLOpenAPISchema.class)
public class OpenAPISchemaGenerator implements Generator {

    public OpenAPISchemaGenerator(Type openAPISchema) {}

    @Override
    public Node generateFieldAccess(Expression object, TerminalNode field) {
        if (field.getText().equals("code")) {
            return new NameExpr(object.asNameExpr() + "Code");
        } else {
            return new MethodCallExpr(object, new SimpleName("get" + StringUtils.capitalize(field.getText())));
        }
    }
}
