package i5.bml.transpiler.generators.types.components.openapi;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.SimpleName;
import i5.bml.parser.types.components.openapi.BMLOpenAPISchema;
import i5.bml.transpiler.generators.CodeGenerator;
import i5.bml.transpiler.generators.Generator;
import i5.bml.transpiler.generators.java.JavaTreeGenerator;
import org.antlr.symtab.Type;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@CodeGenerator(typeClass = BMLOpenAPISchema.class)
public class OpenAPISchemaGenerator extends Generator {

    private BMLOpenAPISchema openAPISchema;

    public OpenAPISchemaGenerator(Type openAPISchema) {
        this.openAPISchema = (BMLOpenAPISchema) openAPISchema;
    }

    @Override
    public Node generateFieldAccess(Expression object, TerminalNode field) {
        if (field.getText().equals("code")) {
            return new NameExpr(object.asNameExpr() + "Code");
        } else {
            return new MethodCallExpr(object, new SimpleName("get" + StringUtils.capitalize(field.getText())));
        }
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @Override
    public com.github.javaparser.ast.type.Type generateVariableType(Type type, JavaTreeGenerator visitor) {
        var cu = visitor.currentClass().findCompilationUnit().get();
        var packageName = getModelImport(visitor, openAPISchema.openAPIComponent().apiName(), openAPISchema.getName());
        cu.addImport(packageName, false, false);
        return super.generateVariableType(type, visitor);
    }

    @NotNull
    private String getModelImport(JavaTreeGenerator visitor, String apiName, String clientClassName) {
        if (!visitor.outputPackage().isEmpty()) {
            return visitor.outputPackage() + ".openapi." + apiName + ".models." + clientClassName;
        } else {
            return "openapi." + apiName + ".models." + clientClassName;
        }
    }
}
